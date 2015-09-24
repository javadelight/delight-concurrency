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
    public Object execute(Runnable runnable);

    public interface WhenExecutorShutDown extends SimpleCallback {

        /**
         * Called when no threads spawned by this executor run anymore.
         */
        @Override
        public void onSuccess();

        @Override
        public void onFailure(Throwable t);
    }

    /**
     * Returns the current thread of the caller.
     * 
     * @return
     */
    public Object getCurrentThread();

    public void shutdown(WhenExecutorShutDown callback);

}
