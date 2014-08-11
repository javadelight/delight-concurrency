package de.mxro.concurrency;

/**
 * A simple lock implementation.
 * 
 * @author Max
 * 
 */
public interface Lock {

	public abstract void lock();

	public abstract void unlock();

	public abstract boolean isHeldByCurrentThread();

}
