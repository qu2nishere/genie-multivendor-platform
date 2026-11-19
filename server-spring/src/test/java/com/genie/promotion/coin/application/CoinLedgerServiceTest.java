package com.genie.promotion.coin.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.genie.common.error.BusinessException;
import com.genie.common.error.ErrorCode;
import com.genie.promotion.coin.domain.CoinTransaction;
import com.genie.promotion.coin.domain.CoinTransactionRepository;
import com.genie.promotion.coin.domain.CoinTransactionType;
import com.genie.promotion.coin.domain.UserCoin;
import com.genie.promotion.coin.domain.UserCoinRepository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

/** Sổ Điểm: lô có hạn, thiếu Điểm, FEFO, kiểm băm (plan Điểm Genie P02.04). */
class CoinLedgerServiceTest {

    private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-06-08T10:00:00+07:00");

    private final UserCoinRepository coins = mock(UserCoinRepository.class);
    private final CoinTransactionRepository txs = mock(CoinTransactionRepository.class);
    private final CoinAlertPort alert = mock(CoinAlertPort.class);
    private final CoinBalanceHasher hasher = new CoinBalanceHasher("k");
    private CoinLedgerService ledger;
    private UserCoin coin;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        ObjectProvider<CoinAlertPort> alerts = mock(ObjectProvider.class);
        when(alerts.getIfAvailable()).thenReturn(alert);
        ledger = new CoinLedgerService(coins, txs, hasher,
                new CoinProperties(12, 30, null, List.of()), alerts);
        coin = UserCoin.builder().userId(5L).balance(300).lastUpdated(NOW).build();
        hasher.seal(coin);
        when(coins.findByIdForUpdate(5L)).thenReturn(Optional.of(coin));
        when(txs.save(any())).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    void creditCreatesLotExpiringInTwelveMonthsAndReseals() {
        CoinLedgerService.Entry e = ledger.credit(5L, 100, CoinTransactionType.CHECK_IN, null, "Điểm danh", NOW);
        assertThat(e.balance()).isEqualTo(400);
        assertThat(e.transaction().getAmount()).isEqualTo(100);
        assertThat(e.transaction().getExpiresAt()).isEqualTo(NOW.plusMonths(12));
        assertThat(hasher.matches(coin)).isTrue();
    }

    @Test
    void debitMoreThanBalanceFailsAndKeepsBalance() {
        assertThatThrownBy(() -> ledger.debit(5L, 301, CoinTransactionType.VOUCHER_REDEEM, "9", "x", NOW))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException b = (BusinessException) ex;
                    assertThat(b.getErrorCode()).isEqualTo(ErrorCode.INSUFFICIENT_COINS);
                    assertThat(b.getDetails()).containsEntry("missing", 1);
                });
        assertThat(coin.getBalance()).isEqualTo(300);
        verify(txs, never()).save(any());
    }

    @Test
    void debitWritesOneNegativeRow() {
        CoinLedgerService.Entry e = ledger.debit(5L, 120, CoinTransactionType.VOUCHER_REDEEM, "9", "Đổi voucher", NOW);
        assertThat(e.balance()).isEqualTo(180);
        assertThat(e.transaction().getAmount()).isEqualTo(-120);
        assertThat(e.transaction().getExpiresAt()).isNull();
    }

    @Test
    void tamperedBalanceIsRejectedAndReported() {
        coin.setBalance(99_999);   // sửa thẳng CSDL, băm không đổi
        when(txs.sumAmount(5L)).thenReturn(300L);
        assertThatThrownBy(() -> ledger.credit(5L, 100, CoinTransactionType.CHECK_IN, null, "x", NOW))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCode.INTERNAL_ERROR);
        verify(alert).balanceTampered(5L, 99_999, 300L);
        verify(txs, never()).save(any());
    }

    @Test
    void fefoConsumesEarliestExpiringLotsFirst() {
        OffsetDateTime d1 = NOW.plusDays(10);
        OffsetDateTime d2 = NOW.plusDays(40);
        List<CoinLots.Lot> lots = List.of(new CoinLots.Lot(2, d2, 500), new CoinLots.Lot(1, d1, 150),
                new CoinLots.Lot(3, null, 50));
        assertThat(CoinLots.remainingLots(lots, 100)).containsExactly(
                new CoinLots.Remaining(1, d1, 50), new CoinLots.Remaining(2, d2, 500), new CoinLots.Remaining(3, null, 50));
        assertThat(CoinLots.remainingLots(lots, 650)).containsExactly(new CoinLots.Remaining(3, null, 50));
        assertThat(CoinLots.remainingLots(lots, 700)).isEmpty();
        // sắp hết hạn trong 30 ngày: chỉ phần còn lại của lô d1
        assertThat(CoinLots.expiringBetween(CoinLots.remainingLots(lots, 100), NOW, NOW.plusDays(30))).isEqualTo(50);
        // tiêu hết lô sắp hết hạn → 0
        assertThat(CoinLots.expiringBetween(CoinLots.remainingLots(lots, 150), NOW, NOW.plusDays(30))).isZero();
    }

    @Test
    void expiredButUnspentLotStillShowsForTheExpiryJob() {
        OffsetDateTime past = NOW.minusDays(1);
        List<CoinLots.Remaining> left = CoinLots.remainingLots(List.of(new CoinLots.Lot(1, past, 100)), 30);
        assertThat(left).containsExactly(new CoinLots.Remaining(1, past, 70));
        assertThat(CoinLots.expiringBetween(left, NOW, NOW.plusDays(30))).isZero();
    }

    @Test
    void newWalletIsCreatedWithSealedZeroBalance() {
        when(coins.findByIdForUpdate(anyLong())).thenReturn(Optional.empty());
        when(coins.saveAndFlush(any())).thenAnswer(i -> {
            UserCoin c = i.getArgument(0);
            when(coins.findByIdForUpdate(c.getUserId())).thenReturn(Optional.of(c));
            return c;
        });
        CoinLedgerService.Entry e = ledger.credit(77L, 100, CoinTransactionType.CHECK_IN, null, "x", NOW);
        assertThat(e.balance()).isEqualTo(100);
        assertThat(e.transaction()).isInstanceOf(CoinTransaction.class);
    }
}
