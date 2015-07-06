package delight.concurrency.wrappers;

/**
 * A simple timer interface, allows the timer to be stopped.
 * 
 * @author mroh004
 * 
 */
public interface SimpleTimer {

	/**
	 * Stops this timer. Running executions are not affected.
	 */
	public void stop();

}
