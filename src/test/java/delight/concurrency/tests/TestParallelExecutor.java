package delight.concurrency.tests;

import delight.concurrency.jre.JreConcurrency;
import delight.concurrency.wrappers.SimpleExecutor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

public class TestParallelExecutor {

    SimpleExecutor executor;

    private class FireThread implements Runnable {

        @Override
        public void run() {

            executor.execute(new Runnable() {

                @Override
                public void run() {
                    System.out.println("run ...");
                    try {
                        Thread.sleep(10);
                    } catch (final InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    System.out.println("done!");

                }

            }, 3000, new Runnable() {

                @Override
                public void run() {

                }
            });

        }

    }

    @Test
    public void test_non_blocking() throws InterruptedException {

        executor = new JreConcurrency().newExecutor().newParallelExecutor(5, this);

        new Thread(new Runnable() {

            @Override
            public void run() {

                for (int i = 0; i < 50; i++) {
                    new Thread(new FireThread()).start();
                }

            }

        }).start();

        Thread.sleep(250);

        Assert.assertTrue("Expected more than 5 pending tasks but got " + executor.pendingTasks(),
                executor.pendingTasks() > 5);

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
