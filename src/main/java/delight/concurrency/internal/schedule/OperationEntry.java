package delight.concurrency.internal.schedule;

import delight.async.Operation;
import delight.async.callbacks.ValueCallback;

public class OperationEntry<R> {

    public final Operation<R> operation;
    publi final ValueCallback<R> callback;

    public OperationEntry(final Operation<R> operation, final ValueCallback<R> callback) {
        super();
        this.operation = operation;
        this.callback = callback;
    }

}
