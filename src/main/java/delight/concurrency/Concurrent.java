package delight.concurrency;

import delight.async.AsyncCommon;
import delight.async.Operation;
import delight.async.callbacks.ValueCallback;
import delight.concurrency.wrappers.SimpleExecutor;
import delight.concurrency.wrappers.SimpleExecutor.WhenExecutorShutDown;
import delight.functional.Closure;

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

        final SimpleExecutor executor = concurrency.newExecutor().newSingleThreadExecutor(callback);

        for (final Operation<R> op : operations) {
            modifiedOperations.add(new Operation<R>() {

                @Override
                public void apply(final ValueCallback<R> callback) {
                    executor.execute(new Runnable() {

                        @Override
                        public void run() {
                            op.apply(callback);
                        }
                    });
                }
            });
        }

        AsyncCommon.sequential(modifiedOperations, AsyncCommon.embed(callback, new Closure<List<R>>() {

            @Override
            public void apply(final List<R> o) {
                executor.shutdown(new WhenExecutorShutDown() {

                    @Override
                    public void thenDo() {
                        callback.onSuccess(o);
                    }

                    @Override
                    public void onFailure(final Throwable t) {
                        callback.onFailure(t);
                    }
                });
            }
        }));

    }

}
