// Cài đặt Shop (API_SPEC 10.11): GET / PATCH /seller/settings, PUT /seller/pickup-address, tài khoản nhận tiền
// (xác minh chính chủ → PUT 🔐 / DELETE 🔐), chế độ nghỉ (PUT / DELETE /seller/vacation), GET /public/banks.
// Mock: dữ liệu prototype Settings; tên trên CCCD "Đặng Quốc Huy"; OTP 123456; còn đơn chưa bàn giao →
// không bật được chế độ nghỉ (lấy từ kho đơn dùng chung).
import { api } from '@/shared/api/http';
import { mockDelay, mockFail, mockState, USE_MOCK } from '@/shared/api/mock';
import { sellerMoneyStore, sellerOrdersStore, sellerStore } from '@/shared/api/mock/seller';
import { canRename, holderMatches, normalizeHolder } from '../domain/settings.rules';
import type {
  Bank,
  CarrierCode,
  PickupAddress,
  SettingsPatch,
  ShopSettings,
} from '../domain/settings.types';
import { KYC_NAME } from './shop.mock';

interface SettingsDto {
  profile: {
    shop_name: string;
    description: string;
    logo_url: string | null;
    cover_url: string | null;
    next_rename_allowed_at: string | null;
  };
  shipping: {
    carriers: Record<CarrierCode, boolean>;
    free_shipping_enabled: boolean;
    free_shipping_threshold: number | null;
  };
  pickup_address: {
    phone_number: string;
    province: { id: number; name: string } | null;
    district: { id: number; name: string } | null;
    ward: { code: string; name: string } | null;
    address_line: string;
    ghn_shop_id: number | null;
  };
  payout_account: {
    bank_name: string;
    account_holder: string;
    account_number_masked: string;
    is_verified: boolean;
  } | null;
  auto_reply: { enabled: boolean; greeting: string; away_message: string };
  pre_order_enabled: boolean;
  vacation: { ends_at: string; message: string } | null;
}

const toPickup = (d: SettingsDto['pickup_address']): PickupAddress => ({
  phone: d.phone_number,
  province: d.province,
  district: d.district,
  ward: d.ward,
  line: d.address_line,
  ghnShopId: d.ghn_shop_id,
});

const toSettings = (d: SettingsDto): ShopSettings => ({
  profile: {
    shopName: d.profile.shop_name,
    description: d.profile.description,
    logoUrl: d.profile.logo_url,
    coverUrl: d.profile.cover_url,
    nextRenameAllowedAt: d.profile.next_rename_allowed_at,
  },
  shipping: {
    carriers: d.shipping.carriers,
    freeShippingEnabled: d.shipping.free_shipping_enabled,
    freeShippingThreshold: d.shipping.free_shipping_threshold,
  },
  pickup: toPickup(d.pickup_address),
  payoutAccount: d.payout_account && {
    bank: d.payout_account.bank_name,
    holder: d.payout_account.account_holder,
    numberMasked: d.payout_account.account_number_masked,
    verified: d.payout_account.is_verified,
  },
  autoReply: {
    enabled: d.auto_reply.enabled,
    greeting: d.auto_reply.greeting,
    away: d.auto_reply.away_message,
  },
  preOrderEnabled: d.pre_order_enabled,
  vacation: d.vacation && { endsAt: d.vacation.ends_at, message: d.vacation.message },
});

// ---- Mock ----
const OTP = '123456';
const NOW = () => Date.parse('2026-06-08T18:00:00+07:00');
const db = mockState<SettingsDto>('seller.settings', () => ({
  profile: {
    shop_name: sellerStore.get().name,
    description: 'Thiết bị công nghệ chính hãng — bảo hành 12 tháng.',
    logo_url: null,
    cover_url: '/img/products/banner-tech.jpg',
    next_rename_allowed_at: null,
  },
  shipping: {
    carriers: {
      GHN: true,
      GHTK: true,
      VIETTEL_POST: false,
      JNT_EXPRESS: false,
      GENIE_EXPRESS: false,
    },
    free_shipping_enabled: true,
    free_shipping_threshold: 99000,
  },
  pickup_address: {
    phone_number: '0912345678',
    province: { id: 201, name: 'TP. Hồ Chí Minh' },
    district: { id: 1443, name: 'Quận 7' },
    ward: { code: '144302', name: 'P. Tân Phong' },
    address_line: 'Kho TechZone, 88 Nguyễn Văn Linh',
    ghn_shop_id: 884120,
  },
  payout_account: {
    bank_name: 'Vietcombank',
    account_holder: 'DANG QUOC HUY',
    account_number_masked: '•••• •••• 6712',
    is_verified: true,
  },
  auto_reply: {
    enabled: true,
    greeting: 'Chào bạn, shop có thể giúp gì cho bạn?',
    away_message: 'Hiện tại shop đang bận, sẽ trả lời bạn sớm nhất có thể.',
  },
  pre_order_enabled: false,
  vacation: null,
}));

