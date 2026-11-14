// E2E người bán (plan G13.15): mở shop + KYC (become-seller → OTP → /seller/kyc → nộp hồ sơ),
// rồi với shop đã xác minh: tạo sản phẩm (lưu nháp) và xử lý đơn mới (xác nhận → chuẩn bị hàng).
import { expect, test } from '@playwright/test';

// PNG 1×1 hợp lệ, thêm byte đệm cho đủ ngưỡng ảnh KYC ≥ 20 KB (KYC_MIN_IMAGE)
const PNG = Buffer.concat([
  Buffer.from(
    'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg==',
    'base64',
  ),
  Buffer.alloc(24 * 1024),
]);
const png = (name: string) => ({ name, mimeType: 'image/png', buffer: PNG });

test.describe.configure({ timeout: 90_000 });

test.beforeEach(async ({ context, baseURL }) => {
  await context.addCookies([{ name: 'genie_logged_in', value: '1', url: baseURL! }]);
  await context.addInitScript(() => {
    try {
      sessionStorage.setItem('genie.popup.seen', '1');
    } catch {
      // storage bị chặn
    }
  });
});

test('mở shop → OTP → hồ sơ KYC → gửi duyệt, khung Seller chuyển "KYC đang chờ"', async ({
  page,
}) => {
  await page.goto('/become-seller', { waitUntil: 'networkidle' });
  const form = page.locator('form').filter({ hasText: 'Đăng ký mở Shop' });
  await form.getByLabel('Tên Shop').fill('Shop Thử Nghiệm E2E');
  await form.getByLabel('Họ tên chủ Shop').fill('Trần Thị Mai');
  await form.getByLabel(/Tôi đồng ý với/).check({ force: true });
  await form.getByRole('button', { name: 'Đăng ký mở Shop' }).click();

  // OTP gửi tới email (mock: 123456) — dán cả mã vào ô đầu
  const otp = page.getByRole('dialog', { name: 'Xác minh Email mở Shop' });
  await expect(otp).toBeVisible();
  await otp.locator('input').first().fill('123456');
  await expect(otp.getByRole('textbox', { name: 'Chữ số thứ 6' })).toHaveValue('6');
  await otp.getByRole('button', { name: 'Xác nhận' }).click();
  await expect(page).toHaveURL(/\/seller\/kyc$/, { timeout: 15_000 });
  await page.waitForLoadState('networkidle');
  await expect(page.getByText('Chưa nộp hồ sơ KYC')).toBeVisible();

  // Thông tin + giấy tờ (Cá nhân: CCCD 2 mặt + selfie)
  await page.getByLabel('Số CCCD').fill('079123456789');
  await page.getByLabel('Người đại diện').fill('Trần Thị Mai');
  await page.getByLabel('Tải lên CCCD mặt trước').setInputFiles(png('cccd-truoc.png'));
  await page.getByLabel('Tải lên CCCD mặt sau').setInputFiles(png('cccd-sau.png'));
  await page.getByLabel(/Tải lên Ảnh chân dung/).setInputFiles(png('selfie.png'));
  await page.getByRole('button', { name: 'Gửi duyệt KYC' }).click();

  await expect(page.getByText('Hồ sơ đang chờ duyệt').first()).toBeVisible({ timeout: 15_000 });
  // Chưa VERIFIED → các mục khác của Seller Center bị khóa (API 0.9)
  await expect(
    page.getByRole('navigation').getByRole('button', { name: 'Sản phẩm' }),
  ).toBeDisabled();
});

test('shop đã xác minh: tạo sản phẩm (lưu nháp) rồi xử lý đơn mới', async ({ page }) => {
  // Tạo sản phẩm — lưu nháp chỉ cần tên
  await page.goto('/seller/products/new', { waitUntil: 'networkidle' });
  await expect(page.getByText('Shop đã được xác minh, được phép đăng bán')).toBeVisible();
  await page.getByLabel('Tên sản phẩm').fill('Tai nghe thử nghiệm E2E chống ồn');
  await page.getByRole('button', { name: 'Lưu nháp' }).first().click();
  await expect(page.getByText('Đã lưu nháp sản phẩm')).toBeVisible();
  await page.goto('/seller/products?tab=DRAFT', { waitUntil: 'networkidle' });
  await expect(page.getByText('Tai nghe thử nghiệm E2E chống ồn').first()).toBeVisible();

  // Xử lý đơn: tab Mới → Xác nhận → Chuẩn bị hàng
  await page.goto('/seller/orders?tab=NEW', { waitUntil: 'networkidle' });
  await page.getByRole('button', { name: 'Xác nhận', exact: true }).first().click();
  await expect(page.getByText(/Đã xác nhận đơn/).first()).toBeVisible();
  await page.goto('/seller/orders?tab=TO_PREPARE', { waitUntil: 'networkidle' });
  await page.getByRole('button', { name: 'Chuẩn bị hàng' }).first().click();
  await expect(page.getByText('Đã chuyển sang Đang chuẩn bị').first()).toBeVisible();
});
