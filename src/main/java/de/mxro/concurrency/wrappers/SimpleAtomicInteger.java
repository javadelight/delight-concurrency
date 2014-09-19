package de.mxro.concurrency.wrappers;

public interface SimpleAtomicInteger {

    public int incrementAndGet();

    public int get();

    public int getAndSet(int newValue);

    public void set(int newValue);

}
