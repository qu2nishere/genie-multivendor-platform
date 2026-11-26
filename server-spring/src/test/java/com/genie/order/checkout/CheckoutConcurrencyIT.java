package com.genie.order.checkout;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.genie.support.OrderTestData.Sku;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;

/**
 * Tranh tồn kho khi đặt hàng (LLD 9.1; plan C2 O07.07, mục 8.3): nhiều người cùng đặt SKU còn ít → đúng số đơn bằng tồn,
 * không bán âm; giữ chỗ trên biến thể đã bỏ / sản phẩm ẩn → 4090; mỗi lần ghi tăng {@code version}.
 */
class CheckoutConcurrencyIT extends CheckoutTestSupport {

    @Test
    void concurrentBuyersNeverOversell() throws Exception {
        long shop = data.shop();
        Sku s = data.sku(shop, 100_000, 3);
        int buyers = 8;
        long[] users = new long[buyers];
        long[] addrs = new long[buyers];
        long[] lines = new long[buyers];
        for (int i = 0; i < buyers; i++) {
            users[i] = buyer();
            addrs[i] = data.address(users[i], 79, "26740", true);
            lines[i] = addLine(users[i], s.variantId(), 1);
        }
        long versionBefore = data.version(s.variantId());
        ExecutorService pool = Executors.newFixedThreadPool(buyers);
        CountDownLatch go = new CountDownLatch(1);
        List<Future<Integer>> results = new ArrayList<>();
        for (int i = 0; i < buyers; i++) {
            final int k = i;
            Callable<Integer> job = () -> {
                go.await();
                return place(users[k], addrs[k], "VNPAY", lines[k]).andReturn().getResponse().getStatus();
            };
            results.add(pool.submit(job));
        }
        go.countDown();
        int ok = 0;
        int conflict = 0;
        for (Future<Integer> f : results) {
            int st = f.get();
            if (st == 201) ok++;
            else if (st == 409) conflict++;
        }
        pool.shutdown();
        assertThat(ok).isEqualTo(3);
        assertThat(conflict).isEqualTo(buyers - 3);
        assertThat(data.reserved(s.variantId())).isEqualTo(3);
        assertThat(data.stock(s.variantId())).isEqualTo(3);
        assertThat(data.version(s.variantId())).isEqualTo(versionBefore + 3);
    }

    @Test
    void cannotReserveRemovedVariantOrHiddenProduct() throws Exception {
        long u = buyer();
        long addr = data.address(u, 79, "26740", true);
        long shop = data.shop();
        Sku removed = data.sku(shop, 100_000, 5);
        Sku hidden = data.sku(shop, 100_000, 5);
        long l1 = addLine(u, removed.variantId(), 1);
        long l2 = addLine(u, hidden.variantId(), 1);
        data.deleteVariant(removed.variantId());
        data.setProductStatus(hidden.productId(), "HIDDEN");
        place(u, addr, "COD", l1).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("GNE-INV-4090"));
        place(u, addr, "COD", l2).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("GNE-INV-4090"));
        assertThat(data.reserved(removed.variantId())).isZero();
        assertThat(data.reserved(hidden.variantId())).isZero();
    }
}
