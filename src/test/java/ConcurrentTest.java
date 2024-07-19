import org.example.ThreadStopper;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class ConcurrentTest {

    @Test
    void test1() {
        final List<ThreadStopper> stoppers = ThreadStopper.getInstances(1_000_000);
        AtomicBoolean switcher = new AtomicBoolean(false);

        ExecutorService executorService = Executors.newFixedThreadPool(1000);

        stoppers.forEach(e -> {
            if(switcher.get()) {
                executorService.submit(() -> e.stop(100));
            } else {
                executorService.submit(() -> e.stop(200));
            }
            switcher.getAndSet(!switcher.get());
        });
    }

    @Test
    void test2() {
        final List<ThreadStopper> stoppers = ThreadStopper.getInstances(5);

        long start = System.currentTimeMillis();

        stoppers.forEach(e -> {
            e.load(20);
        });

        long finish = System.currentTimeMillis();
        System.out.println("Finish: " + (finish - start));
    }

    @Test
    void test3() throws InterruptedException {
        final List<ThreadStopper> stoppers = ThreadStopper.getInstances(50);

        long start = System.currentTimeMillis();

        ExecutorService executorService = Executors.newFixedThreadPool(15);



        stoppers.forEach(e -> {
            executorService.submit(() -> e.load(15));
        });

        executorService.shutdown();
        executorService.awaitTermination(1, TimeUnit.DAYS);

        long finish = System.currentTimeMillis();
        System.out.println("Finish: " + (finish - start));
    }

    @Test
    void test4() throws InterruptedException {
        final List<ThreadStopper> stoppers = ThreadStopper.getInstances(50);
        long start = System.currentTimeMillis();

        ForkJoinPool forkJoinPool = ForkJoinPool.commonPool();

        stoppers.forEach(e -> {
            forkJoinPool.submit(() -> e.load(15));
        });

        forkJoinPool.shutdown();
        forkJoinPool.awaitTermination(1, TimeUnit.DAYS);

        long finish = System.currentTimeMillis();
        System.out.println("Finish: " + (finish - start));
    }

    @Test
    void test5() throws ExecutionException, InterruptedException {
        final List<ThreadStopper> stoppers = ThreadStopper.getInstances(50);

        long start = System.currentTimeMillis();

        List<CompletableFuture<String>> futures = new ArrayList<>();

        stoppers.forEach(e -> futures.add(CompletableFuture.supplyAsync(() -> e.load(15))));

        CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).get();

        long finish = System.currentTimeMillis();
        System.out.println("Finish: " + (finish - start));
    }

    @Test
    void test6() {
        final List<ThreadStopper> stoppers = ThreadStopper.getInstances(10_000);

        long start = System.currentTimeMillis();

        ExecutorService executorService = Executors.newFixedThreadPool(1000);

        stoppers.forEach(e -> {
            executorService.submit(() -> e.load(15));
        });

        long finish = System.currentTimeMillis();
        System.out.println("Finish: " + (finish - start));
    }

    AtomicInteger switcher = new AtomicInteger(1);

    @Test
    void test7() {
        int poolSize = 2 ;

        final List<ThreadStopper> stoppers = ThreadStopper.getInstances(50_000);
        final LinkedList<ThreadStopper> linkedListStoppers = new LinkedList<>(stoppers);

        final AtomicInteger limit = new AtomicInteger(stoppers.size());
        final AtomicInteger pool = new AtomicInteger(poolSize);

        final List<CompletableFuture<String>> tasks = new LinkedList<>();

        while(limit.get() > 0) {
            while(pool.get() > 0) {
                ThreadStopper threadStopper = linkedListStoppers.removeFirst();
                tasks.add(CompletableFuture.supplyAsync(() -> {
                    try {
                        switcher.getAndIncrement();
                        if(switcher.get() % 2 == 0) {
                            return switcher.get() + " " + threadStopper.stop(1000);
                        } else {
                            return switcher.get() + " " + threadStopper.stop(10000);
                        }
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }));
                pool.getAndDecrement();
                limit.getAndSet(linkedListStoppers.size());
            }

            taskLoop:
            while (!tasks.isEmpty()) {
                 for(int i = 0; i < tasks.size(); i++) {
                    var e = tasks.get(i);
                    if(e.isDone()) {
                        System.out.println(e.getNow("Exception"));
                        tasks.remove(e);
                        pool.getAndIncrement();
                        break taskLoop;
                    }
                }
            }
        }
    }
}
