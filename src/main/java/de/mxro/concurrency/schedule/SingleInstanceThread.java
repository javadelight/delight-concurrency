/**
 * 
 */
package de.mxro.concurrency.schedule;

import de.mxro.concurrency.Concurrency;
import de.mxro.concurrency.wrappers.SimpleAtomicBoolean;
import de.mxro.concurrency.wrappers.SimpleExecutor;
import de.mxro.concurrency.wrappers.SimpleExecutor.WhenExecutorShutDown;

/**
 * A thread of which only one instance runs at any one time.<br/>
 * <b>NOTE: </b>Implementing classes MUST call {@link #notifiyFinished()} in
 * their {@link #run()} implementation.
 * 
 * @author <a href="http://www.mxro.de/">Max Erik Rohde</a>
 * 
 *         Copyright Max Erik Rohde 2011. All rights reserved.
 */
@Deprecated
public abstract class SingleInstanceThread {

    private final SimpleExecutor executor;
    private final SimpleAtomicBoolean isRunning;
    // private volatile long lastCall;
    // private long maxCalltime;
    private final Notifiyer notifiyer;

    private Object workerThread;

    public void startIfRequired() {

        /*
         * if (maxCalltime > -1 && lastCall > -1 && (System.currentTimeMillis()
         * - lastCall) > maxCalltime) { isRunning.set(false); new
         * Exception("Worker thread was manually reset.")
         * .printStackTrace(System.err); }
         */

        if (!isRunning.compareAndSet(false, true)) {
            return;
        }

        workerThread = executor.execute(new Runnable() {

            @Override
            public void run() {
                // assert isRunning.get();
                // lastCall = System.currentTimeMillis();

                SingleInstanceThread.this.run(notifiyer);
            }

        });

    }

    public static interface ThreadStoppedCallback {
        public void onSuccess();

        public void onFailure(Throwable t);
    }

    public void stop(final ThreadStoppedCallback callback) {
        // while (this.isRunning.get()) {
        // Thread.yield();

        // }

        executor.shutdown(new WhenExecutorShutDown() {

            @Override
            public void thenDo() {
                callback.onSuccess();
            }

            @Override
            public void onFailure(final Throwable t) {
                callback.onFailure(t);
            }
        });

    }

    public Object getWorkerThread() {
        return workerThread;
    }

    public class Notifiyer {
        /**
         * This method must be called when all pending operations for this
         * thread are completed.
         */
        public void notifiyFinished() {
            // lastCall = -1;
            isRunning.set(false);

        }
    }

    public Boolean getIsRunning() {
        return isRunning.get();
    }

    public void setMaxCallTime(final long maxCallTimeInMs) {
        // this.maxCalltime = maxCallTimeInMs;
    }

    public SimpleExecutor getExecutor() {
        return executor;
    }

    /**
     * callWhenFinished.notifiyFinished must be called when finished.
     * 
     * @param callWhenFinished
     */
    public abstract void run(Notifiyer callWhenFinished);

    /**
     * Only to create this as dummy.
     */
    public SingleInstanceThread() {
        super();
        this.executor = null;
        this.isRunning = null;
        this.notifiyer = null;
        // this.maxCalltime = -1;
        // this.lastCall = -1;
    }

    public SingleInstanceThread(final SimpleExecutor executor, final Concurrency con) {
        super();
        this.executor = executor;
        this.isRunning = con.newAtomicBoolean(false);
        this.notifiyer = new Notifiyer();
        // this.maxCalltime = -1;
        // this.lastCall = -1;
    }

}
