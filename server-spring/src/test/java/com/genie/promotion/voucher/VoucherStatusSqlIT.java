// SQL trạng thái khớp Voucher.status() trên CSDL thật (plan 2026-11-26 voucher D2, V02.06)
package com.genie.promotion.voucher;

import static org.assertj.core.api.Assertions.assertThat;

import com.genie.common.domain.FundingParty;
import com.genie.promotion.voucher.domain.DiscountType;
import com.genie.promotion.voucher.domain.Voucher;
import com.genie.promotion.voucher.domain.VoucherRepository;
import com.genie.promotion.voucher.domain.VoucherScope;
import com.genie.promotion.voucher.domain.VoucherStatus;
import com.genie.promotion.voucher.domain.VoucherStatusSql;
import com.genie.promotion.voucher.domain.VoucherTerms;
import com.genie.support.IntegrationTestBase;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

class VoucherStatusSqlIT extends IntegrationTestBase {

    @Autowired
    VoucherRepository vouchers;

    @Autowired
    NamedParameterJdbcTemplate named;

    private static String code() {
        return "T" + UUID.randomUUID().toString().replace("-", "").substring(0, 9).toUpperCase();
    }

    private Voucher make(OffsetDateTime from, OffsetDateTime to) {
        return Voucher.createPlatform(new VoucherTerms(code(), "Kiểm trạng thái", DiscountType.PERCENTAGE,
                BigDecimal.TEN, BigDecimal.valueOf(10_000), BigDecimal.ZERO, 10, 1, null, from, to, VoucherScope.ALL, null),
                FundingParty.PLATFORM, null);
    }

    @Test
    void sqlConditionsMatchTheDomainStatusOnEveryEdgeCase() {
        OffsetDateTime now = OffsetDateTime.parse(START);
        List<Voucher> all = new ArrayList<>();
        all.add(make(now.plusMinutes(1), now.plusDays(1)));                    // SCHEDULED
        all.add(make(now, now.plusDays(1)));                                    // ACTIVE — đúng giờ bắt đầu
        all.add(make(now.minusDays(2), now));                                   // ENDED — đúng giờ kết thúc
        all.add(make(now.minusDays(2), now.plusSeconds(1)));                    // ACTIVE
        Voucher full = make(now.minusDays(1), now.plusDays(1));
        full.setUsedCount(10);                                                  // hết lượt → ENDED
        all.add(full);
        Voucher almost = make(now.minusDays(1), now.plusDays(1));
        almost.setUsedCount(9);                                                 // ACTIVE
        all.add(almost);
        Voucher budget = make(now.minusDays(1), now.plusDays(1));
        budget.setBudget(BigDecimal.valueOf(100_000));
        budget.setTotalDiscountUsed(BigDecimal.valueOf(100_000));
        budget.setIsActive(false);                                              // tự tắt vì hết ngân sách → ENDED
        all.add(budget);
        Voucher budgetOn = make(now.minusDays(1), now.plusDays(1));
        budgetOn.setBudget(BigDecimal.valueOf(100_000));
        budgetOn.setTotalDiscountUsed(BigDecimal.valueOf(100_000));             // vẫn bật nhưng hết ngân sách → ENDED
        all.add(budgetOn);
        Voucher off = make(now.minusDays(1), now.plusDays(1));
        off.disable();                                                          // DISABLED
        all.add(off);
        Voucher cancelled = make(now.plusDays(1), now.plusDays(2));
        cancelled.disable();                                                    // huỷ lịch → DISABLED
        all.add(cancelled);
        Voucher offEnded = make(now.minusDays(3), now.minusDays(1));
        offEnded.disable();                                                     // tắt rồi mới hết hạn → DISABLED
        all.add(offEnded);
        all.add(make(now.minusDays(3), now.minusDays(1)));                     // ENDED
        vouchers.saveAll(all);

        List<Long> ids = all.stream().map(Voucher::getId).toList();
        MapSqlParameterSource p = new MapSqlParameterSource("now", Timestamp.from(now.toInstant())).addValue("ids", ids);
        for (VoucherStatus st : VoucherStatus.values()) {
            List<Long> bySql = named.queryForList("SELECT v.id FROM vouchers v WHERE v.id IN (:ids) AND "
                    + VoucherStatusSql.of(st) + " ORDER BY v.id", p, Long.class);
            List<Long> byDomain = all.stream().filter(v -> v.status(now) == st).map(Voucher::getId).sorted().toList();
            assertThat(bySql).as(st.name()).isEqualTo(byDomain);
        }
        for (Voucher v : all) {
            String viaCase = named.queryForObject("SELECT " + VoucherStatusSql.CASE + " FROM vouchers v WHERE v.id = :id",
                    new MapSqlParameterSource("now", Timestamp.from(now.toInstant())).addValue("id", v.getId()), String.class);
            assertThat(viaCase).as("voucher %d", v.getId()).isEqualTo(v.status(now).name());
        }
        // mỗi trạng thái đều có ít nhất một ca
        assertThat(all.stream().map(v -> v.status(now)).distinct()).hasSize(4);
    }
}
