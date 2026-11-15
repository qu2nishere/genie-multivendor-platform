package com.genie.common.security;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

/**
 * Lấy người đang đăng nhập trong controller: {@code public X me(@CurrentUser AuthUser me)}.
 * {@code null} khi endpoint cho phép khách vãng lai và request không kèm token hợp lệ.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@AuthenticationPrincipal
public @interface CurrentUser {
}
