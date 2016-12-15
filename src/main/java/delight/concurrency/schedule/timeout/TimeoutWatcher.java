package delight.concurrency.schedule.timeout;

import delight.async.callbacks.SimpleCallback;
import delight.concurrency.Concurrency;
import delight.concurrency.wrappers.SimpleAtomicBoolean;
import delight.functional.Function;

import java.util.Queue;

public final class TimeoutWatcher {

    private final static boolean ENABLE_LOG = false;

    private Queue<TimeoutEntry> monitored;

    private final SimpleAtomicBoolean isShutdown;
    private final SimpleAtomicBoolean isInitialized;

    private final Concurrency con;
    private int invocations = 0;

    private final Runnable runTestRunnable = new Runnable() {

        @Override
        public void run() {
            runTest();
        }

    };

    private final void runTest() {

        if (isShutdown.get()) {
            return;
        }

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
        invocations++;
        if (monitored.size() == 0) {
            if (invocations < 100) {
                con.newTimer().scheduleOnce(100, runTestRunnable);
            } else {
                con.newTimer().scheduleOnce(1500, runTestRunnable);
            }
        } else {
            if (invocations < 200) {
                con.newTimer().scheduleOnce(50, runTestRunnable);
            } else {
                con.newTimer().scheduleOnce(1500, runTestRunnable);
            }
        }

    }

    private final void assertInitialized() {
        if (this.isInitialized.compareAndSet(false, true)) {

            synchronized (this) {
                this.monitored = con.newCollection().newThreadSafeQueue(TimeoutEntry.class);
            }

            con.newTimer().scheduleOnce(15, runTestRunnable);

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
            callback.onSuccess();
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
