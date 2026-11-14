// E2E quản trị (plan G13.16): nhân viên chỉ thấy mục đúng quyền; Admin duyệt KYC và duyệt rút tiền.
// Mock: sessionStorage `genie.mock.staff_title` đăng nhập /admin bằng nhân viên theo chức danh
// (shared/auth/session.ts) — không có nút đổi vai trên giao diện (D5).
import { expect, test, type BrowserContext } from '@playwright/test';

test.describe.configure({ timeout: 90_000 });

async function asStaff(context: BrowserContext, baseURL: string, title?: string) {
  await context.addCookies([{ name: 'genie_logged_in', value: '1', url: baseURL }]);
  await context.addInitScript((t) => {
    try {
      if (t) sessionStorage.setItem('genie.mock.staff_title', t);
    } catch {
      // storage bị chặn
    }
  }, title);
}

const NAV = 'Điều hướng Admin Console';

test('Kiểm duyệt viên (MODERATOR) chỉ thấy 4 mục được cấp; mở màn khác → 403', async ({
  page,
  context,
  baseURL,
}) => {
  await asStaff(context, baseURL!, 'MODERATOR');
  await page.goto('/admin', { waitUntil: 'networkidle' });
  const nav = page.getByRole('navigation', { name: NAV });
  for (const name of ['Tổng quan', 'Duyệt KYC', 'Kiểm duyệt đánh giá', 'Danh mục toàn sàn'])
    await expect(nav.getByRole('link', { name: new RegExp(`^${name}`) })).toBeVisible();
  for (const name of ['Xử lý rút tiền', 'Sổ cái tài chính', 'Phân quyền nhân viên', 'Voucher sàn'])
    await expect(nav.getByRole('link', { name: new RegExp(`^${name}`) })).toHaveCount(0);

  await page.goto('/admin/withdrawals', { waitUntil: 'networkidle' });
  await expect(page.getByRole('heading', { name: 'Bạn không có quyền truy cập' })).toBeVisible();
});

test('Chuyên viên tài chính (FINANCE_OFFICER) thấy mục tài chính, không thấy Duyệt KYC', async ({
  page,
  context,
  baseURL,
}) => {
  await asStaff(context, baseURL!, 'FINANCE_OFFICER');
  await page.goto('/admin', { waitUntil: 'networkidle' });
  const nav = page.getByRole('navigation', { name: NAV });
  await expect(nav.getByRole('link', { name: /^Xử lý rút tiền/ })).toBeVisible();
  await expect(nav.getByRole('link', { name: /^Duyệt KYC/ })).toHaveCount(0);
});

test('Admin duyệt hồ sơ KYC đang chờ', async ({ page, context, baseURL }) => {
  await asStaff(context, baseURL!);
  await page.goto('/admin/kyc', { waitUntil: 'networkidle' });
  // Hồ sơ đầu hàng chờ mở sẵn; phê duyệt gọi API ngay (không có hộp xác nhận)
  await page.getByRole('button', { name: 'Phê duyệt KYC' }).click();
  await expect(page.getByText(/^Đã phê duyệt KYC cho /).first()).toBeVisible();
});

test('Admin duyệt chi lệnh rút tiền → chuyển khoản → hoàn tất', async ({
  page,
  context,
  baseURL,
}) => {
  await asStaff(context, baseURL!);
  await page.goto('/admin/withdrawals', { waitUntil: 'networkidle' });
  await page.getByRole('button', { name: 'Duyệt chi' }).first().click();
  await page.getByRole('button', { name: 'Duyệt và mở chuyển khoản' }).click();
  const transfer = page.getByRole('dialog').filter({ hasText: 'Chuyển khoản lệnh' });
  await expect(transfer).toBeVisible();
  await expect(transfer.getByText(/TRWD/).first()).toBeVisible();
  await transfer.getByRole('button', { name: 'Đã chuyển khoản' }).click();
  await expect(page.getByText(/^Đã hoàn tất chi lệnh /).first()).toBeVisible();
});
