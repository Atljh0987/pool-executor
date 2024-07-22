import org.example.Exceptioner;
import org.example.ThreadStopper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

import static org.awaitility.Awaitility.await;

public class ConcurrentTest {

    @Test
    @DisplayName("FixedThreadPool")
    void test1() {
        final List<ThreadStopper> stoppers = ThreadStopper.getInstances(1_000_000);
        AtomicBoolean switcher = new AtomicBoolean(false);

        ExecutorService executorService = Executors.newFixedThreadPool(1000);

        stoppers.forEach(e -> {
            if(switcher.get()) {
                executorService.submit(() -> e.delay(100));
            } else {
                executorService.submit(() -> e.delay(200));
            }
            switcher.getAndSet(!switcher.get());
        });
    }

    @Test
    @DisplayName("Load tester")
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
    @DisplayName("Thread pool stopper")
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
    @DisplayName("ForkJoin tester")
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
    @DisplayName("CompletableFuture tester")
    void test5() throws ExecutionException, InterruptedException {
        final List<ThreadStopper> stoppers = ThreadStopper.getInstances(50);

        long start = System.currentTimeMillis();

        List<CompletableFuture<String>> futures = new ArrayList<>();

        stoppers.forEach(e -> futures.add(CompletableFuture.supplyAsync(() -> e.load(15))));

        CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).get();

        long finish = System.currentTimeMillis();
        System.out.println("Finish: " + (finish - start));
    }

    @Nested
    @DisplayName("Limited pool")
    public class Test7 {
        AtomicInteger switcher = new AtomicInteger(1);

        private String test7Load1(ThreadStopper threadStopper) {
            try {
                switcher.getAndIncrement();
                if(switcher.get() % 2 == 0) {
                    return switcher.get() + " " + threadStopper.delay(1000);
                } else {
                    return switcher.get() + " " + threadStopper.delay(10000);
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        private String test7Load2(ThreadStopper threadStopper) {
            return threadStopper.noLoad();
        }

        private String test7Load3(ThreadStopper threadStopper) {
            return threadStopper.load(10);
        }

        private String test7Load4(UUID uuid, ThreadStopper threadStopper) throws Throwable {
            var fnc = new Supplier<String>() {
                @Override
                public String get() {
                    return threadStopper.load(4);
                }
            };

            switcher.getAndIncrement();
            if(switcher.get() % 2 == 0) {
                return switcher.get() + " " + threadStopper.delay(2000);
            } else {
                return switcher.get() + " " + threadStopper.runExceptionally(uuid, 0, fnc);
            }
        }

        @Test
        void test7() {
            int poolSize = 2;

            final List<ThreadStopper> stoppers = ThreadStopper.getInstances(10);
            final LinkedList<ThreadStopper> linkedListStoppers = new LinkedList<>(stoppers);

            final AtomicInteger limit = new AtomicInteger(stoppers.size());
            final AtomicInteger pool = new AtomicInteger(poolSize);

            final List<CompletableFuture<String>> tasks = new LinkedList<>();

            final UUID uuid = UUID.randomUUID();

            while(limit.get() > 0) {
                while(pool.get() > 0 && !linkedListStoppers.isEmpty()) {
                    ThreadStopper threadStopper = linkedListStoppers.removeFirst();
                    final CompletableFuture<String> completableFuture = new CompletableFuture<>();

                    CompletableFuture.runAsync(() -> {
                        try {
                            completableFuture.complete(this.test7Load4(uuid, threadStopper));
                        } catch (Throwable e) {
                            completableFuture.completeExceptionally(e);
                        }
                    });

                    tasks.add(completableFuture);

                    pool.getAndDecrement();
                    limit.getAndSet(linkedListStoppers.size());
                }

                while (!tasks.isEmpty()) {
                    for(var task : tasks) {
                        if(task.isCompletedExceptionally()) {
                            System.out.println("Completed exceptionally");
                            tasks.remove(task);
                            pool.getAndIncrement();
                        }

                        if(task.isDone() && !task.isCompletedExceptionally()) {
                            System.out.println(task.getNow("Exception"));
                            tasks.remove(task);
                            pool.getAndIncrement();
                        }
                    }
                }
            }

            await().atMost(10, TimeUnit.SECONDS).until(tasks::isEmpty);
        }
    }

    @Test
    @DisplayName("Future context tester")
    void test8() {
        List<Exceptioner> exceptioners = LongStream.range(1, 100).mapToObj(i -> {
            if(List.of(7L, 4L, 20L).contains(i)) {
                return new Exceptioner(i, false);
            }

            return new Exceptioner(i, true);
        }).toList();

        completable(exceptioners);
    }

    private void completable(List<Exceptioner> exceptioners) {
        for (var exceptioner : exceptioners) {
            CompletableFuture.supplyAsync(() -> {
                try {
                    return exceptioner.call();
                } catch (Throwable e) {
                    System.out.println(exceptioner.getId());
                    throw new RuntimeException(e);
                }
            });
        }
    }
}
