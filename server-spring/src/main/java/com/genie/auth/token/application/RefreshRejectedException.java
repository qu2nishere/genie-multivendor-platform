package com.genie.auth.token.application;

import com.genie.common.error.BusinessException;
import com.genie.common.error.ErrorCode;

/**
 * Làm mới phiên thất bại ({@code 401 GNE-SEC-4010}). {@code clearCookie = false} khi token vừa xoay vòng trong 10 giây
 * ân hạn (tab khác đã làm mới — cookie trình duyệt đang giữ là cookie MỚI, không được xóa; plan AUTH mục 9).
 */
public class RefreshRejectedException extends BusinessException {

    private final boolean clearCookie;

    public RefreshRejectedException(boolean clearCookie) {
        super(ErrorCode.UNAUTHORIZED);
        this.clearCookie = clearCookie;
    }

    public boolean isClearCookie() {
        return clearCookie;
    }
}
