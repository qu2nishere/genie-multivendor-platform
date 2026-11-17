package com.genie.auth.oauth.application;

import com.genie.auth.login.application.AuthResult;
import com.genie.auth.login.application.LoginService;
import com.genie.auth.profile.domain.AccountStatus;
import com.genie.auth.profile.domain.CustomerProfile;
import com.genie.auth.profile.domain.CustomerProfileRepository;
import com.genie.auth.profile.domain.User;
import com.genie.auth.profile.domain.UserRepository;
import com.genie.auth.register.event.UserRegisteredEvent;
import com.genie.common.audit.AuditAction;
import com.genie.common.audit.AuditLogger;
import com.genie.common.error.BusinessException;
import com.genie.common.error.ErrorCode;
import com.genie.common.security.ClientInfo;
import com.genie.common.security.Hashing;
import com.genie.common.security.RateLimiter;
import com.genie.common.storage.UploadPurpose;
import com.genie.common.storage.UploadService;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Đăng nhập bằng Google / Zalo — UC-AUTH-03 (API_SPEC 2.9, 2.10; plan AUTH A14.01–A14.03, A14.10).
 * <ol>
 *   <li>{@link #start}: sinh {@code state}, {@code nonce}, PKCE → lưu Redis 10 phút → URL trang đăng nhập Google.
 *       Callback cố định {@code <web-url>/auth/oauth-callback} (trùng URI đã khai báo ở Google Cloud); trang quay về
 *       {@code next} nằm trong state, không nằm trên URL callback.</li>
 *   <li>{@link #complete}: lấy (và xóa) state → đổi {@code code} lấy {@code id_token} đã kiểm → tìm tài khoản:
 *     <ul>
 *       <li>theo {@code (google, sub)} → {@code LOGGED_IN};</li>
 *       <li>không có, nhưng email trùng tài khoản đã đăng ký → tự GẮN Google vào tài khoản đó → {@code LINKED}
 *           (an toàn vì cả Genie — OTP lúc đăng ký — lẫn Google đều đã xác minh email);</li>
 *       <li>chưa có tài khoản → tạo người mua mới (không mật khẩu, email đã xác minh) → {@code CREATED}.</li>
 *     </ul>
 *     Email Google chưa xác minh → {@code 403 GNE-AUTH-4032}; tài khoản đang khóa tạm → {@code 423}; bị đình chỉ →
 *     {@code 403 GNE-AUTH-4031}; tài khoản đã gắn một Google khác → {@code 409 GNE-AUTH-4092}.</li>
 * </ol>
 * <b>Zalo</b> (không trả email — plan D23): đã gắn → đăng nhập như trên; chưa gắn → KHÔNG tự gắn / tạo tài khoản mà trả
 * {@code SIGNUP_REQUIRED} kèm mã phiên đăng ký tạm (Redis 15 phút) — người dùng nhập email, xác minh OTP rồi mới tạo
 * tài khoản ({@link OAuthSignupService}). Liên kết Zalo thủ công phải nhập lại mật khẩu Genie.
 * Họ tên / ảnh đại diện lấy từ Google CHỈ điền vào chỗ còn trống (tài khoản mới, hoặc tài khoản cũ chưa có ảnh) —
 * không ghi đè cái người dùng đã có. Ảnh Google được tải về và lưu vào Silo ({@code avatar-assets/user/{id}/…}); tải
 * lỗi thì bỏ qua, đăng nhập vẫn thành công.
 */
@Service
public class OAuthLoginService {

    public static final Duration STATE_TTL = Duration.ofMinutes(10);
    public static final String CALLBACK_PATH = "/auth/oauth-callback";
    private static final int MAX_NAME = 255;
    private static final Logger log = LoggerFactory.getLogger(OAuthLoginService.class);

    private final List<IdentityProviderClient> clients;
    private final OAuthStateStore states;
    private final UserRepository users;
    private final CustomerProfileRepository profiles;
    private final LoginService loginService;
    private final AuditLogger audit;
    private final ApplicationEventPublisher events;
    private final Clock clock;
    private final String webUrl;
    private final AvatarDownloader avatars;
    private final UploadService uploads;
    private final PasswordEncoder passwordEncoder;
    private final RateLimiter rateLimiter;
    private final PendingOAuthSignupStore signups;

    /** Thời gian chờ nhập email + OTP khi đăng ký bằng Zalo. */
    public static final Duration SIGNUP_TTL = Duration.ofMinutes(15);

    public OAuthLoginService(List<IdentityProviderClient> clients, OAuthStateStore states, UserRepository users,
                             CustomerProfileRepository profiles, LoginService loginService, AuditLogger audit,
                             ApplicationEventPublisher events, Clock clock,
                             @Value("${genie.web-url:http://localhost:4321}") String webUrl,
                             AvatarDownloader avatars, UploadService uploads, PasswordEncoder passwordEncoder,
                             RateLimiter rateLimiter, PendingOAuthSignupStore signups) {
        this.signups = signups;
        this.passwordEncoder = passwordEncoder;
        this.rateLimiter = rateLimiter;
        this.avatars = avatars;
        this.uploads = uploads;
        this.clients = clients;
        this.states = states;
        this.users = users;
        this.profiles = profiles;
        this.loginService = loginService;
        this.audit = audit;
        this.events = events;
        this.clock = clock;
        this.webUrl = webUrl.endsWith("/") ? webUrl.substring(0, webUrl.length() - 1) : webUrl;
    }

    /** Địa chỉ callback trên web — phải trùng tuyệt đối "Authorized redirect URI" ở Google Cloud / Callback URL ở Zalo. */
    public String callbackUrl() {
        return webUrl + CALLBACK_PATH;
    }

    /** URL trang đăng nhập của nhà cung cấp; chưa cấu hình client → {@code 503}. */
    public String start(OAuthProvider provider, String next) {
        IdentityProviderClient client = configuredClient(provider);
        String state = Hashing.randomToken(32);
        String nonce = Hashing.randomToken(32);
        String verifier = Hashing.randomToken(48);
        states.save(state, new OAuthState(provider, nonce, verifier, callbackUrl(), safeNext(next), null), STATE_TTL);
        return client.authorizeUrl(state, nonce, challenge(verifier), callbackUrl());
    }

    /** Trang quay về sau khi liên kết ở Tài khoản › Liên kết đăng nhập. */
    public static final String LINK_RETURN_PATH = "/account/logins";

    /**
     * Bắt đầu <b>liên kết</b> vào tài khoản đang đăng nhập (A14.04). Gọi bằng XHR có access token (trình duyệt chuyển
     * hướng không mang token) → trả URL nhà cung cấp; state ghi {@code linkUserId} nên chỉ đúng người này hoàn tất được.
     * <p>Zalo không có email để đối chiếu (Google thì email phải trùng) → phải nhập lại <b>mật khẩu Genie</b> ngay bước này
     * (sai → {@code 401 GNE-AUTH-4011}, tối đa 10 lần / phút); tài khoản đã gắn mạng xã hội khác → {@code 409 4092}
     * trước khi rời trang.
     */
    public String startLink(OAuthProvider provider, long userId, String currentPassword, ClientInfo info) {
        IdentityProviderClient client = configuredClient(provider);
        if (!provider.providesVerifiedEmail()) {
            User user = users.findById(userId).filter(User::isActive)
                    .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
            if (provider.id().equals(user.getOauthProvider())) {
                throw new BusinessException(ErrorCode.IDENTITY_ALREADY_LINKED, Map.of("reason", "ALREADY_LINKED"));
            }
            if (user.getOauthProvider() != null) {
                throw new BusinessException(ErrorCode.IDENTITY_ALREADY_LINKED,
                        Map.of("reason", "ACCOUNT_LINKED_TO_ANOTHER_" + user.getOauthProvider().toUpperCase()));
            }
            rateLimiter.check("rl:pwverify:" + userId, MAX_PASSWORD_CHECKS_PER_MINUTE, Duration.ofMinutes(1));
            if (!user.hasPassword() || currentPassword == null
                    || !passwordEncoder.matches(currentPassword, user.getPassword())) {
                audit.recordIndependent(AuditAction.OAUTH_LINKED, userId, provider.id(),
                        Map.of("reason", "BAD_PASSWORD"), info, false);
                throw new BusinessException(ErrorCode.BAD_CREDENTIALS, "Mật khẩu hiện tại không chính xác.");
            }
        }
        String state = Hashing.randomToken(32);
        String nonce = Hashing.randomToken(32);
        String verifier = Hashing.randomToken(48);
        states.save(state, new OAuthState(provider, nonce, verifier, callbackUrl(), LINK_RETURN_PATH, userId),
                STATE_TTL);
        return client.authorizeUrl(state, nonce, challenge(verifier), callbackUrl());
    }

    /**
     * Hoàn tất liên kết (API_SPEC 3.3 {@code POST /me/identities/{provider}}): state phải là state liên kết của chính
     * người gọi; email Google chưa xác minh → {@code 403 4032}; <b>email Google phải trùng email tài khoản</b> — khác →
     * {@code 409 4098} (một người = một tài khoản = một email, đồng nhất với tự liên kết theo email khi đăng nhập; không
     * gắn được Gmail của người khác hay Gmail đang là tài khoản Genie khác); Google này đã gắn người khác hoặc tài khoản
     * đã gắn Google khác → {@code 409 4092}. Đã gắn đúng Google này rồi → coi như thành công. Tên / ảnh chỉ điền khi trống.
     */
    @Transactional
    public void link(OAuthProvider provider, long userId, String code, String state, ClientInfo client) {
        // Zalo: không có email — mật khẩu đã kiểm ở startLink, state chỉ dùng được bởi đúng người đó
        OAuthState saved = states.take(state)
                .filter(s -> s.provider() == provider && s.linkUserId() != null && s.linkUserId() == userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.OAUTH_INVALID));
        IdentityClaims claims = client(provider)
                .exchange(code, saved.codeVerifier(), saved.redirectUri(), saved.nonce());
        if (claims.subject() == null || claims.subject().isBlank()) {
            throw new BusinessException(ErrorCode.OAUTH_INVALID);
        }
        if (provider.providesVerifiedEmail() && !claims.emailVerified()) {
            throw new BusinessException(ErrorCode.OAUTH_EMAIL_UNVERIFIED);
        }
        Optional<User> owner = users.findByOauthProviderAndOauthProviderId(provider.id(), claims.subject());
        if (owner.isPresent()) {
            if (owner.get().getId() == userId) {
                return;
            }
            throw new BusinessException(ErrorCode.IDENTITY_ALREADY_LINKED, Map.of("reason", "IDENTITY_OWNED_BY_OTHER"));
        }
        User user = users.findByIdForUpdate(userId).filter(User::isActive)
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
        String providerEmail = claims.email() == null ? null : User.normalizeEmail(claims.email());
        if (provider.providesVerifiedEmail() && (providerEmail == null || !providerEmail.equals(user.getEmail()))) {
            audit.recordIndependent(AuditAction.OAUTH_LINKED, user.getId(), provider.id(),
                    Map.of("reason", "EMAIL_MISMATCH"), client, false);
            throw new BusinessException(ErrorCode.IDENTITY_EMAIL_MISMATCH,
                    Map.of("account_email", user.getEmail()));
        }
        if (user.getOauthProvider() != null) {
            throw new BusinessException(ErrorCode.IDENTITY_ALREADY_LINKED,
                    Map.of("reason", "ACCOUNT_LINKED_TO_ANOTHER_" + user.getOauthProvider().toUpperCase()));
        }
        user.linkIdentity(provider.id(), claims.subject());
        flushOrConflict(user);
        audit.record(AuditAction.OAUTH_LINKED, user.getId(), provider.id(), linkDetails("ACCOUNT", claims), client, true);
        fillProfileGaps(user, claims);
    }

    /** Số lần nhập mật khẩu khi gỡ liên kết mỗi phút / tài khoản — dùng chung bộ đếm với "kiểm mật khẩu hiện tại". */
    static final int MAX_PASSWORD_CHECKS_PER_MINUTE = 10;

    /**
     * Gỡ liên kết (API_SPEC 3.3): chưa có mật khẩu → {@code 409 4093}; phải nhập lại mật khẩu hiện tại (thao tác nhạy cảm
     * — người mượn máy đang đăng nhập không gỡ được), sai → {@code 401 GNE-AUTH-4011}, tối đa 10 lần / phút
     * ({@code 429}). Không gắn provider này → coi như đã gỡ.
     */
    @Transactional
    public void unlink(OAuthProvider provider, long userId, String currentPassword, ClientInfo client) {
        User user = users.findByIdForUpdate(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
        if (!provider.id().equals(user.getOauthProvider())) {
            return;
        }
        if (!user.hasPassword()) {
            throw new BusinessException(ErrorCode.LAST_LOGIN_METHOD);
        }
        rateLimiter.check("rl:pwverify:" + userId, MAX_PASSWORD_CHECKS_PER_MINUTE, Duration.ofMinutes(1));
        if (currentPassword == null || !passwordEncoder.matches(currentPassword, user.getPassword())) {
            // ghi riêng transaction: transaction chính sẽ rollback vì ném lỗi
            audit.recordIndependent(AuditAction.OAUTH_UNLINKED, user.getId(), provider.id(),
                    Map.of("reason", "BAD_PASSWORD"), client, false);
            throw new BusinessException(ErrorCode.BAD_CREDENTIALS, "Mật khẩu hiện tại không chính xác.");
        }
        user.unlinkIdentity();
        audit.record(AuditAction.OAUTH_UNLINKED, user.getId(), provider.id(), null, client, true);
    }

    /**
     * Ghi kèm email Google / tên Zalo để màn "Liên kết đăng nhập" hiện đúng (bảng users không lưu thông tin bên nhà cung
     * cấp — không thêm cột).
     */
    static Map<String, Object> linkDetails(String source, IdentityClaims claims) {
        Map<String, Object> details = new HashMap<>();
        details.put("source", source);
        if (claims.email() != null) {
            details.put("email", claims.email());
        }
        if (claims.name() != null && !claims.name().isBlank()) {
            details.put("name", claims.name().trim());
        }
        return details;
    }

    /** URL web báo lỗi khi không bắt đầu được (trình duyệt đang được chuyển hướng, không đọc được JSON lỗi). */
    public String errorRedirect(String reason) {
        return callbackUrl() + "?error=" + URLEncoder.encode(reason, StandardCharsets.UTF_8);
    }

    @Transactional
    public OAuthLoginResult complete(OAuthProvider provider, String code, String state, ClientInfo client) {
        OAuthState saved = states.take(state).filter(s -> s.provider() == provider && s.linkUserId() == null)
                .orElseThrow(() -> new BusinessException(ErrorCode.OAUTH_INVALID));
        IdentityClaims claims = client(provider)
                .exchange(code, saved.codeVerifier(), saved.redirectUri(), saved.nonce());
        if (claims.subject() == null || claims.subject().isBlank()) {
            throw new BusinessException(ErrorCode.OAUTH_INVALID);
        }
        OffsetDateTime now = OffsetDateTime.now(clock);

        // 1. Đã gắn Google từ trước — tìm theo sub (đổi email ở Genie / Google vẫn đăng nhập được)
        Optional<User> linked = users.findByOauthProviderAndOauthProviderId(provider.id(), claims.subject())
                .flatMap(u -> users.findByIdForUpdate(u.getId()));
        if (linked.isPresent()) {
            User user = linked.get();
            ensureCanSignIn(user, now, client);
            fillProfileGaps(user, claims);
            return done(provider, OAuthLoginResult.Outcome.LOGGED_IN, user, client, saved.next());
        }

        // Zalo: không có email → chờ người dùng nhập email + OTP (không tự gắn, không tạo tài khoản ở bước này)
        if (!provider.providesVerifiedEmail()) {
            String token = Hashing.randomToken(32);
            String name = displayNameOrNull(claims);
            signups.save(token, new PendingOAuthSignup(provider, claims.subject(), name, claims.picture(),
                    saved.next()), SIGNUP_TTL);
            return new OAuthLoginResult(OAuthLoginResult.Outcome.SIGNUP_REQUIRED, null, saved.next(),
                    new OAuthLoginResult.SignupStep(token, provider.name(), name, SIGNUP_TTL.toSeconds()));
        }

        if (!claims.emailVerified() || claims.email() == null || claims.email().isBlank()) {
            throw new BusinessException(ErrorCode.OAUTH_EMAIL_UNVERIFIED);
        }
        String email = User.normalizeEmail(claims.email());

        // 2. Email đã đăng ký → gắn Google vào tài khoản đó
        Optional<User> sameEmail = users.findByEmail(email).flatMap(u -> users.findByIdForUpdate(u.getId()))
                .filter(u -> u.getStatus() != AccountStatus.ANONYMIZED && u.getStatus() != AccountStatus.DELETED);
        if (sameEmail.isPresent()) {
            User user = sameEmail.get();
            ensureCanSignIn(user, now, client);
            if (user.getOauthProvider() != null) {
                throw new BusinessException(ErrorCode.IDENTITY_ALREADY_LINKED,
                        Map.of("reason", "ACCOUNT_LINKED_TO_ANOTHER_" + user.getOauthProvider().toUpperCase()));
            }
            user.linkIdentity(provider.id(), claims.subject());
            if (user.getEmailVerifiedAt() == null) {
                user.verifyEmail(now);
            }
            flushOrConflict(user);
            audit.record(AuditAction.OAUTH_LINKED, user.getId(), provider.id(), linkDetails("LOGIN", claims), client, true);
            fillProfileGaps(user, claims);
            return done(provider, OAuthLoginResult.Outcome.LINKED, user, client, saved.next());
        }

        // 3. Chưa có tài khoản → tạo người mua mới (không mật khẩu; đặt sau ở "Quên mật khẩu")
        User user = createAccount(provider, claims, email, now, client);
        return done(provider, OAuthLoginResult.Outcome.CREATED, user, client, saved.next());
    }

    /**
     * Tạo người mua mới gắn sẵn nhà cung cấp (email đã xác minh — bởi Google, hoặc bằng OTP Genie với Zalo); không mật
     * khẩu. Dùng chung cho đăng nhập Google lần đầu và {@link OAuthSignupService} (Zalo).
     */
    public User createAccount(OAuthProvider provider, IdentityClaims claims, String email, OffsetDateTime now,
                              ClientInfo client) {
        User user = User.newCustomer(email, null, now);
        user.linkIdentity(provider.id(), claims.subject());
        user = flushOrConflict(user);
        CustomerProfile profile = profiles.save(CustomerProfile.create(user, displayName(claims, email), null,
                language()));
        importAvatar(user.getId(), claims.picture()).ifPresent(profile::changeAvatar);
        audit.record(AuditAction.OAUTH_LINKED, user.getId(), provider.id(), linkDetails("SIGN_UP", claims), client, true);
        events.publishEvent(new UserRegisteredEvent(user.getId(), provider.name(), now));
        return user;
    }

    /** Mở phiên đăng nhập sau khi đã xác định được tài khoản. */
    public OAuthLoginResult done(OAuthProvider provider, OAuthLoginResult.Outcome outcome, User user,
                                 ClientInfo client, String next) {
        AuthResult auth = loginService.completeLogin(user, client, provider.name());
        return OAuthLoginResult.signedIn(outcome, auth, next);
    }

    /** Tài khoản đã có: chỉ điền họ tên / ảnh từ Google khi hồ sơ đang trống (không ghi đè). */
    private void fillProfileGaps(User user, IdentityClaims claims) {
        profiles.findByUserIdForUpdate(user.getId()).ifPresent(p -> {
            if (p.getFullName() == null || p.getFullName().isBlank()) {
                p.updateProfile(displayName(claims, user.getEmail()), null, null, null, null, null);
            }
            if (p.getAvatarUrl() == null || p.getAvatarUrl().isBlank()) {
                importAvatar(user.getId(), claims.picture()).ifPresent(p::changeAvatar);
            }
        });
    }

    /** Tải ảnh Google → lưu Silo → URL công khai; mọi lỗi đều bỏ qua (không chặn đăng nhập). */
    private Optional<String> importAvatar(long userId, String pictureUrl) {
        if (pictureUrl == null || pictureUrl.isBlank()) {
            return Optional.empty();
        }
        return avatars.download(pictureUrl, UploadPurpose.AVATAR.maxBytes()).flatMap(image -> {
            try {
                return Optional.ofNullable(uploads.storeImported(userId, UploadPurpose.AVATAR, image.data(),
                        image.contentType()).publicUrl());
            } catch (BusinessException e) {
                log.info("Không lưu được ảnh đại diện mạng xã hội cho user {}: {}", userId, e.getMessage());
                return Optional.empty();
            }
        });
    }

    /** Cùng điều kiện như đăng nhập mật khẩu: khóa tạm → 423, đình chỉ → 403 (hết hạn đình chỉ thì tự mở). */
    private void ensureCanSignIn(User user, OffsetDateTime now, ClientInfo client) {
        if (user.isLocked(now)) {
            throw loginService.lockedError(user);
        }
        if (user.reactivateIfSuspensionExpired(now)) {
            audit.record(AuditAction.ACCOUNT_REACTIVATED, user.getId(), null, Map.of("reason", "SUSPENSION_EXPIRED"),
                    client, true);
        }
        if (user.getStatus() == AccountStatus.SUSPENDED) {
            throw LoginService.suspendedError(user);
        }
    }

    /** Lưu ngay để bắt trùng {@code (provider, sub)} / email khi hai yêu cầu chạy song song. */
    private User flushOrConflict(User user) {
        try {
            return users.saveAndFlush(user);
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(ErrorCode.IDENTITY_ALREADY_LINKED);
        }
    }

    private IdentityProviderClient client(OAuthProvider provider) {
        return clients.stream().filter(c -> c.provider() == provider).findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    }

    /** Chưa có App ID / Secret → {@code 503}. */
    private IdentityProviderClient configuredClient(OAuthProvider provider) {
        IdentityProviderClient client = client(provider);
        if (!client.configured()) {
            throw new BusinessException(ErrorCode.SERVICE_UNAVAILABLE,
                    "Đăng nhập bằng " + provider.label() + " chưa được cấu hình.");
        }
        return client;
    }

    /** Họ tên từ nhà cung cấp; trống → phần trước @ của email. */
    private static String displayName(IdentityClaims claims, String accountEmail) {
        String name = displayNameOrNull(claims);
        if (name == null) {
            String email = claims.email() != null ? claims.email()
                    : accountEmail != null ? accountEmail : "Người dùng Genie";
            int at = email.indexOf('@');
            name = at > 0 ? email.substring(0, at) : email;
        }
        return name;
    }

    private static String displayNameOrNull(IdentityClaims claims) {
        String name = claims.name();
        if (name == null || name.isBlank()) {
            return null;
        }
        name = name.trim();
        return name.length() > MAX_NAME ? name.substring(0, MAX_NAME) : name;
    }

    private static String language() {
        var locale = LocaleContextHolder.getLocale();
        return locale != null && "en".equals(locale.getLanguage()) ? "en" : "vi";
    }

    /** Chỉ nhận đường dẫn nội bộ ({@code /cart}) — chặn chuyển hướng ra ngoài ({@code //evil.com}, {@code https://…}). */
    static String safeNext(String next) {
        if (next == null || next.isBlank() || next.length() > 500) {
            return null;
        }
        if (!next.startsWith("/") || next.startsWith("//") || next.contains("\\")) {
            return null;
        }
        return next;
    }

    /** PKCE S256: base64url(SHA-256(verifier)), không đệm. */
    static String challenge(String verifier) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(verifier.getBytes(StandardCharsets.US_ASCII));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
