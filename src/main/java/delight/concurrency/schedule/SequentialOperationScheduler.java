package delight.concurrency.schedule;

import delight.async.Operation;
import delight.async.Value;
import delight.async.callbacks.ValueCallback;
import delight.concurrency.Concurrency;
import delight.concurrency.internal.schedule.OperationEntry;
import delight.concurrency.wrappers.SimpleExecutor;
import delight.concurrency.wrappers.SimpleExecutor.WhenExecutorShutDown;
import delight.functional.Success;

import java.util.LinkedList;

public class SequentialOperationScheduler {

    private final LinkedList<OperationEntry<Object>> scheduled;
    private final SimpleExecutor executorForIndirectCalls;
    private final Value<Boolean> running;
    private final Value<Boolean> shuttingDown;

    private final Value<ValueCallback<Success>> shutdownCallback;

    public <R> void schedule(final Operation<R> operation, final ValueCallback<R> callback) {
        synchronized (shuttingDown) {
            if (shuttingDown.get()) {
                throw new IllegalStateException("Trying to schedule operation for shutting down scheduler.");
            }
        }
        synchronized (scheduled) {
            scheduled.add(new OperationEntry<Object>(new Operation<R>() {

                @Override
                public void apply(final ValueCallback<R> callback) {

                }
            }, new ValueCallback<R>() {

                @Override
                public void onFailure(final Throwable t) {
                    // TODO Auto-generated method stub

                }

                @Override
                public void onSuccess(final R value) {
                    // TODO Auto-generated method stub

                }
            }));
        }
    }

    private final Runnable runIfRequiredRunnable = new Runnable() {

        @Override
        public void run() {
            runIfRequired();
        }

    };

    private final void runIfRequired() {
        OperationEntry<Object> entry = null;
        synchronized (running) {
            if (running.get() == false) {
                running.set(true);

                synchronized (scheduled) {
                    if (scheduled.size() == 0) {
                        running.set(false);
                        tryShutdown();
                        return;
                    }
                    entry = scheduled.pop();
                }

            }
        }

        if (entry != null) {
            final OperationEntry<Object> entryClosed = entry;
            entry.operation.apply(new ValueCallback<Object>() {

                @Override
                public void onFailure(final Throwable t) {
                    entryClosed.callback.onFailure(t);
                    executorForIndirectCalls.execute(runIfRequiredRunnable);
                }

                @Override
                public void onSuccess(final Object value) {
                    entryClosed.callback.onSuccess(value);
                    executorForIndirectCalls.execute(runIfRequiredRunnable);
                }
            });
        }

    }

    public void shutdown(final ValueCallback<Success> cb) {
        synchronized (shuttingDown) {
            if (shuttingDown.get()) {
                throw new IllegalStateException("Called shutdown for already shut down scheduler");
            }

            shuttingDown.set(true);
        }

        shutdownCallback.set(cb);

        tryShutdown();
    }

    private final void tryShutdown() {

        synchronized (shuttingDown) {
            if (!shuttingDown.get()) {
                return;
            }
        }

        synchronized (running) {
            if (running.get() == false) {
                synchronized (scheduled) {
                    if (scheduled.isEmpty()) {
                        this.executorForIndirectCalls.shutdown(new WhenExecutorShutDown() {

                            @Override
                            public void thenDo() {
                                shutdownCallback.get().onSuccess(Success.INSTANCE);
                            }

                            @Override
                            public void onFailure(final Throwable t) {
                                shutdownCallback.get().onFailure(t);
                            }
                        });
                        return;
                    }
                }
            }
        }
    }

    public SequentialOperationScheduler(final Concurrency concurrency) {
        super();
        this.scheduled = new LinkedList<OperationEntry<R>>();
        this.running = new Value<Boolean>(false);
        this.shuttingDown = new Value<Boolean>(false);
        this.shutdownCallback = new Value<ValueCallback<Success>>(null);
        this.executorForIndirectCalls = concurrency.newExecutor().newSingleThreadExecutor(this);
    }

}
