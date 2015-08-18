package delight.concurrency;

import delight.async.Operation;
import delight.async.Value;
import delight.async.callbacks.ValueCallback;
import delight.concurrency.wrappers.SimpleExecutor;

import java.util.ArrayList;
import java.util.List;

public class Concurrent {

    /**
     * Perform these operations sequentially but their own threads to prevent
     * stack overflows.
     * 
     * @param operations
     * @param concurrency
     * @param callback
     */
    public static <R> void sequential(final List<Operation<R>> operations, final Concurrency concurrency,
            final ValueCallback<List<R>> callback) {

        final List<Operation<R>> modifiedOperations = new ArrayList<Operation<R>>(operations.size());

        sequentialInt(modifiedOperations, 0, new ArrayList<R>(operations.size()), concurrency, callback);

    }

    private static <R> void sequentialInt(final List<Operation<R>> operations, final int idx, final List<R> results,
            final Concurrency concurrency, final ValueCallback<List<R>> callback) {

        if (idx >= operations.size()) {
            callback.onSuccess(results);
            return;
        }

        final Value<SimpleExecutor> executor = new Value<SimpleExecutor>(null);

        operations.get(idx).apply(new ValueCallback<R>() {

            @Override
            public void onFailure(final Throwable t) {
                callback.onFailure(t);
            }

            @Override
            public void onSuccess(final R value) {
                results.add(value);

                if (idx == 0 || idx % 5 != 0) {
                    sequentialInt(operations, idx + 1, results, concurrency, callback);
                    return;
                }
                final SimpleExecutor exc = executor.get();

                if (exc == null) {
                    exec = concurrency.newExecutor().newSingleThreadExecutor(callback);
                }

            }
        });

    }

}
