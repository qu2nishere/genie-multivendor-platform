// E2E mở shop + KYC với backend THẬT (plan 2026-11-16 Mở shop & KYC K11.09; TS 14 — 1 trong 9 kịch bản bắt buộc).
// Chỉ chạy khi E2E_REAL_API=1: backend profile local (AI_WORKER_MODE=stub), Mailpit http://localhost:8025, web chạy với
// PUBLIC_REAL_API=auth,upload,profile,security,vendor. Mã OTP đọc qua API Mailpit. Spec mock kyc-onboarding.spec.ts giữ nguyên.
import { expect, test, type APIRequestContext } from '@playwright/test';

const REAL = process.env.E2E_REAL_API === '1';
const API = process.env.E2E_API_URL ?? 'http://localhost:8080/api/v1';
const MAILPIT = process.env.E2E_MAILPIT_URL ?? 'http://localhost:8025';

test.skip(!REAL, 'Chỉ chạy khi E2E_REAL_API=1 (cần backend + Mailpit)');
test.describe.configure({ timeout: 120_000 });

// Ảnh JPEG giả ≥ 20 KB (ngưỡng ảnh KYC — API 4.1)
const JPEG = Buffer.concat([
  Buffer.from([0xff, 0xd8, 0xff, 0xe0]),
  Buffer.alloc(30 * 1024, 7),
  Buffer.from([0xff, 0xd9]),
]);
const jpeg = (name: string) => ({ name, mimeType: 'image/jpeg', buffer: JPEG });

/** Mã OTP mới nhất gửi tới `to` (Mailpit API). */
async function otpFor(request: APIRequestContext, to: string): Promise<string> {
  for (let i = 0; i < 30; i++) {
    const res = await request.get(`${MAILPIT}/api/v1/search`, {
      params: { query: `to:"${to}"`, limit: '1' },
    });
    const list = (await res.json()) as { messages?: { ID: string }[] };
    const id = list.messages?.[0]?.ID;
    if (id) {
      const msg = (await (await request.get(`${MAILPIT}/api/v1/message/${id}`)).json()) as {
        Text: string;
      };
      const code = /\b(\d{6})\b/.exec(msg.Text)?.[1];
      if (code) return code;
    }
    await new Promise((r) => setTimeout(r, 500));
  }
  throw new Error(`Không thấy OTP gửi tới ${to}`);
}

test('tài khoản mới → mở shop (OTP) → nộp KYC → giả lập AI tự duyệt', async ({ page, request }) => {
  const tag = Date.now().toString(36);
  const email = `e2e-${tag}@genie.test`;
  const password = 'Genie@123';

  // 1. Đăng ký tài khoản người mua qua API + OTP Mailpit
  const reg = await request.post(`${API}/auth/registrations`, {
    data: {
      full_name: 'Trần Thị Mai',
      email,
      phone_number: '0912345678',
      password,
      accept_terms: true,
    },
  });
  expect(reg.status()).toBe(202);
  const challenge = ((await reg.json()) as { data: { challenge_id: string } }).data.challenge_id;
  const verified = await request.post(`${API}/auth/otp-challenges/${challenge}/verify`, {
    data: { otp: await otpFor(request, email) },
  });
  expect(verified.status()).toBe(201);

  // 2. Đăng nhập trên giao diện
  await page.goto('/auth/login', { waitUntil: 'networkidle' });
  await page.getByLabel('Email').fill(email);
  await page.getByLabel('Mật khẩu', { exact: true }).fill(password);
  await page.getByRole('button', { name: 'Đăng nhập', exact: true }).click();
  await page.waitForURL((u) => !u.pathname.startsWith('/auth'));

  // 3. Mở shop → OTP gửi tới email liên hệ của shop
  const shopEmail = `shop-${tag}@genie.test`;
  await page.goto('/become-seller', { waitUntil: 'networkidle' });
  const form = page.locator('form').filter({ hasText: 'Đăng ký mở Shop' });
  await form.getByLabel('Tên Shop').fill(`Shop E2E ${tag}`);
  await form.getByLabel('Họ tên chủ Shop').fill('Trần Thị Mai');
  await form.getByLabel('Email').fill(shopEmail);
  await form.getByLabel(/Tôi đồng ý với/).check({ force: true });
  await form.getByRole('button', { name: 'Đăng ký mở Shop' }).click();
  const otp = page.getByRole('dialog', { name: 'Xác minh Email mở Shop' });
  await expect(otp).toBeVisible();
  await otp
    .locator('input')
    .first()
    .fill(await otpFor(request, shopEmail));
  await otp.getByRole('button', { name: 'Xác nhận' }).click();
  await expect(page).toHaveURL(/\/seller\/kyc$/, { timeout: 20_000 });
  await expect(page.getByText('Chưa nộp hồ sơ KYC')).toBeVisible();

  // 4. Nộp KYC — CCCD đuôi 10 → bản giả lập AI tự duyệt sau ~2 giây
  await page.getByLabel('Số CCCD').fill('079096001210');
  await page.getByLabel('Người đại diện').fill('Trần Thị Mai');
  await page.getByLabel('Tải lên CCCD mặt trước').setInputFiles(jpeg('cccd-truoc.jpg'));
  await page.getByLabel('Tải lên CCCD mặt sau').setInputFiles(jpeg('cccd-sau.jpg'));
  await page.getByLabel(/Tải lên Ảnh chân dung/).setInputFiles(jpeg('selfie.jpg'));
  await page.getByRole('button', { name: 'Gửi duyệt KYC' }).click();
  await expect(page.getByText('Hồ sơ đang chờ duyệt').first()).toBeVisible({ timeout: 15_000 });

  // 5. Tải lại sau khi giả lập AI chạy: hồ sơ đã duyệt, Seller Center mở khóa
  await expect(async () => {
    await page.reload({ waitUntil: 'networkidle' });
    await expect(page.getByText(/đã được duyệt|Đã duyệt/).first()).toBeVisible({ timeout: 2_000 });
  }).toPass({ timeout: 30_000 });
});
