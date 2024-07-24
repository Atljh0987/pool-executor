package org.example;

import java.util.UUID;

public class RestEmulator {
    private int counter = 1;
    private DBEmulator dbEmulator = new DBEmulator();

    public synchronized void updateToken() {
        if(dbEmulator.hasToken()) {
            System.out.println("Token exists");
            return;
        }

        if(counter == 1) {
            System.out.println("Interrupting");
            counter++;
            Thread.currentThread().interrupt();
        }

        System.out.println("Token is calling: " + Thread.currentThread().getName());
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        dbEmulator.setToken(UUID.randomUUID().toString());
    }
}
