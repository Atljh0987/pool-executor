package org.example;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class DBEmulator {
    public static HashMap<Integer, String> unsynchronizedStorage = new HashMap<>();
    public static List<String> tokens = new ArrayList<>();

    public boolean contains(int key) {
        return unsynchronizedStorage.containsKey(key);
    }

    public void set(int key, String val) {
        unsynchronizedStorage.put(key, val);
    }

    public String get(int key) {
        return unsynchronizedStorage.get(key);
    }


    public boolean hasToken() {
        return !tokens.isEmpty();
    }

    public String getToken() {
        return tokens.get(tokens.size() - 1);
    }

    public void setToken(String token) {
        tokens.add(token);
    }
}
