package com.genie.common.error;

import org.springframework.http.HttpStatus;

/**
 * Mã lỗi {@code GNE-<MIỀN>-<HTTP><STT>} — danh mục đầy đủ ở Document_Vi 3.7.1; thêm mã mới thì thêm vào danh mục.
 */
public enum ErrorCode {

    VALIDATION_FAILED("GNE-VAL-4220", HttpStatus.UNPROCESSABLE_ENTITY),
    RATE_LIMITED("GNE-RAT-4290", HttpStatus.TOO_MANY_REQUESTS),
    OUT_OF_STOCK("GNE-INV-4090", HttpStatus.CONFLICT),
    FLASH_SALE_SOLD_OUT("GNE-INV-4091", HttpStatus.CONFLICT),
    ORDER_CANNOT_CANCEL("GNE-ORD-4092", HttpStatus.CONFLICT),
    SERVICE_UNAVAILABLE("GNE-SYS-5030", HttpStatus.SERVICE_UNAVAILABLE);
    // TODO: bổ sung theo Document_Vi 3.7.1

    private final String code;
    private final HttpStatus status;

    ErrorCode(String code, HttpStatus status) {
        this.code = code;
        this.status = status;
    }

    public String code() {
        return code;
    }

    public HttpStatus status() {
        return status;
    }
}
