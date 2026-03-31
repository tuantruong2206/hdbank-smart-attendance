package com.hdbank.attendance.domain.exception;

public class DuplicateCheckInException extends RuntimeException {
    public DuplicateCheckInException(String message) {
        super(message);
    }
}
