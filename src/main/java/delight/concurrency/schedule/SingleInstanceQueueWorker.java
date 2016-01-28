package delight.concurrency.schedule;

import delight.async.Operation;
import delight.async.callbacks.ValueCallback;
import delight.concurrency.Concurrency;
import delight.concurrency.wrappers.SimpleExecutor;
import delight.functional.Success;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.Vector;

/**
 * Allows to build a queue of objects, which are processed sequentially and
 * non-concurrently.
 * 
 * @author mroh004
 * 
 * @param <GItem>
 */
public abstract class SingleInstanceQueueWorker<GItem> {

    private final SequentialOperationScheduler thread;
    protected final Queue<GItem> queue;

    private volatile boolean shutdownRequested = false;
    private volatile boolean isShutDown = false;
    private volatile QueueShutdownCallback shutDowncallback;

    private final Vector<WhenProcessed> finalizedListener;

    public static interface WhenProcessed {
        public void thenDo();
    }

    /**
     * It is guaranteed that this method is only called by one worker thread at
     * the time and that the items are forwarded FIFO how they were offered.
     * 
     * @param item
     */
    protected abstract void processItems(List<GItem> item);

    public void shutdown(final ValueCallback<Success> cb) {
        thread.shutdown(cb);
    }

    public interface QueueShutdownCallback {
        public void onShutdown();

        public void onFailure(Throwable t);
    }

    public void requestShutdown(final QueueShutdownCallback callback) {
        shutDowncallback = callback;
        shutdownRequested = true;
        thread.startIfRequired();
    }

    /**
     * Schedules to process this item.
     * 
     * @param item
     */
    public void offer(final GItem item) {
        thread.schedule(new Operation<Object>() {

            @Override
            public void apply(final ValueCallback<Object> callback) {
                queue.offer(item);
            }

        }, new ValueCallback<Object>() {

            @Override
            public void onFailure(final Throwable t) {
                throw new RuntimeException(t);
            }

            @Override
            public void onSuccess(final Object value) {

            }
        });

        synchronized (queue) {
            if (isShutDown) {
                throw new IllegalStateException("Cannot submit tasks for a shutdown worker: [" + item + "]");
            }
            queue.offer(item);
        }
    }

    public boolean isRunning() {
        return thread.getIsRunning();
    }

    public SingleInstanceThread getThread() {
        return thread;
    }

    private void callFinalizeListener() {
        if (finalizedListener.size() > 0) {
            final ArrayList<WhenProcessed> toProcesses = new ArrayList<WhenProcessed>(finalizedListener);
            for (final WhenProcessed p : toProcesses) {
                p.thenDo();
            }
        }
    }

    /**
     * Only to create this as a dummy.
     */
    public SingleInstanceQueueWorker() {
        super();
        this.thread = null;
        this.queue = null;
        this.finalizedListener = null;
    }

    public SingleInstanceQueueWorker(final SimpleExecutor executor, final Queue<GItem> queue, final Concurrency con) {

        this.thread = new SequentialOperationScheduler(con);

        this.thread = new SingleInstanceThread(executor, con) {

            @Override
            public void run(final Notifiyer notifiyer) {

                synchronized (queue) {

                    while (queue.size() > 0) {
                        final List<GItem> items = new ArrayList<GItem>(queue.size());

                        GItem next;
                        while ((next = queue.poll()) != null) {
                            items.add(next);
                            // break;
                        }

                        processItems(items);
                    }

                    notifiyer.notifiyFinished();

                    callFinalizeListener();

                    if (shutdownRequested) {
                        isShutDown = true;
                        shutDowncallback.onShutdown();
                    }

                }
            }

        };
        this.queue = queue;
        this.finalizedListener = new Vector<SingleInstanceQueueWorker.WhenProcessed>(5);
    }

}
