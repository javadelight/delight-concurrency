package delight.concurrency.schedule;

import delight.async.Operation;
import delight.async.callbacks.ValueCallback;
import delight.concurrency.Concurrency;
import delight.functional.Success;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

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

    /**
     * Schedules to process this item.
     * 
     * @param item
     */
    public void offer(final GItem item) {

        queue.offer(item);

        thread.schedule(new Operation<Object>() {

            @Override
            public void apply(final ValueCallback<Object> callback) {

                while (queue.size() > 0) {
                    final List<GItem> items = new ArrayList<GItem>(queue.size());

                    GItem next;
                    while ((next = queue.poll()) != null) {
                        items.add(next);
                        // break;
                    }

                    processItems(items);

                }

                callback.onSuccess(Success.INSTANCE);
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

    }

    public boolean isRunning() {
        return thread.isRunning();
    }

    public SequentialOperationScheduler getThread() {
        return thread;
    }

    /**
     * Only to create this as a dummy.
     */
    public SingleInstanceQueueWorker() {
        super();
        this.thread = null;
        this.queue = null;
    }

    public SingleInstanceQueueWorker(final Queue<GItem> queue, final Concurrency con) {

        this.thread = new SequentialOperationScheduler(con);
        this.queue = queue;
    }

}
