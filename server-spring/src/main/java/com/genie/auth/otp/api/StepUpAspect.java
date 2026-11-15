package com.genie.auth.otp.api;

import com.genie.auth.otp.application.OtpFacade;
import com.genie.auth.otp.application.RequireStepUp;
import com.genie.common.error.BusinessException;
import com.genie.common.error.ErrorCode;
import com.genie.common.security.AuthUser;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Kiểm OTP bước 2 cho method có {@link RequireStepUp} (API_SPEC 0.7): đọc {@code X-OTP-Challenge-Id}, {@code X-OTP-Code};
 * challenge phải thuộc người đang đăng nhập và đúng mục đích. Thiếu header → {@code 422 GNE-AUTH-4222}.
 * Dùng aspect (chạy khi gọi method — SAU khi body đã qua {@code @Valid}) thay cho interceptor, để body sai không làm
 * tiêu mất mã OTP.
 */
@Aspect
@Component
public class StepUpAspect {

    public static final String CHALLENGE_HEADER = "X-OTP-Challenge-Id";
    public static final String CODE_HEADER = "X-OTP-Code";

    private final OtpFacade otpFacade;

    public StepUpAspect(OtpFacade otpFacade) {
        this.otpFacade = otpFacade;
    }

    @Before("@annotation(stepUp)")
    public void verify(RequireStepUp stepUp) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof AuthUser user)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        HttpServletRequest request = currentRequest();
        String challengeId = request == null ? null : request.getHeader(CHALLENGE_HEADER);
        String code = request == null ? null : request.getHeader(CODE_HEADER);
        if (challengeId == null || challengeId.isBlank() || code == null || code.isBlank()) {
            throw new BusinessException(ErrorCode.OTP_INVALID, "Thao tác này cần mã OTP gửi tới email.",
                    Map.of("remaining_attempts", 5));
        }
        otpFacade.verifyStepUp(user.id(), stepUp.value(), challengeId.trim(), code.trim());
    }

    private static HttpServletRequest currentRequest() {
        return RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs
                ? attrs.getRequest() : null;
    }
}
