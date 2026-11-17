package com.genie.auth.profile.application;

import com.genie.auth.profile.domain.User;
import com.genie.auth.profile.domain.UserRepository;
import com.genie.common.error.BusinessException;
import com.genie.common.error.ErrorCode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@code GET /me} (API_SPEC 3.1, UC-AUTH-02 / 08; plan AUTH A11.02, D13, D18): người dùng + tên / ảnh (khách: hồ sơ;
 * nhân viên: {@link StaffProfileLookup}) + email + cách đăng nhập + shop (cổng {@link AccountShopProvider}) + bộ đếm
 * (cổng {@link AccountCounterContributor}). Web gọi ngay sau đăng nhập / làm mới token để dựng {@code SessionUser}.
 * Một cổng của module khác lỗi thì bỏ qua phần đó (shop = null / đếm = 0), không làm hỏng cả phiên.
 */
@Service
public class MeQueryService {

    private static final Logger log = LoggerFactory.getLogger(MeQueryService.class);

    private final UserRepository users;
    private final AccountSummaryService summaries;
    private final List<AccountShopProvider> shops;
    private final List<AccountCounterContributor> counters;

    public MeQueryService(UserRepository users, AccountSummaryService summaries, List<AccountShopProvider> shops,
                          List<AccountCounterContributor> counters) {
        this.users = users;
        this.summaries = summaries;
        this.shops = shops;
        this.counters = counters;
    }

    @Transactional(readOnly = true)
    public MeView of(long userId) {
        User user = users.findById(userId).filter(User::isActive)
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
        UserSummary summary = summaries.summaryOf(user);
        String provider = user.getOauthProvider();
        MeView.LoginMethods methods = new MeView.LoginMethods(user.hasPassword(), "google".equals(provider),
                "zalo".equals(provider));
        return new MeView(user.getId(), user.getRole(), summary.fullName(), summary.avatarUrl(), user.getEmail(),
                methods, shopOf(userId), countersOf(userId));
    }

    private AccountShopProvider.ShopSummary shopOf(long userId) {
        for (AccountShopProvider provider : shops) {
            try {
                var shop = provider.shopOf(userId);
                if (shop.isPresent()) {
                    return shop.get();
                }
            } catch (RuntimeException e) {
                log.warn("Không lấy được shop của user {} từ {}: {}", userId, provider.getClass().getSimpleName(),
                        e.getMessage());
            }
        }
        return null;
    }

    private MeView.Counters countersOf(long userId) {
        Map<String, Long> totals = new HashMap<>();
        for (AccountCounterContributor contributor : counters) {
            try {
                Map<String, Long> part = contributor.countersOf(userId);
                if (part == null) {
                    continue;
                }
                part.forEach((key, value) -> {
                    if (value != null && MeView.Counters.KEYS.contains(key)) {
                        totals.merge(key, Math.max(0L, value), Long::sum);
                    }
                });
            } catch (RuntimeException e) {
                log.warn("Không lấy được bộ đếm của user {} từ {}: {}", userId,
                        contributor.getClass().getSimpleName(), e.getMessage());
            }
        }
        return MeView.Counters.of(totals);
    }
}
