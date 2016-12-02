package delight.concurrency.schedule.timeout;

import delight.functional.Function;

public final class TimeoutEntry {

    public final int timeout;
    public final Function<Void, Boolean> isCompleted;
    public final Runnable onTimeout;
    public final long startTime;

    public TimeoutEntry(final int timeout, final Function<Void, Boolean> isCompleted, final Runnable onTimeout) {
        super();
        this.timeout = timeout;
        this.isCompleted = isCompleted;
        this.onTimeout = onTimeout;
        this.startTime = System.currentTimeMillis();
    }

}
