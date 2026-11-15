package com.genie.common.error;

/**
 * Vượt giới hạn tần suất → {@code 429 GNE-RAT-4290} + header {@code Retry-After} (API_SPEC 0.11).
 */
public class TooManyRequestsException extends BusinessException {

    private final long retryAfterSeconds;

    public TooManyRequestsException(long retryAfterSeconds) {
        super(ErrorCode.RATE_LIMITED, java.util.Map.of("retry_after", Math.max(1, retryAfterSeconds)));
        this.retryAfterSeconds = Math.max(1, retryAfterSeconds);
    }

    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
