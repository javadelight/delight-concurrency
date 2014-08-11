package de.mxro.concurrency;

/**
 * Abstraction of basic concurrency operations, most of which can be emulated in a one-thread
 * environment (like JavaScript).
 * 
 * @author <a href="http://www.mxro.de/">Max Rohde</a>
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