const BANKS: Bank[] = [
  { code: 'VCB', name: 'Vietcombank' },
  { code: 'TCB', name: 'Techcombank' },
  { code: 'BIDV', name: 'BIDV' },
  { code: 'CTG', name: 'VietinBank' },
  { code: 'MB', name: 'MB Bank' },
  { code: 'ACB', name: 'ACB' },
];

const otpHeaders = (p: { challengeId: string; code: string }) => ({
  'X-OTP-Challenge-Id': p.challengeId,
  'X-OTP-Code': p.code,
});
const checkOtp = (code: string) => {
  if (code !== OTP) mockFail('GNE-AUTH-4222', { details: { remaining_attempts: 4 } });
};

export async function getSettings(): Promise<ShopSettings> {
  if (USE_MOCK) {
    await mockDelay(200);
    return toSettings(db.get());
  }
  return toSettings(await api.get<SettingsDto>('/seller/settings'));
}

export async function patchSettings(p: SettingsPatch): Promise<ShopSettings> {
  if (USE_MOCK) {
    await mockDelay(300);
    const s = db.get();
    const next: SettingsDto = { ...s };
    if (p.profile) {
      const renamed = p.profile.shopName.trim() !== s.profile.shop_name;
      if (renamed && !canRename(s.profile.next_rename_allowed_at, NOW()))
        mockFail('GNE-VND-4091', {
          details: { next_rename_allowed_at: s.profile.next_rename_allowed_at },
        });
      next.profile = {
        ...s.profile,
        shop_name: p.profile.shopName.trim(),
        description: p.profile.description,
        logo_url:
          p.profile.logoObjectKey === undefined
            ? s.profile.logo_url
            : p.profile.logoObjectKey
              ? '/img/products/backpack.jpg'
              : null,
        next_rename_allowed_at: renamed
          ? '2026-07-08T18:00:00+07:00'
          : s.profile.next_rename_allowed_at,
      };
      if (renamed) sellerStore.set({ ...sellerStore.get(), name: next.profile.shop_name });
    }
    if (p.shipping) {
      if (!Object.values(p.shipping.carriers).some(Boolean)) mockFail('GNE-VND-4221');
      next.shipping = {
        carriers: p.shipping.carriers,
        free_shipping_enabled: p.shipping.freeShippingEnabled,
        free_shipping_threshold: p.shipping.freeShippingThreshold,
      };
    }
    if (p.autoReply) {
      if (p.autoReply.enabled && !p.autoReply.greeting.trim() && !p.autoReply.away.trim())
        mockFail('GNE-VND-4222');
      next.auto_reply = {
        enabled: p.autoReply.enabled,
        greeting: p.autoReply.greeting,
        away_message: p.autoReply.away,
      };
    }
    if (p.preOrderEnabled !== undefined) next.pre_order_enabled = p.preOrderEnabled;
    db.set(next);
    return toSettings(next);
  }
  return toSettings(
    await api.patch<SettingsDto>('/seller/settings', {
      profile: p.profile && {
        shop_name: p.profile.shopName.trim(),
        description: p.profile.description,
        logo_object_key: p.profile.logoObjectKey,
        cover_object_key: p.profile.coverObjectKey,
      },
      shipping: p.shipping && {
        carriers: p.shipping.carriers,
        free_shipping_enabled: p.shipping.freeShippingEnabled,
        free_shipping_threshold: p.shipping.freeShippingThreshold,
      },
      auto_reply: p.autoReply && {
        enabled: p.autoReply.enabled,
        greeting: p.autoReply.greeting,
        away_message: p.autoReply.away,
      },
      pre_order_enabled: p.preOrderEnabled,
    }),
  );
}

