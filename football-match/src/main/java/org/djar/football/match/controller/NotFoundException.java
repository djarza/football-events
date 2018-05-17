package org.djar.football.match.controller;

public class NotFoundException extends RuntimeException {

    private Object id;

    public NotFoundException(String message, Object id) {
        super(message);
        this.id = id;
    }

    public Object getId() {
        return id;
    }
}
