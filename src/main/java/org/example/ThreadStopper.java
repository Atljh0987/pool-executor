package org.example;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Supplier;

public class ThreadStopper {
    private final static Map<UUID, AtomicInteger> exceptionCounters = new ConcurrentHashMap<>();

    public String noLoad() {
        return "NoLoad";
    }

    public String runExceptionally(
            UUID uuid,
            int numBeforeException,
            Supplier<String> function,
            Throwable throwable
    ) throws Throwable {
        if(!exceptionCounters.containsKey(uuid)) {
            exceptionCounters.put(uuid, new AtomicInteger(0));
            return "Run exceptionally " + Thread.currentThread().getName();
        }

        int runCount = exceptionCounters.get(uuid).getAndIncrement();

        if(runCount >= numBeforeException) {
            throw throwable;
        } else {
            return function.get();
        }
    }

    public String runExceptionally(
            UUID uuid,
            int numBeforeException,
            Supplier<String> function
    ) throws Throwable {
        return this.runExceptionally(
                uuid,
                numBeforeException,
                function,
                new RuntimeException("Default exception")
        );
    }

    public String load(int level) {
        ArrayList<Integer> integers = new ArrayList<>();
        var threadName = Thread.currentThread().getName();

        System.out.printf("Type: load. Thread name: %s. Status: starting. Level: %d", threadName, level);
        long start = System.currentTimeMillis();
        for(int i = 0; i < level * 10000; i++) {
            integers.add(0, i);
        }

        while (!integers.isEmpty()) {
            integers.remove(0);
        }

        long finish = System.currentTimeMillis();
        System.out.printf("Type: load. Thread name: %s. Status: finished. Level: %d", threadName, (finish - start));

        return String.format("Type: load. Thread name: %s. Return: %s ", threadName, level);
    }

    public String delay(int mills) throws InterruptedException {
        var threadName = Thread.currentThread().getName();

        var thread = new Thread(() -> {
            try {
                Thread.sleep(mills);
            } catch (InterruptedException e) {
                System.out.printf("Thread name: %s. Exception: %d", threadName, mills);
                throw new RuntimeException(e);
            }
        });

        System.out.printf("Type: delay. Thread name: %s. Status starting. Delay: %d", thread.getName(), mills);

        thread.start();
        thread.join();

        System.out.printf("Type: delay. Thread name: %s. Status: finished. Delay: %d", thread.getName(), mills);

        return String.format("Type: delay. Thread name: %s. Return: %s ", thread.getName(), mills);
    }

    public static List<ThreadStopper> getInstances(int count) {
        var list = new ArrayList<ThreadStopper>();

        for(int i = 0; i < count; i++) {
            list.add(new ThreadStopper());
        }

        return list;
    }
}
