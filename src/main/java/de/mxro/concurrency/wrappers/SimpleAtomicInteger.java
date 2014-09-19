package de.mxro.concurrency.wrappers;

public interface SimpleAtomicInteger {

    public int incementAndGet();

    public boolean get();

    public boolean getAndSet(boolean newValue);

    public void set(boolean newValue);

}
