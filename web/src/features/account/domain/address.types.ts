// Sổ địa chỉ (plan G10.45, G10.48–G10.49; API_SPEC 3.8–3.9 $Address; prototype ADDRESSES, AddressModal.jsx).
// Địa giới 2 cấp từ 01/07/2025 (plan AUTH D25): Tỉnh / thành → Phường / xã / đặc khu, không còn quận / huyện.

export interface Region {
  id: number;
  name: string;
}

/** Phường / xã / đặc khu — mã chuỗi. */
export interface Ward {
  code: string;
  name: string;
}

/** Loại địa chỉ (API_SPEC 1 `$Address.label`; plan AUTH D24 thêm Trường học, Chung cư, Khác). */
export type AddressLabel = 'HOME' | 'OFFICE' | 'SCHOOL' | 'APARTMENT' | 'OTHER';
export const ADDRESS_LABELS: readonly AddressLabel[] = [
  'HOME',
  'OFFICE',
  'SCHOOL',
  'APARTMENT',
  'OTHER',
];

export interface Address {
  id: number;
  recipient: string;
  phone: string;
  province: Region;
  ward: Ward;
  line: string;
  /** Ghi chú cho người giao hàng (plan AUTH D27); không có → null. */
  note: string | null;
  label: AddressLabel;
  isDefault: boolean;
}

/** Dữ liệu form thêm / sửa ($AddressInput). */
export interface AddressInput {
  recipient: string;
  phone: string;
  provinceId: number;
  wardCode: string;
  line: string;
  note: string;
  label: AddressLabel;
  isDefault: boolean;
}

/** Kết quả "Dùng vị trí hiện tại" (3.9 reverse-geocode): cấp nào không khớp được danh mục thì `null` (người dùng tự chọn). */
export interface GeoAddress {
  province: Region | null;
  ward: Ward | null;
  line: string;
}
