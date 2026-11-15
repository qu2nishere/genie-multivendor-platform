package com.genie.auth.register.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.genie.auth.login.application.AuthResult;
import com.genie.auth.login.application.LoginService;
import com.genie.auth.otp.application.IssuedOtp;
import com.genie.auth.otp.application.OtpChallenge;
import com.genie.auth.otp.application.OtpChallengeService;
import com.genie.auth.otp.application.OtpExhaustedException;
import com.genie.auth.otp.application.OtpPurpose;
import com.genie.auth.profile.domain.CustomerProfile;
import com.genie.auth.profile.domain.CustomerProfileRepository;
import com.genie.auth.profile.domain.User;
import com.genie.auth.profile.domain.UserRepository;
import com.genie.auth.register.event.UserRegisteredEvent;
import com.genie.common.error.BusinessException;
import com.genie.common.error.ErrorCode;
import com.genie.common.security.ClientInfo;
import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Map;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Đăng ký người mua bằng email + OTP (UC-AUTH-01, API_SPEC 2.1 / 2.4; plan AUTH A06, D2, D3).
 * <ol>
 *   <li>{@code POST /auth/registrations}: kiểm email chưa có tài khoản ({@code 409 GNE-AUTH-4091}), băm mật khẩu, lưu tạm
 *       Redis {@code register:temp:{email}} 15 phút (không ghi rác vào PostgreSQL), gửi OTP {@code REGISTER};</li>
 *   <li>xác minh đúng mã → tạo {@code users} (CUSTOMER, ACTIVE, đã xác minh email) + {@code customer_profiles}, đăng nhập luôn;</li>
 *   <li>sai mã 5 lần → hủy dữ liệu tạm, phải đăng ký lại.</li>
 * </ol>
 */
@Service
public class RegistrationService {

    public static final Duration TEMP_TTL = Duration.ofMinutes(15);
    private static final String TEMP_KEY = "register:temp:";

    private final UserRepository users;
    private final CustomerProfileRepository profiles;
    private final PasswordEncoder passwordEncoder;
    private final OtpChallengeService otps;
    private final LoginService loginService;
    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    public RegistrationService(UserRepository users, CustomerProfileRepository profiles, PasswordEncoder passwordEncoder,
                               OtpChallengeService otps, LoginService loginService, StringRedisTemplate redis,
                               ObjectMapper objectMapper, ApplicationEventPublisher events, Clock clock) {
        this.users = users;
        this.profiles = profiles;
        this.passwordEncoder = passwordEncoder;
        this.otps = otps;
        this.loginService = loginService;
        this.redis = redis;
        this.objectMapper = objectMapper;
        this.events = events;
        this.clock = clock;
    }

    /** Dữ liệu đăng ký tạm — mật khẩu đã băm trước khi rời service. */
    public record PendingRegistration(String fullName, String phoneNumber, String passwordHash, String language) {
    }

    @Transactional(readOnly = true)
    public IssuedOtp register(String fullName, String rawEmail, String phoneNumber, String password, String language) {
        String email = User.normalizeEmail(rawEmail);
        if (users.existsByEmail(email)) {
            throw new BusinessException(ErrorCode.EMAIL_ALREADY_REGISTERED);
        }
        PendingRegistration pending = new PendingRegistration(fullName.trim(), phoneNumber.trim(),
                passwordEncoder.encode(password), "en".equals(language) ? "en" : "vi");
        try {
            redis.opsForValue().set(TEMP_KEY + email, objectMapper.writeValueAsString(pending), TEMP_TTL);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        } catch (DataAccessException e) {
            throw new BusinessException(ErrorCode.SERVICE_UNAVAILABLE);
        }
        return otps.issue(OtpPurpose.REGISTER, email, null, Map.of(OtpChallengeService.DATA_NAME, pending.fullName()));
    }

    /** Xác minh OTP đăng ký → tạo tài khoản và đăng nhập ({@code 201 $AuthTokens}). */
    @Transactional
    public AuthResult verify(String challengeId, String code, ClientInfo client) {
        OtpChallenge challenge;
        try {
            challenge = otps.verify(challengeId, code, OtpPurpose.REGISTER);
        } catch (OtpExhaustedException e) {
            deleteTemp(e.getChallenge().email());
            throw e;
        }
        String email = challenge.email();
        PendingRegistration pending = readTemp(email);
        if (users.existsByEmail(email)) {
            deleteTemp(email);
            throw new BusinessException(ErrorCode.EMAIL_ALREADY_REGISTERED);
        }
        OffsetDateTime now = OffsetDateTime.now(clock);
        User user;
        try {
            user = users.saveAndFlush(User.newCustomer(email, pending.passwordHash(), now));
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(ErrorCode.EMAIL_ALREADY_REGISTERED);   // đăng ký song song cùng email
        }
        profiles.save(CustomerProfile.create(user, pending.fullName(), pending.phoneNumber(), pending.language()));
        deleteTemp(email);
        events.publishEvent(new UserRegisteredEvent(user.getId(), "EMAIL", now));
        return loginService.completeLogin(user, client, "REGISTER");
    }

    private PendingRegistration readTemp(String email) {
        try {
            String json = redis.opsForValue().get(TEMP_KEY + email);
            if (json == null) {
                throw new BusinessException(ErrorCode.OTP_EXPIRED, "Phiên đăng ký đã hết hạn, vui lòng đăng ký lại.");
            }
            return objectMapper.readValue(json, PendingRegistration.class);
        } catch (JsonProcessingException e) {
            throw new BusinessException(ErrorCode.OTP_EXPIRED, "Phiên đăng ký đã hết hạn, vui lòng đăng ký lại.");
        } catch (DataAccessException e) {
            throw new BusinessException(ErrorCode.SERVICE_UNAVAILABLE);
        }
    }

    private void deleteTemp(String email) {
        try {
            redis.delete(TEMP_KEY + email);
        } catch (DataAccessException ignored) {
            // TTL 15 phút tự dọn
        }
    }
}
