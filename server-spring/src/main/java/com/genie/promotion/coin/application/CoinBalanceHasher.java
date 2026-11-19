package com.genie.promotion.coin.application;

import com.genie.promotion.coin.domain.UserCoin;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Băm số dư Điểm chống sửa thẳng CSDL (ENTITY 4.7; plan Điểm Genie D2, P01.03):
 * {@code HMAC-SHA256(WALLET_HMAC_SECRET, "coin:" + userId + ":" + balance)} — hex chữ thường. Tiền tố {@code coin:} để không
 * trùng băm ví shop ({@code wallet:…}) dùng chung khóa. Dữ liệu mẫu ({@code R__seed_demo_70}) tính đúng công thức này.
 */
@Component
public class CoinBalanceHasher {

    private final SecretKeySpec key;

    public CoinBalanceHasher(@Value("${genie.security.wallet-hmac-secret}") String secret) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("Thiếu genie.security.wallet-hmac-secret (WALLET_HMAC_SECRET)");
        }
        this.key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }

    public String hash(long userId, int balance) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(key);
            return HexFormat.of().formatHex(mac.doFinal(("coin:" + userId + ":" + balance).getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("Không tính được băm số dư Điểm", e);
        }
    }

    /** Số dư trong CSDL còn khớp băm (không bị sửa ngoài hệ thống). */
    public boolean matches(UserCoin coin) {
        return coin.verifyHash(hash(coin.getUserId(), coin.getBalance()));
    }

    /** Ghi lại băm sau mỗi lần đổi số dư. */
    public void seal(UserCoin coin) {
        coin.setBalanceHash(hash(coin.getUserId(), coin.getBalance()));
    }
}
