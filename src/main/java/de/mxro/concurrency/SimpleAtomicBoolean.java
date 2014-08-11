package de.mxro.concurrency;

public interface SimpleAtomicBoolean {

	public boolean compareAndSet(boolean expect, boolean update);

	public boolean get();

	public boolean getAndSet(boolean newValue);

	public void set(boolean newValue);

}
