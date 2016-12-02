package delight.concurrency.schedule;

import delight.async.AsyncCommon;
import delight.async.Operation;
import delight.async.Value;
import delight.async.callbacks.ValueCallback;
import delight.concurrency.Concurrency;
import delight.concurrency.internal.schedule.OperationEntry;
import delight.concurrency.schedule.timeout.TimeoutWatcher;
import delight.concurrency.wrappers.SimpleAtomicBoolean;
import delight.concurrency.wrappers.SimpleAtomicInteger;
import delight.concurrency.wrappers.SimpleExecutor;
import delight.functional.Closure;
import delight.functional.Function;
import delight.functional.Success;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

public final class SequentialOperationScheduler {

    private static final boolean ENABLE_LOG = false;

    @SuppressWarnings("rawtypes")
    private final Queue<OperationEntry> scheduled;
    private final SimpleExecutor operationExecutor;
    // private final SimpleExecutor executorForTimeouts;

    private final Concurrency concurrency;

    private final SimpleAtomicBoolean shuttingDown;
    private final SimpleAtomicBoolean shutDown;
    private final SimpleAtomicInteger suspendCount;
    private final SimpleAtomicBoolean operationInProgress;

    private boolean enforceOwnThread;

    private final Value<ValueCallback<Success>> shutdownCallback;

    private int timeout;

    private final SimpleExecutor callbackExecutor;

    private final Object owner;

    private final TimeoutWatcher timeoutWatcher;

    public boolean isRunning() {

        return operationInProgress.get();

    }

