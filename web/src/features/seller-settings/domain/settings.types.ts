// Cài đặt Shop (API_SPEC 10.11; prototype VendorPages.jsx Settings).
export type CarrierCode = 'GHN' | 'GHTK' | 'VIETTEL_POST' | 'JNT_EXPRESS' | 'GENIE_EXPRESS';
export const CARRIERS: CarrierCode[] = [
  'GHN',
  'GHTK',
  'VIETTEL_POST',
  'JNT_EXPRESS',
  'GENIE_EXPRESS',
];

export interface PickupAddress {
  phone: string;
  province: { id: number; name: string } | null;
  district: { id: number; name: string } | null;
  ward: { code: string; name: string } | null;
  line: string;
  ghnShopId: number | null;
}

export interface ShopSettings {
  profile: {
    shopName: string;
    description: string;
    logoUrl: string | null;
    coverUrl: string | null;
    nextRenameAllowedAt: string | null;
  };
  shipping: {
    carriers: Record<CarrierCode, boolean>;
    freeShippingEnabled: boolean;
    freeShippingThreshold: number | null;
  };
  pickup: PickupAddress;
  payoutAccount: { bank: string; holder: string; numberMasked: string; verified: boolean } | null;
  autoReply: { enabled: boolean; greeting: string; away: string };
  preOrderEnabled: boolean;
  vacation: { endsAt: string; message: string } | null;
}

/** PATCH /seller/settings — gửi khối nào cập nhật khối đó. */
export interface SettingsPatch {
  profile?: {
    shopName: string;
    description: string;
    logoObjectKey?: string | null;
    coverObjectKey?: string | null;
  };
  shipping?: ShopSettings['shipping'];
  autoReply?: ShopSettings['autoReply'];
  preOrderEnabled?: boolean;
}

export interface Bank {
  code: string;
  name: string;
}
