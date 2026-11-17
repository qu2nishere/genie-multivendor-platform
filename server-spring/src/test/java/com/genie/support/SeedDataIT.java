package com.genie.support;

import static org.assertj.core.api.Assertions.assertThat;

import com.genie.admin.banner.domain.BannerPosition;
import com.genie.auth.profile.domain.AccountStatus;
import com.genie.catalog.attribute.domain.AttributeDataType;
import com.genie.catalog.product.domain.ProductCondition;
import com.genie.catalog.product.domain.ProductStatus;
import com.genie.catalog.product.domain.ShippingPayer;
import com.genie.common.domain.FundingParty;
import com.genie.common.domain.UserRole;
import com.genie.common.domain.VoucherType;
import com.genie.promotion.coin.domain.CoinTransactionType;
import com.genie.promotion.voucher.domain.DiscountType;
import com.genie.promotion.voucher.domain.VoucherScope;
import com.genie.vendor.kyc.domain.KycOverallResult;
import com.genie.vendor.shop.domain.BusinessType;
import com.genie.vendor.shop.domain.KycStatus;
import com.genie.vendor.shop.domain.KycVerificationMethod;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Dữ liệu mẫu đầy đủ và chạy lại được (plan seed 2026-11-16 D7, S07.01–S07.03): Flyway chạy {@code db/migration} +
 * {@code db/seed} (placeholder {@code seed_demo=true}) trên PostgreSQL 16 thật, khẳng định số dòng tối thiểu, toàn vẹn quan hệ,
 * băm số dư, giá trị enum; rồi chạy lại toàn bộ {@code db/seed} lần 2 — số dòng không đổi.
 * Schema đổi (migration V15+) làm seed vi phạm NOT NULL / CHECK thì test này đỏ trong CI.
 */
@Testcontainers
class SeedDataIT {

    static {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    }

    /** Khóa WALLET_HMAC_SECRET mặc định máy dev (application-local.yml) — seed tính băm theo khóa này. */
    private static final String LOCAL_WALLET_SECRET = "local-wallet-secret";

    private static final List<String> COUNTED_TABLES = List.of("users", "customer_profiles", "categories", "brands",
            "category_attributes", "vendor_profiles", "vendor_wallets", "vendor_shipping_config", "products",
            "product_variants", "product_images", "vouchers", "home_banners", "user_coins", "coin_transactions",
            "daily_checkins", "user_wishlists", "shop_follows", "kyc_results");

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16");

    @BeforeAll
    static void migrateWithSeed() {
        Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration", "classpath:db/seed")
                .placeholders(Map.of("seed_demo", "true", "bootstrap_admin_email", "bootstrap@genie.test",
                        "bootstrap_admin_password_hash", ""))
                .load()
                .migrate();
    }

    @Test
    void seedContainsEnoughConsistentData() throws SQLException, IOException {
        assertThat(count("SELECT count(*) FROM categories")).isGreaterThanOrEqualTo(23);
        assertThat(count("SELECT count(*) FROM categories WHERE slug IS NULL")).isZero();
        assertThat(count("SELECT count(*) - count(DISTINCT slug) FROM categories")).isZero();
        assertThat(count("SELECT count(*) FROM vendor_profiles WHERE kyc_status = 'VERIFIED'")).isGreaterThanOrEqualTo(6);
        // mức cơ bản 20 sản phẩm của mock; có file 90 (S08 — sinh tự động) thì ≥ 200
        boolean bulk = new PathMatchingResourcePatternResolver()
                .getResources("classpath:db/seed/R__seed_demo_90_*.sql").length > 0;
        assertThat(count("SELECT count(*) FROM products WHERE status = 'PUBLISHED'")).isGreaterThanOrEqualTo(bulk ? 200 : 20);
        assertThat(count("""
                SELECT count(*) FROM products p WHERE p.status = 'PUBLISHED' AND (
                    NOT EXISTS (SELECT 1 FROM product_variants v WHERE v.product_id = p.id)
                 OR NOT EXISTS (SELECT 1 FROM product_images i WHERE i.product_id = p.id AND i.is_primary))""")).isZero();
        assertThat(count("""
                SELECT count(*) FROM products p
                WHERE NOT EXISTS (SELECT 1 FROM vendor_profiles vp WHERE vp.user_id = p.vendor_id AND vp.kyc_status = 'VERIFIED')""")).isZero();
        assertThat(count("SELECT count(*) FROM product_images WHERE image_url NOT LIKE 'seed/%'")).isZero();
        assertThat(count("SELECT count(*) FROM vouchers WHERE code IN ('COIN500', 'COIN1000', 'COIN2000')")).isEqualTo(3);
        assertThat(count("SELECT count(*) FROM home_banners WHERE target_url NOT LIKE '/%' OR target_url LIKE '%//%'")).isZero();
        assertThat(count("""
                SELECT count(*) FROM (SELECT position FROM home_banners
                    WHERE is_active AND starts_at <= now() AND ends_at > now() GROUP BY position HAVING count(*) > 8) x""")).isZero();
        assertThat(count("SELECT count(*) FROM vendor_profiles WHERE kyc_status = 'MANUAL_REVIEW'")).isGreaterThanOrEqualTo(3);
        assertThat(queryString("SELECT role FROM users WHERE email = 'mai.tran@gmail.com'")).isEqualTo("VENDOR");
    }

