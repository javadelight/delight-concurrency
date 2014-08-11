package de.mxro.concurrency;

/**
 * Basic concurrency operations, most of which can be emulated in a one-thread
 * environment (like JavaScript).
 * 
 * @author mroh004
 * 
 */
public interface Concurrency {

	public abstract TimerFactory newTimer();

	public abstract ExecutorFactory newExecutor();

	public abstract void runLater(Runnable runnable);

	public abstract Lock newLock();

	public abstract CollectionFactory newCollection();

	public abstract SimpleAtomicBoolean newAtomicBoolean(boolean value);
}
