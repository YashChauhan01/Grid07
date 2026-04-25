package com.grid07.assignment.exception;

public class CapExceededException extends RuntimeException {
    public CapExceededException(String message) {
        super(message);
    }
}