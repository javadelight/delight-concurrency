package delight.concurrency.tests;

import delight.concurrency.jre.JreConcurrency;
import delight.concurrency.wrappers.SimpleExecutor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Assert;
import org.junit.Test;

public class TestParallelExecutor {

    SimpleExecutor executor;

    AtomicInteger runCount;

    AtomicInteger maxCount;

    private class FireThread implements Runnable {

        @Override
        public void run() {

            executor.execute(new Runnable() {

                @Override
                public void run() {

                    final int newCount = runCount.incrementAndGet();
                    Assert.assertTrue(newCount <= 5);

                    if (newCount > maxCount.get()) {
                        maxCount.set(newCount);
                    }

                    // final long startTime = System.currentTimeMillis();

                    try {
                        Thread.sleep(10);
                    } catch (final InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    runCount.decrementAndGet();

                }

            });

        }

    }

    @Test
    public void test_non_blocking() throws InterruptedException {

        executor = new JreConcurrency().newExecutor().newParallelExecutor(5, this);
        runCount = new AtomicInteger(0);
        maxCount = new AtomicInteger(0);
        new Thread(new Runnable() {

            @Override
            public void run() {

                for (int i = 0; i < 50; i++) {
                    new Thread(new FireThread()).start();
                }

            }

        }).start();

        Thread.sleep(100);

        Assert.assertTrue("Got count: " + maxCount.get(), maxCount.get() > 3);

        final List<String> list = Collections.synchronizedList(new ArrayList<String>());

        executor.execute(new Runnable() {

            @Override
            public void run() {
                list.add("2");
            }

        });

        list.add("1");

        Thread.sleep(500);

        Assert.assertEquals("1", list.get(0));
        Assert.assertEquals("2", list.get(1));

    }

}
