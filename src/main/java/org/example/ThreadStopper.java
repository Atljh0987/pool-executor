package org.example;

import java.util.ArrayList;
import java.util.List;

public class ThreadStopper {

    public String noLoad() {
        return "NoLoad";
    }

    public String load(int level) {
        ArrayList<Integer> integers = new ArrayList<>();
        var threadName = Thread.currentThread().getName();

        System.out.println("Load: " + threadName + " stating " + level);
        long start = System.currentTimeMillis();
        for(int i = 0; i < level * 10000; i++) {
            integers.add(0, i);
        }

        while (!integers.isEmpty()) {
            integers.remove(0);
        }

        long finish = System.currentTimeMillis();
        System.out.println("Load: " + threadName + " finished " + level + ". Time: " + (finish - start));

        return "Load " + threadName + " Return " + level;
    }

    public String stop(int mills) throws InterruptedException {
        var threadName = Thread.currentThread().getName();

        var thread = new Thread(() -> {
            try {
                Thread.sleep(mills);
            } catch (InterruptedException e) {
                System.out.println(threadName + " exception " + mills);
                throw new RuntimeException(e);
            }
        });

        System.out.println("Stop: " + thread.getName() + " stating " + mills);

        thread.start();
        thread.join();

        System.out.println("Stop: " + thread.getName() + " finished " + mills);

        return "Stop " + threadName + " Return " + mills;
    }

    public static List<ThreadStopper> getInstances(int count) {
        var list = new ArrayList<ThreadStopper>();

        for(int i = 0; i < count; i++) {
            list.add(new ThreadStopper());
        }

        return list;
    }
}
