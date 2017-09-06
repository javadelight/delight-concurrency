package delight.concurrency.wrappers;

public interface SimpleReadWriteLock {

	public SimpleLock readLock();
	
	public SimpleLock writeLock();
	
}
