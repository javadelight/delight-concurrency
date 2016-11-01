package delight.concurrency.schedule;

import delight.async.callbacks.SimpleCallback;
import delight.concurrency.Concurrency;
import delight.concurrency.wrappers.SimpleExecutor;

/**
 * Used to ensure that there is only one thread working on a client at any one
 * time.
 * 
 * @author Max Rohde
 * 
 */
public interface AccessThread {

    public void shutdown(final SimpleCallback callback);

    public Concurrency getConcurrency();

    public boolean hasMutex();

    public void acquireMutex();

    public void releaseMutex();

    public void startIfRequired();

    public void addAllOperationsDoneListener(final SimpleCallback whenProcessed);

    public void requestShutdown(final SimpleCallback callback);

    public void offer(final Step item);

    public boolean isRunning();

    public SimpleExecutor getExecutor();

    /**
     * Gets the underlying system thread.
     * 
     * @return
     */
    // public Object getSystemThread();

    /**
     * The current thread.
     * 
     * @return
     */
    public Object currentThread();

    public SingleInstanceThread asSingleInstanceThread();

    public ThreadSpace asThreadSpace();

    public SingleInstanceQueueWorker<Step> asQueueWorker();

}
