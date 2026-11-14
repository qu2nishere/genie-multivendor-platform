// Sổ địa chỉ (plan G10.45, G10.48–G10.49; API_SPEC 3.8–3.9 $Address; prototype ADDRESSES, AddressModal.jsx).

export interface Region {
  id: number;
  name: string;
}

/** Phường / xã dùng mã chuỗi chuẩn GHN. */
export interface Ward {
  code: string;
  name: string;
}

export type AddressLabel = 'HOME' | 'OFFICE';

export interface Address {
  id: number;
  recipient: string;
  phone: string;
  province: Region;
  district: Region;
  ward: Ward;
  line: string;
  label: AddressLabel;
  isDefault: boolean;
}

/** Dữ liệu form thêm / sửa ($AddressInput). */
export interface AddressInput {
  recipient: string;
  phone: string;
  provinceId: number;
  districtId: number;
  wardCode: string;
  line: string;
  label: AddressLabel;
  isDefault: boolean;
}

/** Kết quả "Dùng vị trí hiện tại" (3.9 reverse-geocode). */
export interface GeoAddress {
  province: Region;
  district: Region;
  ward: Ward;
  line: string;
}
