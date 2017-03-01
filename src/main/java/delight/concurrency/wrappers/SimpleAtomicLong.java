package delight.concurrency.wrappers;

public interface SimpleAtomicLong {
    public long incrementAndGet();

    public long decrementAndGet();

    public long get();

    public long getAndSet(long newValue);

    public void set(long newValue);
}
