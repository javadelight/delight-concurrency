package delight.concurrency.wrappers;

/**
 * A simple lock implementation.
 * 
 * @author Max
 * 
 */
public interface SimpleLock {

	public abstract void lock();

	public abstract void unlock();

	public abstract boolean isHeldByCurrentThread();

}
