package delight.concurrency.schedule;

import delight.async.AsyncCommon;
import delight.async.Operation;
import delight.async.Value;
import delight.async.callbacks.ValueCallback;
import delight.concurrency.Concurrency;
import delight.concurrency.internal.schedule.OperationEntry;
import delight.concurrency.wrappers.SimpleAtomicBoolean;
import delight.concurrency.wrappers.SimpleAtomicInteger;
import delight.concurrency.wrappers.SimpleExecutor;
import delight.functional.Closure;
import delight.functional.Success;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public final class SequentialOperationScheduler {

    private static final boolean ENABLE_LOG = false;

    private final LinkedList<OperationEntry<Object>> scheduled;
    private final SimpleExecutor operationExecutor;
    // private final SimpleExecutor executorForTimeouts;

    private final Concurrency concurrency;

    private final SimpleAtomicBoolean running;
    private final SimpleAtomicBoolean shuttingDown;
    private final SimpleAtomicBoolean shutDown;
    private final SimpleAtomicInteger suspendCount;
    private final SimpleAtomicBoolean operationInProgress;

    private boolean enforceOwnThread;

    private final Value<ValueCallback<Success>> shutdownCallback;

    private int timeout;

    private final SimpleExecutor callbackExecutor;

    public boolean isRunning() {
        synchronized (running) {
            return running.get();
        }
    }

    /**
     * If its NOT running and CAN be suspended, suspend and return true.
     * 
     * @return
     */
    public boolean suspendIfPossible() {

        if (!running.get()) {
            suspend();
            return true;
        }

        return false;

    }

    public void suspend() {
        suspendCount.incrementAndGet();
    }

    public void resume() {
        suspendCount.decrementAndGet();
        runIfRequired(enforceOwnThread);
    }

    @SuppressWarnings("unchecked")
    public <R> void schedule(final Operation<R> operation, final ValueCallback<R> callback) {

        if (shuttingDown.get()) {
            throw new IllegalStateException("Trying to schedule operation for shutting down scheduler.");
        }

        synchronized (scheduled) {
            if (ENABLE_LOG) {
                System.out.println(this + ": Add operation " + operation);
            }
            scheduled.add(new OperationEntry<Object>((Operation<Object>) operation, new ValueCallback<Object>() {

                @Override
                public void onFailure(final Throwable t) {
                    callback.onFailure(t);
                }

                @Override
                public void onSuccess(final Object value) {
                    if (ENABLE_LOG) {
                        System.out.println(SequentialOperationScheduler.this + ": Operation successful " + operation
                                + " returns [" + value + "]");
                    }
                    callback.onSuccess((R) value);
                }
            }));
        }

        if (operationInProgress.get()) {
            return;
        }

        runIfRequired(enforceOwnThread);

    }

    // TODO can this method be improved (made more efficient, easier to
    // understand?
    private synchronized final void runIfRequired(final boolean forceOwnThread) {

        if (suspendCount.get() > 0) {
            if (ENABLE_LOG) {
                System.out.println(this + ": Is suspended ...");
            }
            return;
        }

        if (operationInProgress.get()) {
            return;
        }

        OperationEntry<Object> entry = null;

        synchronized (running) {
            if (ENABLE_LOG) {
                System.out.println(this + ": Running state [" + running.get() + "]");
            }
            if (running.get() == false) {
                running.set(true);

                synchronized (scheduled) {
                    entry = scheduled.poll();
                }

                if (entry == null) {
                    running.set(false);
                    tryShutdown();
                    return;
                }

            } else {
                if (ENABLE_LOG) {

                    System.out.println(this + ": Still to process " + scheduled.size());
                }

                synchronized (scheduled) {

                    if (scheduled.size() == 0) {

                        running.set(false);
                        tryShutdown();
                        return;
                    }

                    entry = scheduled.poll();
                }
            }
        }
        if (entry != null) {
            final OperationEntry<Object> entryClosed = entry;

            if (ENABLE_LOG) {
                System.out.println(this + ": Execute operation " + entry.operation);
            }

            final SimpleAtomicBoolean operationCompleted = concurrency.newAtomicBoolean(false);

            final long operationStartTimestamp = System.currentTimeMillis();

            executeOperation(entryClosed, operationCompleted);

            if (operationCompleted.get() || shutDown.get()) {
                return;
            }

            final Runnable test = createMonitorForTimouts(entryClosed, operationCompleted, operationStartTimestamp);

            concurrency.newTimer().scheduleOnce(500, test);

        }

    }

    private void executeOperation(final OperationEntry<Object> entryClosed,
            final SimpleAtomicBoolean operationCompleted) {
        try {
            this.operationInProgress.set(true);
            entryClosed.operation.apply(new ValueCallback<Object>() {

                @Override
                public void onFailure(final Throwable t) {
                    if (operationCompleted.get()) {
                        throw new RuntimeException("Operation [" + entryClosed.operation
                                + "] failed. Callback cannot be triggered, it was already triggered.", t);
                    }
                    operationCompleted.set(true);
                    operationInProgress.set(false);

                    runIfRequired(true);

                    callbackExecutor.execute(new Runnable() {

                        @Override
                        public void run() {
                            entryClosed.callback.onFailure(t);
                        }
                    });

                }

                @Override
                public void onSuccess(final Object value) {
                    if (operationCompleted.get()) {
                        throw new RuntimeException("Operation [" + entryClosed.operation
                                + "] successful. Callback cannot be triggered, it was already triggered.");
                    }
                    operationCompleted.set(true);
                    operationInProgress.set(false);
                    runIfRequired(true);

                    callbackExecutor.execute(new Runnable() {

                        @Override
                        public void run() {
                            entryClosed.callback.onSuccess(value);
                        }
                    });

                }
            });
        } catch (final Throwable t) {
            operationCompleted.set(true);
            operationInProgress.set(false);

            runIfRequired(true);

            callbackExecutor.execute(new Runnable() {

                @Override
                public void run() {
                    entryClosed.callback.onFailure(t);
                }
            });

        }
    }

    private Runnable createMonitorForTimouts(final OperationEntry<Object> entryClosed,
            final SimpleAtomicBoolean operationCompleted, final long operationStartTimestamp) {
        return new Runnable() {

            @Override
            public void run() {
                if (operationCompleted.get() == true) {
                    return;
                }
                if (System.currentTimeMillis() - operationStartTimestamp > timeout) {

                    operationCompleted.set(true);
                    operationInProgress.set(false);
                    runIfRequired(true);

                    callbackExecutor.execute(new Runnable() {

                        @Override
                        public void run() {
                            entryClosed.callback
                                    .onFailure(new Exception("Operation [" + entryClosed.operation + "] timed out."));
                        }
                    });

                    return;
                }

                concurrency.newTimer().scheduleOnce(500,
                        createMonitorForTimouts(entryClosed, operationCompleted, operationStartTimestamp));
            }
        };
    }

    public void shutdown(final ValueCallback<Success> cb) {

        if (shuttingDown.get()) {
            throw new IllegalStateException("Called shutdown for already shut down scheduler");
        }

        shuttingDown.set(true);

        shutdownCallback.set(cb);

        tryShutdown();
    }

    private final void tryShutdown() {

        if (ENABLE_LOG) {
            System.out.println(this + ": Attempting shutdown .. ");
        }

        if (!shuttingDown.get()) {
            return;
        }

        if (ENABLE_LOG) {
            System.out.println(this + ": Attempting shutdown; running state: " + running.get());
        }
        if (running.get() == false) {
            synchronized (scheduled) {
                if (ENABLE_LOG) {
                    System.out.println(this + ": Attempting shutdown; still scheduled: " + scheduled.size());
                }
                if (scheduled.isEmpty()) {
                    performShutdown();
                    return;
                }
            }
        }

    }

    private final void performShutdown() {

        final List<Operation<Success>> ops = new ArrayList<Operation<Success>>(4);

        ops.add(new Operation<Success>() {

            @Override
            public void apply(final ValueCallback<Success> callback) {
                operationExecutor.shutdown(AsyncCommon.asSimpleCallback(callback));
            }
        });

        ops.add(new Operation<Success>() {

            @Override
            public void apply(final ValueCallback<Success> callback) {
                callbackExecutor.shutdown(AsyncCommon.asSimpleCallback(callback));

            }

        });

        AsyncCommon.sequential(ops, AsyncCommon.embed(shutdownCallback.get(), new Closure<List<Success>>() {

            @Override
            public void apply(final List<Success> o) {
                if (shutDown.compareAndSet(false, true)) {

                    shutdownCallback.get().onSuccess(Success.INSTANCE);
                }
            }
        }));

    }

    public void setTimeout(final int timeoutInMs) {
        this.timeout = timeoutInMs;
    }

    public void setEnforceOwnThread(final boolean value) {
        this.enforceOwnThread = value;
    }

    public SequentialOperationScheduler(final Concurrency concurrency) {
        super();
        assert concurrency != null;
        this.concurrency = concurrency;
        this.scheduled = new LinkedList<OperationEntry<Object>>();
        this.running = concurrency.newAtomicBoolean(false);
        this.shuttingDown = concurrency.newAtomicBoolean(false);
        this.shutdownCallback = new Value<ValueCallback<Success>>(null);
        this.operationExecutor = concurrency.newExecutor().newSingleThreadExecutor(this);

        this.callbackExecutor = concurrency.newExecutor().newParallelExecutor(10, this);

        // this.executorForTimeouts =
        // concurrency.newExecutor().newSingleThreadExecutor(this);

        this.suspendCount = concurrency.newAtomicInteger(0);
        this.operationInProgress = concurrency.newAtomicBoolean(false);
        this.shutDown = concurrency.newAtomicBoolean(false);
        this.timeout = 3000;

        this.enforceOwnThread = false;

    }

}
