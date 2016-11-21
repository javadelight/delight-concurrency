package delight.concurrency.jre;

import delight.async.callbacks.SimpleCallback;
import delight.concurrency.factories.TimerFactory;
import delight.concurrency.wrappers.SimpleExecutor;

import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class JavaExecutor implements SimpleExecutor {
    private final ThreadPoolExecutor executor;
    private final TimerFactory timers;

    @Override
    public void execute(final Runnable runnable, final int timeout) {

        final Future<?> future = executor.submit(runnable);

        timers.scheduleOnce(timeout, new Runnable() {

            @Override
            public void run() {
                if (!future.isDone()) {
                    future.cancel(true);
                    System.err.println(this + ": Task exceeded timeout of " + timeout + " ms (Task: " + runnable + ")");
                }
            }
        });

    }

    @Override
    public void execute(final Runnable runnable) {

        executor.execute(new Runnable() {

            @Override
            public void run() {

                runnable.run();
            }
        });

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

    public JavaExecutor(final ThreadPoolExecutor executor, final JreConcurrency concurrency) {
        super();
        this.executor = executor;
        this.timers = concurrency.newTimer();
    }

    @Override
    public int pendingTasks() {

        return executor.getQueue().size();
    }

}