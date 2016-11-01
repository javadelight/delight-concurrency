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

    /**
     * 
     * @param runnable
     * @param timeout
     *            The timeout in ms when the task should cancel.
     */
    public void execute(Runnable runnable, int timeout);

    public void shutdown(SimpleCallback callback);

}
