package delight.concurrency.utils;

import delight.async.callbacks.SimpleCallback;
import delight.concurrency.wrappers.SimpleExecutor;
import delight.functional.Function;

/**
 * <p>
 * Allows multiple clients to share one executor.
 * <p>
 * Useful to avoid creation/release of too many threads.
 * 
 * @author <a href="http://www.mxro.de">Max Rohde</a>
 *
 */
public final class SharedExecutor {

    private final Function<Void, SimpleExecutor> executorFactory;
    private volatile SimpleExecutor executor;

    private Integer executorCount;

    public SimpleExecutor reserveExecutor() {

        synchronized (executorCount) {
            executorCount += 1;

            if (executor == null) {
                executor = executorFactory.apply(null);
            }

        }

        return executor;
    }

    public void releaseExecutor(final SimpleCallback callback) {

        synchronized (executorCount) {
            executorCount -= 1;

            executor.shutdown(callback);
            executor = null;

        }

    }

    public SharedExecutor(final Function<Void, SimpleExecutor> executorFactory) {
        super();
        this.executorFactory = executorFactory;
        this.executorCount = 0;
    }

}
