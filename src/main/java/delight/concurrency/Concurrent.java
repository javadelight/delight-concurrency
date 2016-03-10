package delight.concurrency;

import delight.async.Operation;
import delight.async.Value;
import delight.async.callbacks.SimpleCallback;
import delight.async.callbacks.ValueCallback;
import delight.concurrency.wrappers.SimpleExecutor;
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

        final Value<SimpleExecutor> executor = new Value<SimpleExecutor>(null);

        sequentialInt(operations, 0, new ArrayList<R>(operations.size()), concurrency, executor, callback);

    }

    public static <R> void sequential(final List<Operation<R>> operations, final Closure<Runnable> asyncExecutor,
            final ValueCallback<List<R>> callback) {
        sequentialInt(operations, 0, new ArrayList<R>(operations.size()), asyncExecutor, callback);
    }

    private static <R> void sequentialInt(final List<Operation<R>> operations, final int idx, final List<R> results,
            final Closure<Runnable> asyncExecutor, final ValueCallback<List<R>> callback) {

        if (idx >= operations.size()) {
            callback.onSuccess(results);

            return;
        }

        operations.get(idx).apply(new ValueCallback<R>() {

            @Override
            public void onFailure(final Throwable t) {

                callback.onFailure(t);
            }

            @Override
            public void onSuccess(final R value) {
                if (results.size() >= idx) {
                    callback.onFailure(
                            new Exception("Callback for operation was already called: " + operations.get(idx)));
                    return;
                }
                results.add(value);

                if (idx == 0 || idx % 4 != 0) {
                    sequentialInt(operations, idx + 1, results, asyncExecutor, callback);
                    return;
                }

                asyncExecutor.apply(new Runnable() {

                    @Override
                    public void run() {
                        sequentialInt(operations, idx + 1, results, asyncExecutor, callback);
                    }
                });

            }
        });

    }

    private static <R> void sequentialInt(final List<Operation<R>> operations, final int idx, final List<R> results,
            final Concurrency concurrency, final Value<SimpleExecutor> executor,
            final ValueCallback<List<R>> callback) {

        if (idx >= operations.size()) {
            final SimpleExecutor exc = executor.get();
            if (exc == null) {
                // System.out.println("No executor required.");
                callback.onSuccess(results);
                return;
            }

            exc.shutdown(new SimpleCallback() {

                @Override
                public void onFailure(final Throwable t) {
                    callback.onFailure(t);
                }

                @Override
                public void onSuccess() {
                    callback.onSuccess(results);
                }
            });

            return;
        }

        operations.get(idx).apply(new ValueCallback<R>() {

            @Override
            public void onFailure(final Throwable t) {

                callback.onFailure(t);
            }

            @Override
            public void onSuccess(final R value) {
                results.add(value);

                if (idx == 0 || idx % 4 != 0) {
                    sequentialInt(operations, idx + 1, results, concurrency, executor, callback);
                    return;
                }
                SimpleExecutor exc = executor.get();

                if (exc == null) {
                    // System.out.println("Create dedicated executor.");
                    exc = concurrency.newExecutor().newSingleThreadExecutor(callback);
                    executor.set(exc);
                }

                exc.execute(new Runnable() {

                    @Override
                    public void run() {
                        sequentialInt(operations, idx + 1, results, concurrency, executor, callback);
                    }
                });

            }
        });

    }

}
