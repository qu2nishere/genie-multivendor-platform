// E2E mua hàng (plan G13.13): đăng nhập → giỏ nhiều shop → voucher 3 tầng (shop / sàn / freeship)
// + Điểm Genie → thanh toán → cổng VNPAY mô phỏng → trang kết quả. Chạy trên dữ liệu mock (D3).
import { expect, test, type Page } from '@playwright/test';

const vnd = (s: string | null) => Number((s ?? '').replace(/[^\d]/g, ''));

async function login(page: Page, next: string) {
  await page.goto(`/auth/login?next=${encodeURIComponent(next)}`, { waitUntil: 'networkidle' });
  await page.getByLabel('Email').fill('mai.tran@gmail.com');
  await page.getByLabel('Mật khẩu', { exact: true }).fill('Genie@123');
  await page.getByRole('button', { name: 'Đăng nhập', exact: true }).click();
  await expect(page).toHaveURL(new RegExp(next.replace('/', '\\/')));
  await page.waitForLoadState('networkidle');
}

async function pickVnpay(page: Page) {
  const radio = page.getByRole('radio', { name: /Cổng VNPAY/ });
  await radio.focus();
  await page.keyboard.press('Space');
  await expect(radio).toBeChecked();
}

test.describe.configure({ timeout: 90_000 });

test.beforeEach(async ({ context }) => {
  await context.addInitScript(() => {
    try {
      sessionStorage.setItem('genie.popup.seen', '1');
    } catch {
      // storage bị chặn
    }
  });
});

test('checkout VNPay nhiều shop: voucher 3 tầng + Điểm → thanh toán thành công', async ({
  page,
}) => {
  await login(page, '/cart');

  // Giỏ nhiều shop
  const shops = page.getByRole('checkbox', { name: /^Chọn tất cả sản phẩm của / });
  await expect.poll(() => shops.count(), { timeout: 20_000 }).toBeGreaterThanOrEqual(3);

  // Voucher 3 tầng: voucher shop ở từng nhóm, Voucher Genie (sàn) + Freeship ở thanh tổng
  await expect
    .poll(() => page.getByRole('button', { name: /^Thêm voucher của Shop \(−[\d.]+₫\)$/ }).count())
    .toBeGreaterThanOrEqual(3);
  await expect(
    page.getByRole('button', { name: /^Voucher Genie \(−[\d.]+₫\) \+ Freeship$/ }),
  ).toBeVisible();

  // Điểm Genie: bật công tắc → tổng thanh toán giảm
  const totalOf = async () =>
    vnd(await page.locator('.tr-cart-pay__total').filter({ visible: true }).first().textContent());
  const before = await totalOf();
  expect(before).toBe(3_485_200); // "bộ số vàng" của prototype (G10.18)
  const coins = page.getByRole('switch', { name: 'Dùng Điểm Genie để giảm giá' });
  await coins.focus(); // công tắc là input ẩn trong label → thao tác bằng bàn phím
  await page.keyboard.press('Space');
  await expect(coins).toBeChecked();
  await expect.poll(totalOf).toBeLessThan(before);

  // Mua hàng → trang thanh toán (mọi đơn con theo shop)
  await page
    .locator('button:visible', { hasText: /^Mua hàng \(\d+\)$/ })
    .first()
    .click();
  await expect(page).toHaveURL(/\/checkout$/);
  await page.waitForLoadState('networkidle');
  await expect(page.getByText(/Đơn 1\/\d/).first()).toBeVisible();

  // Chọn VNPAY → đặt hàng → cổng mô phỏng → thanh toán
  await pickVnpay(page);
  await page
    .locator('button:visible', { hasText: /^Đặt hàng( & thanh toán VNPAY)?$/ })
    .first()
    .click();
  const gateway = page.getByRole('dialog').filter({ hasText: 'Cổng thanh toán VNPAY (mô phỏng)' });
  await expect(gateway).toBeVisible();
  await gateway.getByRole('button', { name: /^Thanh toán/ }).click();

  // Trang kết quả: thành công, có đơn con + Theo dõi
  await expect(page).toHaveURL(/\/checkout\/result\?session=/);
  await expect(page.getByRole('heading', { name: 'Thanh toán thành công!' })).toBeVisible();
  await expect(page.getByText('Mã giao dịch')).toBeVisible();
  expect(await page.getByRole('link', { name: /Theo dõi/ }).count()).toBeGreaterThanOrEqual(3);
});

test('VNPAY trả về "khách hủy" → trang thất bại còn giữ chỗ, có Thử lại', async ({ page }) => {
  await login(page, '/cart');
  await page
    .locator('button:visible', { hasText: /^Mua hàng \(\d+\)$/ })
    .first()
    .click();
  await expect(page).toHaveURL(/\/checkout$/);
  await page.waitForLoadState('networkidle');
  await pickVnpay(page);
  await page
    .locator('button:visible', { hasText: /^Đặt hàng( & thanh toán VNPAY)?$/ })
    .first()
    .click();
  const gateway = page.getByRole('dialog').filter({ hasText: 'Cổng thanh toán VNPAY (mô phỏng)' });
  await gateway.getByRole('button', { name: 'Quay lại đơn hàng' }).click();
  await expect(page).toHaveURL(/\/checkout\/result\?session=/);
  await expect(page.getByRole('heading', { name: 'Thanh toán thất bại' })).toBeVisible();
  await expect(page.getByRole('button', { name: 'Thử thanh toán lại' })).toBeVisible();
  await expect(page.getByRole('button', { name: 'Đổi phương thức' })).toBeVisible();
});
