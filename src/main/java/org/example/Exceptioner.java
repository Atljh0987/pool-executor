package org.example;

public class Exceptioner {
    private final long id;
    private final boolean isGood;
    private final Throwable throwable;

    public Exceptioner(long id, boolean isGood) {
        this(id, isGood, new RuntimeException("Everything is exception"));
    }

    public Exceptioner(long id, boolean isGood, Throwable throwable) {
        this.id = id;
        this.isGood = isGood;
        this.throwable = throwable;
    }

    public String call() throws Throwable {
        if(!isGood) {
            throw throwable;
        }

        return "Everything is good";
    }

    public long getId() {
        return id;
    }
}