    /**
     * If its NOT running and CAN be suspended, suspend and return true.
     * 
     * @return
     */
    public boolean suspendIfPossible() {

        if (!operationInProgress.get()) {
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
                callback.onSuccess((R) value);
            }
        }));

        runIfRequired(enforceOwnThread);

    }

    // TODO can this method be improved (made more efficient, easier to
    // understand?
    @SuppressWarnings("unchecked")
    private final void runIfRequired(final boolean forceOwnThread) {

        if (suspendCount.get() > 0) {
            if (ENABLE_LOG) {
                System.out.println(this + ": Is suspended ...");
            }
            return;
        }

        if (ENABLE_LOG) {
            System.out.println(this + ": Test run required. Is in progress: " + operationInProgress.get());
        }

        if (!operationInProgress.compareAndSet(false, true)) {
            return;
        }
        if (ENABLE_LOG) {
            System.out.println(this + ": Perform run. Is in progress: " + operationInProgress.get());
        }

        OperationEntry<Object> entry = null;

        entry = scheduled.poll();

        if (entry == null) {
            operationInProgress.set(false);
            tryShutdown();
            return;
        }

        if (!enforceOwnThread) {

            executeWithTimeout(entry);
        } else {
            final OperationEntry<Object> entryClosed = entry;
            operationExecutor.execute(new Runnable() {

                @Override
                public void run() {
                    executeWithTimeout(entryClosed);

                }

            });
        }

    }

    private final void executeWithTimeout(final OperationEntry<Object> entry) {
        final SimpleAtomicBoolean operationCompleted = concurrency.newAtomicBoolean(false);

        if (!enforceOwnThread) {

            // TODO: this causes the engine to run much slower.

            // watchForTimeouts(entry, operationCompleted);
        }

        executeOperation(entry, operationCompleted);
        if (operationCompleted.get() || shutDown.get()) {
            return;
        }

        if (enforceOwnThread) {

            watchForTimeouts(entry, operationCompleted);

        }

    }

    private void watchForTimeouts(final OperationEntry<Object> entry, final SimpleAtomicBoolean operationCompleted) {
        this.timeoutWatcher.watch(this.timeout, new Function<Void, Boolean>() {

            @Override
            public Boolean apply(final Void input) {

                return operationCompleted.get();
            }

        }, new Runnable() {

            @Override
            public void run() {
                operationCompleted.set(true);
                operationInProgress.set(false);
                runIfRequired(true);

                System.err.println(SequentialOperationScheduler.this + ": Timeout for operation: " + entry.operation);
            }

        });
    }

    private void executeOperation(final OperationEntry<Object> entryClosed,
            final SimpleAtomicBoolean operationCompleted) {

        if (ENABLE_LOG) {
            System.out.println(this + ": Execute operation " + entryClosed.operation);
        }
        try {

            entryClosed.operation.apply(new ValueCallback<Object>() {

                @Override
                public void onFailure(final Throwable t) {
                    if (ENABLE_LOG) {
                        System.out.println(this + ": Operation failed: " + entryClosed.operation);
                    }

                    operationInProgress.set(false);

                    runIfRequired(true);

                    callbackExecutor.execute(new Runnable() {

                        @Override
                        public void run() {
                            if (operationCompleted.get()) {
                                throw new RuntimeException(
                                        "Operation [" + entryClosed.operation
                                                + "] failed. Callback cannot be triggered, it was already triggered.",
                                        t);
                            }
                            operationCompleted.set(true);

                            entryClosed.callback.onFailure(t);
                        }
                    });

                }

                @Override
                public void onSuccess(final Object value) {
                    if (ENABLE_LOG) {
                        System.out.println(
                                SequentialOperationScheduler.this + ": Operation successful: " + entryClosed.operation);
                    }

                    operationInProgress.set(false);

                    runIfRequired(true);

                    callbackExecutor.execute(new Runnable() {

                        @Override
                        public void run() {
                            if (operationCompleted.get()) {
                                throw new RuntimeException("Operation [" + entryClosed.operation
                                        + "] successful. Callback cannot be triggered, it was already triggered.");
                            }
                            operationCompleted.set(true);

                            entryClosed.callback.onSuccess(value);
                        }
                    });

                }
            });
        } catch (final Throwable t) {

            operationInProgress.set(false);

            runIfRequired(true);

            callbackExecutor.execute(new Runnable() {

                @Override
                public void run() {
                    operationCompleted.set(true);
                    entryClosed.callback.onFailure(t);
                }
            });

        }
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
            System.out.println(this + "->" + owner + ": Attempting shutdown .. ");
        }

        if (!shuttingDown.get()) {
            return;
        }

        if (ENABLE_LOG) {
            System.out.println(
                    this + "->" + owner + ": Attempting shutdown; running state: " + operationInProgress.get());
        }
        if (operationInProgress.get() == false) {

            if (ENABLE_LOG) {
                System.out.println(this + "->" + owner + ": Attempting shutdown; still scheduled: " + scheduled.size());
            }
            if (scheduled.isEmpty()) {
                performShutdown();
                return;
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

        ops.add(new Operation<Success>() {

            @Override
            public void apply(final ValueCallback<Success> callback) {
                timeoutWatcher.shutdown(AsyncCommon.asSimpleCallback(callback));
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

    public SequentialOperationScheduler(final Object owner, final Concurrency concurrency) {
        super();
        assert concurrency != null;
        this.owner = owner;
        this.concurrency = concurrency;
        this.scheduled = concurrency.newCollection().newThreadSafeQueue(OperationEntry.class);

        this.shuttingDown = concurrency.newAtomicBoolean(false);
        this.shutdownCallback = new Value<ValueCallback<Success>>(null);
        this.operationExecutor = concurrency.newExecutor().newSingleThreadExecutor(owner);

        this.callbackExecutor = concurrency.newExecutor().newParallelExecutor(10, owner);

        this.suspendCount = concurrency.newAtomicInteger(0);
        this.operationInProgress = concurrency.newAtomicBoolean(false);
        this.shutDown = concurrency.newAtomicBoolean(false);
        this.timeout = 3000;

        this.timeoutWatcher = new TimeoutWatcher(concurrency);

        this.enforceOwnThread = false;

    }

}
