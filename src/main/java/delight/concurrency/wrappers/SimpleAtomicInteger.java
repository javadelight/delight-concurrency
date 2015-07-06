package delight.concurrency.wrappers;

public interface SimpleAtomicInteger {

    public int incrementAndGet();

    public int decrementAndGet();

    public int get();

    public int getAndSet(int newValue);

    public void set(int newValue);

}
