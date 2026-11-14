// Mở shop (API_SPEC 9.1–9.2). Mock: mã 123456, tên "TechZone Official" đã dùng (GNE-VND-4094).
// Tài khoản mẫu vốn là chủ shop — mock bỏ kiểm GNE-VND-4093 để trình diễn được luồng người mua mở shop.
import { api } from '@/shared/api/http';
import { mockDelay, mockFail, USE_MOCK } from '@/shared/api/mock';
import { maskEmail } from '@/shared/lib/mask';
import type { RegistrationChallenge, ShopRegistration } from '../domain/registration.types';

export async function registerShop(r: ShopRegistration): Promise<RegistrationChallenge> {
  if (USE_MOCK) {
    await mockDelay(400);
    if (r.shopName.trim().toLowerCase() === 'techzone official') mockFail('GNE-VND-4094');
    return { id: `reg_${Date.now()}`, maskedTarget: maskEmail(r.email) };
  }
  const d = await api.post<{ challenge_id: string; masked_target: string }>(
    '/me/shop-registrations',
    {
      shop_name: r.shopName,
      owner_name: r.ownerName,
      phone_number: r.phone,
      email: r.email,
      main_category_id: r.mainCategoryId,
      business_type: r.businessType,
      accept_terms: r.acceptTerms,
    },
  );
  return { id: d.challenge_id, maskedTarget: d.masked_target };
}

/** 9.2 — xác nhận OTP → tài khoản lên VENDOR, KYC chưa nộp. */
export async function confirmShopRegistration(
  challengeId: string,
  otp: string,
): Promise<{ shopId: number }> {
  if (USE_MOCK) {
    await mockDelay(400);
    if (otp !== '123456') mockFail('GNE-AUTH-4222', { details: { remaining_attempts: 4 } });
    return { shopId: 1 };
  }
  const d = await api.post<{ shop_id: number; kyc_status: string }>(
    `/me/shop-registrations/${challengeId}/confirm`,
    { otp },
  );
  return { shopId: d.shop_id };
}
