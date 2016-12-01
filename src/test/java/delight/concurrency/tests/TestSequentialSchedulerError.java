package delight.concurrency.tests;

import delight.async.AsyncCommon;
import delight.async.Operation;
import delight.async.callbacks.ValueCallback;
import delight.async.jre.Async;
import delight.concurrency.jre.ConcurrencyJre;
import delight.concurrency.schedule.SequentialOperationScheduler;
import delight.functional.Closure;
import delight.functional.Success;

import org.junit.Assert;
import org.junit.Test;

public class TestSequentialSchedulerError {

    /**
     * Test that even if an error occurs, further operations can be submitted to
     * the scheduler.
     */
    @Test
    public void test_error_in_operation() {

        final SequentialOperationScheduler scheduler = new SequentialOperationScheduler(this, ConcurrencyJre.create());

        try {
            Async.waitFor(new Operation<Success>() {

                @Override
                public void apply(final ValueCallback<Success> callback) {

                    scheduler.schedule(new Operation<Success>() {

                        @Override
                        public void apply(final ValueCallback<Success> callback) {
                            throw new RuntimeException("Error");
                        }

                    }, callback);

                }

            });

            Assert.fail("Exception was expected.");
        } catch (final Throwable t) {
            // as expected
        }

        Async.waitFor(new Operation<Success>() {

            @Override
            public void apply(final ValueCallback<Success> callback) {

                scheduler.schedule(new Operation<Success>() {

                    @Override
                    public void apply(final ValueCallback<Success> callback) {
                        callback.onSuccess(Success.INSTANCE);
                    }

                }, callback);

            }

        });

    }

    /**
     * Test that even if an error occurs, further operations can be submitted to
     * the scheduler.
     * 
     * @throws InterruptedException
     */
    @Test
    public void test_error_in_callback() throws InterruptedException {

        final SequentialOperationScheduler scheduler = new SequentialOperationScheduler(ConcurrencyJre.create());

        try {
            Async.waitFor(new Operation<Success>() {

                @Override
                public void apply(final ValueCallback<Success> callback) {

                    scheduler.schedule(new Operation<Success>() {

                        @Override
                        public void apply(final ValueCallback<Success> callback) {
                            callback.onSuccess(Success.INSTANCE);
                        }

                    }, AsyncCommon.embed(callback, new Closure<Success>() {

                        @Override
                        public void apply(final Success o) {
                            callback.onSuccess(o);

                            throw new RuntimeException("Expected Error!");

                        }

                    }));

                }

            });

            Assert.fail("Exception was expected.");
        } catch (final Throwable t) {
            // as expected
        }

        Thread.sleep(50);

        Async.waitFor(new Operation<Success>() {

            @Override
            public void apply(final ValueCallback<Success> callback) {

                scheduler.schedule(new Operation<Success>() {

                    @Override
                    public void apply(final ValueCallback<Success> callback) {
                        callback.onSuccess(Success.INSTANCE);
                    }

                }, callback);

            }

        });

    }

}
