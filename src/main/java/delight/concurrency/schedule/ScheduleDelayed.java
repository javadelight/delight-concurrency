package delight.concurrency.schedule;

import delight.async.callbacks.SimpleCallback;
import delight.concurrency.Concurrency;
import delight.concurrency.internal.schedule.OperationEntry;
import delight.concurrency.wrappers.SimpleAtomicBoolean;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * FIXME Incompleted!
 * 
 * @author <a href="http://www.mxro.de">Max Rohde</a>
 *
 * @param <R>
 */
@Deprecated
public final class ScheduleDelayed<R> {

    private final int delay;
    private final int maxParallelOps;
    private final Concurrency con;

    private final SimpleAtomicBoolean shuttingDown;
    private final SimpleAtomicBoolean shutDown;

    private final SimpleAtomicBoolean processing;

    private final SimpleAtomicBoolean operationScheduled;

    private final List<OperationEntry<R>> operations;

    private final void processOperations() {
        if (operationScheduled.get()) {
            return;
        }
        operationScheduled.set(true);

        con.newTimer().scheduleOnce(delay, new Runnable() {

            @Override
            public void run() {
                operationScheduled.set(false);
                processing.set(true);
                List<OperationEntry<R>> toProcess;
                synchronized (operations) {
                    toProcess = new ArrayList<OperationEntry<R>>(operations);
                }

                // FIXME incomeplted

            }
        });
    }

    public void add(final OperationEntry<R> operation) {
        synchronized (operations) {
            operations.add(operation);
        }
        processOperations();
    }

    public void shutdown(final SimpleCallback callback) {
        this.shuttingDown.set(true);

        if (this.operationScheduled.get() || this.processing.get()) {
            con.newTimer().scheduleOnce(10, new Runnable() {

                @Override
                public void run() {
                    shutdown(callback);
                }
            });
            return;
        }

        this.shutDown.set(true);
        callback.onSuccess();

    }

    public ScheduleDelayed(final int delay, final int maxParallelOps, final Concurrency con) {
        super();
        this.delay = delay;
        this.con = con;
        this.maxParallelOps = maxParallelOps;
        this.shuttingDown = con.newAtomicBoolean(false);
        this.shutDown = con.newAtomicBoolean(false);
        this.operationScheduled = con.newAtomicBoolean(false);
        this.processing = con.newAtomicBoolean(false);

        this.operations = new LinkedList<OperationEntry<R>>();
    }

}
