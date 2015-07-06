package delight.concurrency;

import delight.concurrency.factories.CollectionFactory;
import delight.concurrency.factories.ExecutorFactory;
import delight.concurrency.factories.TimerFactory;
import delight.concurrency.wrappers.SimpleAtomicBoolean;
import delight.concurrency.wrappers.SimpleAtomicInteger;
import delight.concurrency.wrappers.SimpleLock;

/**
 * Abstraction of basic concurrency operations which can be emulated in a
 * one-thread environment (like JavaScript).
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

    public abstract SimpleAtomicInteger newAtomicInteger(int value);
}
