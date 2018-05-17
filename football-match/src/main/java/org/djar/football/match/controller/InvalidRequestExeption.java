package org.djar.football.match.controller;

public class InvalidRequestExeption extends RuntimeException {

    public InvalidRequestExeption(String msg) {
        super(msg);
    }
}
