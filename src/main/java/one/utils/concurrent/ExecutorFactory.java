package one.utils.concurrent;

public interface ExecutorFactory {

	/**
	 * Assures that only one thread is executed concurrently.
	 * 
	 * @return
	 */
	public abstract Executor newSingleThreadExecutor(Object owner);

	/**
	 * Will execute commands in a number of parallel threads.
	 * 
	 * 
	 * @param maxParallelThreads
	 * @return
	 */
	public abstract Executor newParallelExecutor(int maxParallelThreads,
			final Object owner);

	/**
	 * Will execute all commands immediately as part of the thread which calls
	 * this executor.
	 * 
	 * @return
	 */
	public abstract Executor newImmideateExecutor();

}