    @Test
    void coinLedgerMatchesBalanceAndHashes() throws SQLException {
        assertThat(count("""
                SELECT count(*) FROM user_coins c
                WHERE c.balance <> (SELECT coalesce(sum(t.amount), 0) FROM coin_transactions t WHERE t.user_id = c.user_id)""")).isZero();
        assertThat(count("SELECT balance FROM user_coins c JOIN users u ON u.id = c.user_id WHERE u.email = 'mai.tran@gmail.com'"))
                .isEqualTo(1250);

        try (Connection c = connect(); Statement st = c.createStatement()) {
            try (ResultSet rs = st.executeQuery("SELECT user_id, balance, balance_hash FROM user_coins")) {
                while (rs.next()) {
                    assertThat(rs.getString(3)).as("balance_hash ví Điểm user %d", rs.getLong(1))
                            .isEqualTo(hmac("coin:" + rs.getLong(1) + ":" + rs.getInt(2)));
                }
            }
            try (ResultSet rs = st.executeQuery("SELECT vendor_id, balance, pending_withdrawal, balance_hash FROM vendor_wallets")) {
                int wallets = 0;
                while (rs.next()) {
                    wallets++;
                    assertThat(rs.getString(4)).as("balance_hash ví shop %d", rs.getLong(1)).isEqualTo(hmac(
                            "wallet:" + rs.getLong(1) + ":" + rs.getBigDecimal(2).toPlainString() + ":"
                                    + rs.getBigDecimal(3).toPlainString()));
                }
                assertThat(wallets).isGreaterThanOrEqualTo(6);
            }
        }
    }

    @Test
    void enumValuesMatchJavaEnums() throws SQLException {
        assertEnum("SELECT DISTINCT role FROM users", UserRole.class);
        assertEnum("SELECT DISTINCT status FROM users", AccountStatus.class);
        assertEnum("SELECT DISTINCT data_type FROM category_attributes", AttributeDataType.class);
        assertEnum("SELECT DISTINCT kyc_status FROM vendor_profiles", KycStatus.class);
        assertEnum("SELECT DISTINCT business_type FROM vendor_profiles WHERE business_type IS NOT NULL", BusinessType.class);
        assertEnum("SELECT DISTINCT kyc_verification_method FROM vendor_profiles WHERE kyc_verification_method IS NOT NULL",
                KycVerificationMethod.class);
        assertEnum("SELECT DISTINCT status FROM products", ProductStatus.class);
        assertEnum("SELECT DISTINCT item_condition FROM products", ProductCondition.class);
        assertEnum("SELECT DISTINCT shipping_payer FROM products WHERE shipping_payer IS NOT NULL", ShippingPayer.class);
        assertEnum("SELECT DISTINCT type FROM vouchers", VoucherType.class);
        assertEnum("SELECT DISTINCT scope FROM vouchers", VoucherScope.class);
        assertEnum("SELECT DISTINCT discount_type FROM vouchers", DiscountType.class);
        assertEnum("SELECT DISTINCT sponsor_type FROM vouchers", FundingParty.class);
        assertEnum("SELECT DISTINCT position FROM home_banners", BannerPosition.class);
        assertEnum("SELECT DISTINCT type FROM coin_transactions", CoinTransactionType.class);
        assertEnum("SELECT DISTINCT overall_status FROM kyc_results", KycOverallResult.class);
    }

    @Test
    void rerunningSeedFilesKeepsRowCounts() throws Exception {
        Map<String, Long> before = rowCounts();
        Resource[] files = new PathMatchingResourcePatternResolver().getResources("classpath:db/seed/R__seed_demo_*.sql");
        Arrays.sort(files, (a, b) -> String.valueOf(a.getFilename()).compareTo(String.valueOf(b.getFilename())));
        assertThat(files).hasSizeGreaterThanOrEqualTo(8);
        try (Connection c = connect()) {
            for (Resource file : files) {
                runScript(c, file);
            }
        }
        assertThat(rowCounts()).isEqualTo(before);
    }

    // ---------- tiện ích ----------

    /** Chạy nguyên văn một file seed trong một transaction (PgJDBC hiểu nhiều câu lệnh và khối $$ … $$). */
    private static void runScript(Connection c, Resource file) throws SQLException, IOException {
        String sql = file.getContentAsString(StandardCharsets.UTF_8);
        assertThat(sql).as("%s không được chứa placeholder Flyway (N3)", file.getFilename()).doesNotContain("${");
        c.setAutoCommit(false);
        try (Statement st = c.createStatement()) {
            st.execute(sql);
            c.commit();
        } catch (SQLException e) {
            c.rollback();
            throw new SQLException("Chạy lại " + file.getFilename() + " lỗi: " + e.getMessage(), e);
        }
    }

    private static Map<String, Long> rowCounts() throws SQLException {
        Map<String, Long> counts = new LinkedHashMap<>();
        for (String table : COUNTED_TABLES) {
            counts.put(table, count("SELECT count(*) FROM " + table));
        }
        return counts;
    }

    private static <E extends Enum<E>> void assertEnum(String sql, Class<E> type) throws SQLException {
        List<String> values = new ArrayList<>();
        try (Connection c = connect(); Statement st = c.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                values.add(rs.getString(1));
            }
        }
        assertThat(values).as(sql).isNotEmpty();
        for (String v : values) {
            assertThat(Enum.valueOf(type, v)).as("%s → %s", sql, v).isNotNull();
        }
    }

    private static long count(String sql) throws SQLException {
        try (Connection c = connect(); Statement st = c.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            rs.next();
            return rs.getLong(1);
        }
    }

    private static String queryString(String sql) throws SQLException {
        try (Connection c = connect(); Statement st = c.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            rs.next();
            return rs.getString(1);
        }
    }

    private static Connection connect() throws SQLException {
        return DriverManager.getConnection(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
    }

    private static String hmac(String message) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(LOCAL_WALLET_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(message.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
