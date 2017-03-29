package delight.concurrency.jre;

import delight.async.callbacks.SimpleCallback;
import delight.concurrency.schedule.timeout.TimeoutWatcher;
import delight.concurrency.wrappers.SimpleAtomicInteger;
import delight.concurrency.wrappers.SimpleExecutor;
import delight.functional.Function;

import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class JavaExecutor implements SimpleExecutor {

    private ThreadPoolExecutor executor;
    private final TimeoutWatcher timeoutWatcher;

    private final SimpleAtomicInteger running;
    private final SimpleAtomicInteger scheduled;
    private final Function<Void, ThreadPoolExecutor> executorFactory;

    private final void assertExecutor() {
        synchronized (this) {
            if (executor != null) {
                return;
            }

            executor = executorFactory.apply(null);
        }

    }

    @Override
    public void execute(final Runnable runnable, final int timeout, final Runnable onTimeout) {
        assertExecutor();

        scheduled.incrementAndGet();
        final Future<?> future = executor.submit(new Runnable() {

            @Override
            public void run() {
                running.incrementAndGet();
                scheduled.decrementAndGet();

                runnable.run();
                running.decrementAndGet();

            }
        });

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
        assertExecutor();
        this.scheduled.incrementAndGet();
        executor.execute(new Runnable() {

            @Override
            public void run() {
                running.incrementAndGet();
                scheduled.decrementAndGet();

                runnable.run();
                running.decrementAndGet();
            }
        });

    }

    @Override
    public void shutdown(final SimpleCallback callback) {
        synchronized (this) {
            if (executor == null) {
                timeoutWatcher.shutdown(callback);
                return;
            }
        }

        executor.shutdown();

        if (this.running.get() == 0 && this.scheduled.get() == 0) {

            timeoutWatcher.shutdown(callback);
            return;
        }

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

    public JavaExecutor(final Function<Void, ThreadPoolExecutor> executorFactory, final JreConcurrency concurrency) {
        super();
        this.executorFactory = executorFactory;
        this.executor = null;
        this.timeoutWatcher = new TimeoutWatcher(concurrency);
        this.running = concurrency.newAtomicInteger(0);
        this.scheduled = concurrency.newAtomicInteger(0);

    }

    @Override
    public int pendingTasks() {
        synchronized (this) {
            if (executor == null) {
                return 0;
            }
        }
        return executor.getQueue().size();
    }

}