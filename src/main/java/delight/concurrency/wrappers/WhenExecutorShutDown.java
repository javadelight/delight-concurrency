package delight.concurrency.wrappers;

import delight.async.callbacks.SimpleCallback;

/**
 * Use {@link SimpleCallback} instead.
 * 
 * @author <a href="http://www.mxro.de">Max Rohde</a>
 *
 */
@Deprecated
public interface WhenExecutorShutDown extends SimpleCallback {

    /**
     * Called when no threads spawned by this executor run anymore.
     */
    @Override
    public void onSuccess();

    @Override
    public void onFailure(Throwable t);
}