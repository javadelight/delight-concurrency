package delight.concurrency.jre;

import delight.async.callbacks.SimpleCallback;
import delight.concurrency.wrappers.SimpleExecutor;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class JavaExecutor implements SimpleExecutor {
    private final ThreadPoolExecutor executor;

    @Override
    public void execute(final Callable<Object> callable, final int timeout) {
        final List<Callable<Object>> callables = new ArrayList<Callable<Object>>();

        callables.add(callable);

        try {
            executor.invokeAll(callables, timeout, TimeUnit.MILLISECONDS);
        } catch (final InterruptedException e) {
            throw new RuntimeException(e);
        }

    }

    @Override
    public void execute(final Runnable runnable) {
        /*
         * final CountDownLatch latch = new CountDownLatch(2);
         * 
         * assert !executor.isShutdown() && !executor.isTerminated() :
         * "Cannot execute task as executor is shut down. " +
         * executor.toString() + " " + runnable;
         */

        executor.execute(new Runnable() {

            @Override
            public void run() {
                // lastThread = Thread.currentThread();
                // latch.countDown();
                runnable.run();
            }
        });

        /*
         * latch.countDown();
         * 
         * if (lastThread == null) { try { latch.await(5000,
         * TimeUnit.MILLISECONDS); } catch (final InterruptedException e) {
         * throw new RuntimeException(
         * "Cannot determine handle of thread to be executed."); } }
         * 
         * return lastThread;
         */
    }

    @Override
    public void shutdown(final SimpleCallback callback) {

        executor.shutdown();

        Thread t = new Thread() {

            @Override
            public void run() {
                try {
                    executor.awaitTermination(10000, TimeUnit.MILLISECONDS);
                    callback.onSuccess();
                } catch (final Throwable t) {
                    callback.onFailure(t);
                }

            }

        };
        t.start();
        t = null;

    }

    public JavaExecutor(final ThreadPoolExecutor executor) {
        super();
        this.executor = executor;
    }

    @Override
    public int pendingTasks() {

        return executor.getQueue().size();
    }

}