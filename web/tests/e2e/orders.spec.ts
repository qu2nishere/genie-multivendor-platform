// E2E đơn hàng (plan G13.14): theo dõi đơn → yêu cầu trả hàng (bằng chứng ảnh + video, tài khoản
// hoàn tiền) → đánh giá sản phẩm. Dữ liệu mock: đơn 44 đang giao, 41 đã giao (VietQR), 33 hoàn thành.
import { expect, test, type Page } from '@playwright/test';

// PNG 1×1 hợp lệ (bằng chứng ảnh) + vài byte giả làm MP4 (mock tải tệp không đọc nội dung video)
const PNG = Buffer.from(
  'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg==',
  'base64',
);
const MP4 = Buffer.from('000000186674797069736f6d0000020069736f6d69736f32', 'hex');

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

const open = async (page: Page, path: string) => {
  await page.goto(path, { waitUntil: 'networkidle' });
};

test('theo dõi đơn đang giao từ danh sách: mã đơn, sao chép mã vận đơn, chưa cho trả hàng', async ({
  page,
}) => {
  await open(page, '/orders');
  await expect(page.getByText('TR-20260608-000044').first()).toBeVisible();
  await page
    .getByRole('link', { name: /Theo dõi đơn/ })
    .first()
    .click();
  await expect(page).toHaveURL(/\/orders\/44$/);
  await page.waitForLoadState('networkidle');
  await expect(page.getByText('TR-20260608-000044').first()).toBeVisible();
  await expect(page.getByRole('button', { name: /Sao chép/ }).first()).toBeVisible();
  // Đơn chưa giao xong thì chưa có Trả hàng / Đánh giá
  await expect(page.getByRole('button', { name: 'Trả hàng' })).toHaveCount(0);
});

test('yêu cầu trả hàng đơn đã giao (VietQR) → hiện khối yêu cầu trả hàng', async ({ page }) => {
  await open(page, '/orders/41?return=1');
  const dialog = page.getByRole('dialog', { name: 'Yêu cầu Trả hàng / Hoàn tiền' });
  await expect(dialog).toBeVisible();

  // Bằng chứng: 1 ảnh + 1 video mở hộp
  await dialog.locator('#refund-evidence').setInputFiles([
    { name: 'nhan-van-don.png', mimeType: 'image/png', buffer: PNG },
    { name: 'mo-hop.mp4', mimeType: 'video/mp4', buffer: MP4 },
  ]);
  await expect(dialog.getByText(/Còn thiếu/)).toHaveCount(0);

  // Đơn VietQR hoàn bằng chuyển khoản → nhập tài khoản nhận tiền
  await dialog.getByLabel('Số tài khoản').fill('0123456789');
  await dialog.getByLabel('Tên chủ tài khoản').fill('TRAN THI MAI');

  await dialog.getByRole('button', { name: 'Gửi yêu cầu' }).click();
  await expect(
    page.getByText('Đã gửi yêu cầu trả hàng. Shop có 48 giờ để phản hồi.'),
  ).toBeVisible();
  await expect(dialog).toBeHidden();
  await expect(page.locator('#return')).toBeVisible();
});

test('đánh giá đơn hoàn thành: chọn sao bằng bàn phím, nhận xét, gửi', async ({ page }) => {
  await open(page, '/orders/33?review=1');
  const dialog = page.getByRole('dialog', { name: /Đánh giá sản phẩm/ });
  await expect(dialog).toBeVisible();

  const stars = dialog.getByRole('radiogroup', { name: 'Số sao' });
  // Mặc định 5 sao; focus vào sao đang chọn rồi ← → đổi sao, focus đi theo sao mới
  await stars.locator('[role="radio"][tabindex="0"]').focus();
  await page.keyboard.press('ArrowLeft');
  const four = stars.getByRole('radio', { name: '4 sao' });
  await expect(four).toHaveAttribute('aria-checked', 'true');
  await expect(four).toBeFocused();

  await dialog
    .getByPlaceholder('Hãy chia sẻ nhận xét của bạn về sản phẩm này nhé...')
    .fill('Giày êm, đúng size, giao nhanh. Shop đóng gói cẩn thận.');
  await dialog.getByRole('button', { name: /^Gửi/ }).last().click();
  await expect(page.getByText(/Cảm ơn bạn đã đánh giá!/).first()).toBeVisible();
});
