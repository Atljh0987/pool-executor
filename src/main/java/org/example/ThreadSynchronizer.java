package org.example;

public class ThreadSynchronizer {
    private DBEmulator dbEmulator = new DBEmulator();
    private RestEmulator restEmulator = new RestEmulator();

    public void start() {
        if(!dbEmulator.hasToken()) {
            restEmulator.updateToken();
        }

        System.out.println("End. Thread name: " + Thread.currentThread().getName());
    }
}
