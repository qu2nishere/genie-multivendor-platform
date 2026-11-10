package com.genie.common.error;

/**
 * Không tìm thấy tài nguyên (404).
 */
public class NotFoundException extends RuntimeException {

    public NotFoundException(String message) {
        super(message);
    }
}
