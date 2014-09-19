package de.mxro.concurrency;

import de.mxro.concurrency.factories.CollectionFactory;
import de.mxro.concurrency.factories.ExecutorFactory;
import de.mxro.concurrency.factories.TimerFactory;
import de.mxro.concurrency.wrappers.SimpleLock;
import de.mxro.concurrency.wrappers.SimpleAtomicBoolean;

/**
 * Abstraction of basic concurrency operations which can be emulated in a one-thread
 * environment (like JavaScript).
 * 
 * @author <a href="http://www.mxro.de/">Max Rohde</a>
 * 
 */
public interface Concurrency {

	public abstract TimerFactory newTimer();

	public abstract ExecutorFactory newExecutor();

	public abstract void runLater(Runnable runnable);

	public abstract SimpleLock newLock();

	public abstract CollectionFactory newCollection();

	public abstract SimpleAtomicBoolean newAtomicBoolean(boolean value);
}
