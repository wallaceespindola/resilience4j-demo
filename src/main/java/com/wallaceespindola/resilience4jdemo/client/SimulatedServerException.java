package com.wallaceespindola.resilience4jdemo.client;

/** Thrown by the simulated downstream client to represent an HTTP 5xx error. */
public class SimulatedServerException extends RuntimeException {

    public SimulatedServerException(String message) {
        super(message);
    }
}
