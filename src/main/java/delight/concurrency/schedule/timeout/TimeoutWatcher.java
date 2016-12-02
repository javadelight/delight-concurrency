package delight.concurrency.schedule.timeout;

import delight.async.callbacks.SimpleCallback;
import delight.concurrency.Concurrency;
import delight.concurrency.wrappers.SimpleAtomicBoolean;
import delight.concurrency.wrappers.SimpleExecutor;
import delight.functional.Function;

import java.util.Queue;

public final class TimeoutWatcher {

    private final static boolean ENABLE_LOG = false;

    private Queue<TimeoutEntry> monitored;
    private SimpleExecutor thread;
    private final SimpleAtomicBoolean isShutdown;
    private final SimpleAtomicBoolean isInitialized;

    private final Concurrency con;

    private final void assertInitialized() {
        if (this.isInitialized.compareAndSet(false, true)) {

            synchronized (this) {
                this.monitored = con.newCollection().newThreadSafeQueue(TimeoutEntry.class);

                this.thread = con.newExecutor().newSingleThreadExecutor(this);
            }

            this.thread.execute(new Runnable() {

                @Override
                public void run() {
                    while (!isShutdown.get()) {

                        final int opcount = Math.max(monitored.size() / 2, 1);

                        if (ENABLE_LOG) {
                            System.out.println(this + ": Checking " + opcount + " threads.");
                        }

                        for (int i = 0; i < opcount; i++) {

                            final TimeoutEntry entry = monitored.poll();
                            if (entry != null && !entry.isCompleted.apply(null)) {

                                if (System.currentTimeMillis() - entry.startTime > entry.timeout) {
                                    if (ENABLE_LOG) {
                                        System.out.println(this + ": Is timed out.");
                                    }
                                    entry.onTimeout.run();
                                } else {
                                    monitored.add(entry);
                                }
                            }
                        }

                        if (ENABLE_LOG) {
                            System.out.println(this + ": Tasks remaining " + monitored.size());
                        }

                        try {
                            if (monitored.size() == 0) {
                                Thread.sleep(100);
                            } else {
                                Thread.sleep(5);
                            }
                        } catch (final InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    }

                }

            });
        }

    }

    public void watch(final int timeout, final Function<Void, Boolean> isCompleted, final Runnable onTimeout) {
        assertInitialized();
        if (monitored == null) {
            synchronized (this) {

            }
        }
        monitored.add(new TimeoutEntry(timeout, isCompleted, onTimeout));
    }

    public void shutdown(final SimpleCallback callback) {
        this.isShutdown.set(true);
        if (this.isInitialized.get()) {
            this.thread.shutdown(callback);
        } else {
            callback.onSuccess();
        }
    }

    public TimeoutWatcher(final Concurrency con) {
        super();
        this.con = con;
        this.isShutdown = con.newAtomicBoolean(false);
        this.isInitialized = con.newAtomicBoolean(false);

    }

}
