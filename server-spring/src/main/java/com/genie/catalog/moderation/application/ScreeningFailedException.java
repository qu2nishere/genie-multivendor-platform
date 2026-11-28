// Ngoại lệ khi gọi AI sàng lọc thất bại — UC-CAT-12 (plan C1 P06; c1_auto_product.md mục 4.3)
package com.genie.catalog.moderation.application;

public class ScreeningFailedException extends RuntimeException {

    public ScreeningFailedException(String message) {
        super(message);
    }

    public ScreeningFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}
