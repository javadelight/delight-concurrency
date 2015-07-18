package delight.concurrency.schedule;

import delight.async.Operation;
import delight.async.Value;
import delight.async.callbacks.ValueCallback;
import delight.concurrency.Concurrency;
import delight.concurrency.internal.schedule.OperationEntry;
import delight.concurrency.wrappers.SimpleAtomicBoolean;
import delight.concurrency.wrappers.SimpleAtomicInteger;
import delight.concurrency.wrappers.SimpleExecutor;
import delight.concurrency.wrappers.SimpleExecutor.WhenExecutorShutDown;
import delight.functional.Success;

import java.util.LinkedList;

public class SequentialOperationScheduler {

    private static final boolean ENABLE_LOG = false;

    private final LinkedList<OperationEntry<Object>> scheduled;
    private final SimpleExecutor executorForIndirectCalls;
    private final SimpleAtomicBoolean running;
    private final SimpleAtomicBoolean shuttingDown;
    private final SimpleAtomicBoolean shutDown;
    private final SimpleAtomicInteger suspendCount;
    private final SimpleAtomicBoolean operationInProgress;

    private final Value<ValueCallback<Success>> shutdownCallback;

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
        runIfRequired();
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
        runIfRequired();
    }

    private final Runnable runIfRequiredRunnable = new Runnable() {

        @Override
        public void run() {
            runIfRequired();
        }

    };

    private final void runIfRequired() {

        if (operationInProgress.get()) {
            return;
        }

        if (suspendCount.get() > 0) {
            if (ENABLE_LOG) {
                System.out.println(this + ": Is suspended ...");
            }
            return;
        }

        OperationEntry<Object> entry = null;

        if (ENABLE_LOG) {
            System.out.println(this + ": Running state [" + running.get() + "]");
        }
        if (running.get() == false) {
            running.set(true);

            synchronized (scheduled) {

                entry = scheduled.pollFirst();
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
            if (scheduled.size() == 0) {

                running.set(false);
                tryShutdown();
                return;
            }
            synchronized (scheduled) {

                entry = scheduled.pollFirst();
            }
        }

        if (entry != null) {
            final OperationEntry<Object> entryClosed = entry;
            this.operationInProgress.set(true);

            if (ENABLE_LOG) {
                System.out.println(this + ": Execute operation " + entry.operation);
            }
            entry.operation.apply(new ValueCallback<Object>() {

                @Override
                public void onFailure(final Throwable t) {
                    operationInProgress.set(false);
                    executorForIndirectCalls.execute(runIfRequiredRunnable);

                    entryClosed.callback.onFailure(t);

                }

                @Override
                public void onSuccess(final Object value) {

                    operationInProgress.set(false);
                    executorForIndirectCalls.execute(runIfRequiredRunnable);

                    entryClosed.callback.onSuccess(value);

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
                    this.executorForIndirectCalls.shutdown(new WhenExecutorShutDown() {

                        @Override
                        public void thenDo() {
                            if (shutDown.compareAndSet(false, true)) {

                                shutdownCallback.get().onSuccess(Success.INSTANCE);
                            }
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

    public SequentialOperationScheduler(final Concurrency concurrency) {
        super();
        this.scheduled = new LinkedList<OperationEntry<Object>>();
        this.running = concurrency.newAtomicBoolean(false);
        this.shuttingDown = concurrency.newAtomicBoolean(false);
        this.shutdownCallback = new Value<ValueCallback<Success>>(null);
        this.executorForIndirectCalls = concurrency.newExecutor().newSingleThreadExecutor(this);
        this.suspendCount = concurrency.newAtomicInteger(0);
        this.operationInProgress = concurrency.newAtomicBoolean(false);
        this.shutDown = concurrency.newAtomicBoolean(false);

    }

}
