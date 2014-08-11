package de.mxro.concurrency;

/**
 * Simple factory to create timers for repeating and non-repeating tasks.
 * 
 * @author mroh004
 * 
 */
public interface TimerFactory {

	public SimpleTimer scheduleOnce(int when, Runnable runnable);

	public SimpleTimer scheduleRepeating(int offsetInMs, int intervallInMs,
			Runnable runnable);

}
