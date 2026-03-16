package com.recordsite.backend.exception;

public class ChampionNotFoundException extends RuntimeException {
    public ChampionNotFoundException(String name) {
        super("Champion not Found: " + name);
    }
}
