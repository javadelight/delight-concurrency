package delight.concurrency.wrappers;

import delight.async.callbacks.SimpleCallback;

public interface SimpleExecutor {

    /**
     * Returns an object representing the thread used to execute the runnable if
     * available.
     * 
     * @param runnable
     * @return
     */
    public void execute(Runnable runnable);

    public void shutdown(SimpleCallback callback);

}
