package delight.concurrency.tests;

import delight.async.Operation;
import delight.async.callbacks.ValueCallback;
import delight.async.jre.Async;
import delight.concurrency.jre.ConcurrencyJre;
import delight.concurrency.schedule.SequentialOperationScheduler;
import delight.functional.Success;

import org.junit.Assert;
import org.junit.Test;

public class TestSequentialSchedulerError {

    @Test
    public void test() {

        final SequentialOperationScheduler scheduler = new SequentialOperationScheduler(ConcurrencyJre.create());

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

}