export async function savePickupAddress(a: PickupAddress): Promise<PickupAddress> {
  if (USE_MOCK) {
    await mockDelay(500);
    const s = db.get();
    const next = {
      phone_number: a.phone.replace(/\s/g, ''),
      province: a.province,
      district: a.district,
      ward: a.ward,
      address_line: a.line.trim(),
      ghn_shop_id: 884000 + a.line.trim().length,
    };
    db.set({ ...s, pickup_address: next });
    return toPickup(next);
  }
  const d = await api.put<SettingsDto['pickup_address']>('/seller/pickup-address', {
    phone_number: a.phone.replace(/\s/g, ''),
    province_id: a.province?.id,
    district_id: a.district?.id,
    ward_code: a.ward?.code,
    address_line: a.line.trim(),
  });
  return toPickup(d);
}

export async function listBanks(): Promise<Bank[]> {
  if (USE_MOCK) {
    await mockDelay(80);
    return BANKS;
  }
  return api.get<Bank[]>('/public/banks', { auth: false });
}

export interface PayoutInput {
  bankCode: string;
  accountNumber: string;
  holder: string;
}

/** Bước 1: so tên chủ tài khoản với CCCD đã duyệt (422 GNE-WAL-4222 kèm `kyc_name`). */
export async function verifyPayoutAccount(v: PayoutInput): Promise<{ normalizedHolder: string }> {
  if (USE_MOCK) {
    await mockDelay(400);
    if (!holderMatches(v.holder, KYC_NAME))
      mockFail('GNE-WAL-4222', { details: { kyc_name: KYC_NAME } });
    return { normalizedHolder: normalizeHolder(v.holder) };
  }
  const d = await api.post<{ matched: boolean; normalized_holder: string }>(
    '/seller/payout-account/verification',
    { bank_code: v.bankCode, account_number: v.accountNumber, account_holder: v.holder },
  );
  return { normalizedHolder: d.normalized_holder };
}

/** Bước 2 🔐 PAYOUT_ACCOUNT_UPDATE. */
export async function savePayoutAccount(
  v: PayoutInput,
  proof: { challengeId: string; code: string },
): Promise<void> {
  if (USE_MOCK) {
    await mockDelay(350);
    checkOtp(proof.code);
    const bank = BANKS.find((b) => b.code === v.bankCode)?.name ?? v.bankCode;
    db.set({
      ...db.get(),
      payout_account: {
        bank_name: bank,
        account_holder: normalizeHolder(v.holder),
        account_number_masked: `•••• •••• ${v.accountNumber.slice(-4)}`,
        is_verified: true,
      },
    });
    return;
  }
  await api.put(
    '/seller/payout-account',
    { bank_code: v.bankCode, account_number: v.accountNumber, account_holder: v.holder },
    { headers: otpHeaders(proof) },
  );
}

/** 🔐 PAYOUT_ACCOUNT_DELETE — còn lệnh rút đang chờ → 409 GNE-WAL-4091. */
export async function deletePayoutAccount(proof: {
  challengeId: string;
  code: string;
}): Promise<void> {
  if (USE_MOCK) {
    await mockDelay(300);
    checkOtp(proof.code);
    if (sellerMoneyStore.get().pending_withdrawal > 0) mockFail('GNE-WAL-4091');
    db.set({ ...db.get(), payout_account: null });
    return;
  }
  await api.delete('/seller/payout-account', { headers: otpHeaders(proof) });
}

/** Đơn chưa bàn giao ĐVVC (chặn bật chế độ nghỉ). */
export function mockPendingOrders(): string[] {
  return sellerOrdersStore
    .get()
    .filter((o) => ['NEW', 'CONFIRMED', 'PREPARING'].includes(o.status))
    .map((o) => o.code);
}

export async function startVacation(endsAt: string, message: string): Promise<void> {
  if (USE_MOCK) {
    await mockDelay(300);
    const pending = mockPendingOrders();
    if (pending.length)
      mockFail('GNE-VND-4092', { details: { pending_order_codes: pending.join(', ') } });
    db.set({ ...db.get(), vacation: { ends_at: `${endsAt}T00:00:00+07:00`, message } });
    return;
  }
  await api.put('/seller/vacation', { ends_at: `${endsAt}T00:00:00+07:00`, message });
}

export async function endVacation(): Promise<void> {
  if (USE_MOCK) {
    await mockDelay(200);
    db.set({ ...db.get(), vacation: null });
    return;
  }
  await api.delete('/seller/vacation');
}
