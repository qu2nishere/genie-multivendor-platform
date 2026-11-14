// E2E khách vãng lai (plan G13.12): trang chủ → tìm kiếm → sản phẩm → thêm giỏ → bị nhắc đăng nhập.
// Thao tác bằng BÀN PHÍM (focus + Enter / Space) để kiểm luồng mua dùng được không cần chuột (G13.07).
import { expect, test } from '@playwright/test';

test.describe('khách vãng lai', () => {
  test.beforeEach(async ({ context }) => {
    // không hiện popup banner lần đầu để không che trang
    await context.addInitScript(() => {
      try {
        sessionStorage.setItem('genie.popup.seen', '1');
      } catch {
        // bỏ qua khi storage bị chặn
      }
    });
  });

  test('tìm sản phẩm, thêm vào giỏ rồi được nhắc đăng nhập khi mua hàng', async ({ page }) => {
    await page.goto('/', { waitUntil: 'networkidle' });
    await expect(page.locator('#main h1')).toBeVisible();

    // Tìm kiếm từ ô tìm kiếm ở header
    const search = page
      .getByRole('combobox', { name: /tìm/i })
      .or(page.getByPlaceholder(/Tìm sản phẩm/))
      .first();
    await search.focus();
    await page.keyboard.type('tai nghe');
    await page.keyboard.press('Enter');
    await expect(page).toHaveURL(/\/search\?q=tai(\+|%20)nghe/);
    await expect(page.locator('#main h1')).toContainText('tai nghe');

    // Mở sản phẩm bằng bàn phím
    const card = page.locator('main a[href^="/p/"]').first();
    await card.focus();
    await page.keyboard.press('Enter');
    await expect(page).toHaveURL(/\/p\/tai-nghe-bluetooth/);
    await expect(page.locator('#main h1')).toContainText('Tai nghe Bluetooth');
    await page.waitForLoadState('networkidle'); // BuyBox (client:load) đã hydrate

    // Chọn phân loại + thêm vào giỏ bằng bàn phím
    for (const name of ['Đen', 'Tiêu chuẩn']) {
      const opt = page.getByRole('button', { name, exact: true });
      await opt.focus();
      await page.keyboard.press('Space');
      await expect(opt).toHaveAttribute('aria-pressed', 'true');
    }
    const add = page.getByRole('button', { name: 'Thêm vào giỏ' }).first();
    await add.focus();
    await page.keyboard.press('Enter');

    // Giỏ khách: số dòng trên icon giỏ ở header tăng
    await expect(page.getByRole('link', { name: /Giỏ hàng/ }).first()).toContainText('1');

    // Sang giỏ → "Mua hàng" → hộp nhắc đăng nhập
    await page.goto('/cart', { waitUntil: 'networkidle' });
    await expect(page.getByText('Tai nghe Bluetooth chống ồn ANC Pro 5').first()).toBeVisible();
    // Giỏ nạp xong thì mọi dòng mua được đã được chọn sẵn; bỏ chọn rồi chọn lại bằng bàn phím
    const buy = page.locator('button:visible', { hasText: 'Mua hàng (1)' }).first();
    await expect(buy).toBeEnabled();
    const all = page.getByRole('checkbox', { name: /Chọn tất cả \(/ });
    await all.focus();
    await page.keyboard.press('Space');
    await expect(all).not.toBeChecked();
    await page.keyboard.press('Space');
    await expect(all).toBeChecked();
    await buy.focus();
    await page.keyboard.press('Enter');
    const dialog = page.getByRole('dialog');
    await expect(dialog).toContainText('Vui lòng đăng nhập');
    await expect(dialog.getByRole('link', { name: /Đăng nhập/ }).first()).toHaveAttribute(
      'href',
      /\/auth\/login\?next=%2Fcart/,
    );
    // Esc đóng hộp, focus không mất khỏi trang
    await page.keyboard.press('Escape');
    await expect(dialog).toBeHidden();
  });

  test('trang cần đăng nhập chuyển về /auth/login kèm next', async ({ page }) => {
    await page.goto('/orders');
    await expect(page).toHaveURL(/\/auth\/login\?next=%2Forders/);
  });
});
