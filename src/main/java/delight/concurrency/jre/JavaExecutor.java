package delight.concurrency.jre;

import delight.async.callbacks.SimpleCallback;
import delight.concurrency.schedule.timeout.TimeoutWatcher;
import delight.concurrency.wrappers.SimpleExecutor;
import delight.functional.Function;

import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class JavaExecutor implements SimpleExecutor {
    private final ThreadPoolExecutor executor;
    private final TimeoutWatcher timeoutWatcher;

    @Override
    public void execute(final Runnable runnable, final int timeout, final Runnable onTimeout) {

        final Future<?> future = executor.submit(runnable);

        timeoutWatcher.watch(timeout, new Function<Void, Boolean>() {

            @Override
            public Boolean apply(final Void input) {

                return future.isDone();
            }

        }, new Runnable() {

            @Override
            public void run() {
                future.cancel(true);
                System.err.println(this + ": Task exceeded timeout of " + timeout + " ms (Task: " + runnable + ")");
                onTimeout.run();
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
                    timeoutWatcher.shutdown(callback);

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
        this.timeoutWatcher = new TimeoutWatcher(concurrency);

    }

    @Override
    public int pendingTasks() {

        return executor.getQueue().size();
    }

}