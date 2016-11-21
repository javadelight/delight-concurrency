package delight.concurrency.tests;

import delight.async.AsyncCommon;
import delight.async.Operation;
import delight.async.callbacks.ValueCallback;
import delight.async.jre.Async;
import delight.concurrency.jre.JreConcurrency;
import delight.concurrency.wrappers.SimpleExecutor;
import delight.functional.Success;

import org.junit.Test;

public class TestExecutorTimeout {

    @Test(timeout = 1000)
    public void test() throws InterruptedException {
        final SimpleExecutor executor = new JreConcurrency().newExecutor().newParallelExecutor(1, this);

        executor.execute(new Runnable() {

            @Override
            public void run() {
                try {
                    Thread.sleep(150000);
                } catch (final InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }

        }, 50);

        Thread.sleep(300);

        Async.waitFor(new Operation<Success>() {

            @Override
            public void apply(final ValueCallback<Success> callback) {
                executor.shutdown(AsyncCommon.asSimpleCallback(callback));
            }

        });

    }

}
