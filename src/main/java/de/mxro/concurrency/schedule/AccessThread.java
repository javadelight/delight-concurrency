package de.mxro.concurrency.schedule;

import one.utils.server.ShutdownCallback;
import de.mxro.concurrency.Concurrency;
import de.mxro.concurrency.wrappers.SimpleExecutor;

/**
 * Used to ensure that there is only one thread working on a client at any one
 * time.
 * 
 * @author Max Rohde
 * 
 */
public interface AccessThread {

    public void shutdown(final ShutdownCallback callback);

    public Concurrency getConcurrency();

    public boolean hasMutex();

    public void acquireMutex();

    public void releaseMutex();

    public void startIfRequired();

    public void addAllOperationsDoneListener(final WhenProcessed whenProcessed);

    public void requestShutdown(final QueueShutdownCallback callback);

    public void offer(final Step item);

    public boolean isRunning();

    public SimpleExecutor getExecutor();

    // public void run(final Notifiyer callWhenFinished);

    /**
     * Gets the underlying system thread.
     * 
     * @return
     */
    public Object getSystemThread();

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
