// Sổ địa chỉ + đơn vị hành chính (API_SPEC 3.8–3.9). Mock giữ trong phiên tab (mockState).
// Nối API thật khi `PUBLIC_REAL_API` có `address` (plan AUTH A12), gồm cả "Dùng vị trí hiện tại" (A12.06 —
// backend đổi tọa độ thành địa chỉ qua OpenStreetMap rồi khớp danh mục). Địa giới 2 cấp: Tỉnh → Phường (D25).
import { api } from '@/shared/api/http';
import { mockDelay, mockFail, mockState } from '@/shared/api/mock';
import { isMocked } from '@/shared/config/env';
import { isSameAddress, sortAddresses } from '../domain/address.rules';
import type { Address, AddressInput, GeoAddress, Region, Ward } from '../domain/address.types';
import type { AddressDto, GeocodeDto, RegionDto, WardDto } from './address.dto';
import { toAddress, toAddressInputDto, toGeoAddress } from './address.mapper';
import { PROVINCES, regionOf, seedAddresses, WARDS } from './address.mock';

const USE_MOCK = isMocked('address');

const db = mockState<AddressDto[]>('addresses', seedAddresses);

/** Mock thêm (id null) / sửa; đặt mặc định thì bỏ cờ ở địa chỉ khác; địa chỉ đầu tiên tự mặc định. */
function mockSave(input: AddressInput, id: number | null): AddressDto {
  const p = PROVINCES.find((x) => x.id === input.provinceId);
  const w = WARDS.find((x) => x.code === input.wardCode && x.province_id === input.provinceId);
  if (!p || !w) mockFail('GNE-VAL-4220');
  const list = db.get();
  const old = id != null ? list.find((a) => a.id === id) : undefined;
  if (id != null && !old) mockFail('GNE-VAL-4220');
  // chặn trùng như server (A12.07): cùng tỉnh + phường + SĐT + địa chỉ cụ thể, trừ chính địa chỉ đang sửa
  const dup = list.find(
    (a) =>
      a.id !== id &&
      isSameAddress(
        {
          provinceId: a.province.id,
          wardCode: a.ward.code,
          phone: a.phone_number,
          line: a.address_line,
        },
        input,
      ),
  );
  if (dup) mockFail('GNE-USR-4093', { details: { existing_address_id: dup.id } });
  const isDefault = input.isDefault || list.length === 0 || !!old?.is_default;
  const body = toAddressInputDto(input);
  const dto: AddressDto = {
    id: id ?? Math.max(0, ...list.map((a) => a.id)) + 1,
    recipient_name: body.recipient_name,
    phone_number: body.phone_number,
    province: p,
    ward: { code: w.code, name: w.name },
    address_line: body.address_line,
    delivery_note: body.delivery_note,
    label: body.label,
    is_default: isDefault,
  };
  const others = list
    .filter((a) => a.id !== dto.id)
    .map((a) => (isDefault ? { ...a, is_default: false } : a));
  db.set(
    id != null
      ? list.map((a) => (a.id === id ? dto : (others.find((o) => o.id === a.id) ?? a)))
      : [...others, dto],
  );
  return dto;
}

export async function listAddresses(): Promise<Address[]> {
  if (USE_MOCK) {
    await mockDelay(120);
    return sortAddresses(db.get().map(toAddress));
  }
  return (await api.get<AddressDto[]>('/me/addresses')).map(toAddress);
}

export async function createAddress(input: AddressInput): Promise<Address> {
  if (USE_MOCK) {
    await mockDelay(200);
    return toAddress(mockSave(input, null));
  }
  return toAddress(await api.post<AddressDto>('/me/addresses', toAddressInputDto(input)));
}

export async function updateAddress(id: number, input: AddressInput): Promise<Address> {
  if (USE_MOCK) {
    await mockDelay(200);
    return toAddress(mockSave(input, id));
  }
  return toAddress(await api.put<AddressDto>(`/me/addresses/${id}`, toAddressInputDto(input)));
}

export async function setDefaultAddress(id: number): Promise<void> {
  if (USE_MOCK) {
    await mockDelay(150);
    if (!db.get().some((a) => a.id === id)) mockFail('GNE-VAL-4220');
    db.set(db.get().map((a) => ({ ...a, is_default: a.id === id })));
    return;
  }
  await api.patch<AddressDto>(`/me/addresses/${id}`, { is_default: true });
}

/** Xóa (mềm); địa chỉ mặc định → 409 GNE-USR-4092. */
export async function deleteAddress(id: number): Promise<void> {
  if (USE_MOCK) {
    await mockDelay(150);
    if (db.get().find((a) => a.id === id)?.is_default) mockFail('GNE-USR-4092');
    db.set(db.get().filter((a) => a.id !== id));
    return;
  }
  await api.delete(`/me/addresses/${id}`);
}

/**
 * Phiên bản danh mục hành chính. Backend cho trình duyệt giữ danh mục 24 giờ (`Cache-Control: max-age=86400`),
 * nên khi danh mục đổi (vd. địa giới 2 cấp — plan AUTH D25) phải đổi giá trị này để URL mới, trình duyệt không
 * dùng lại bản cũ trong cache. Backend bỏ qua tham số `v`.
 */
const REGIONS_VERSION = '2025-07-2cap';

export async function listProvinces(): Promise<Region[]> {
  if (USE_MOCK) return PROVINCES;
  return api.get<RegionDto[]>('/public/regions/provinces', {
    auth: false,
    query: { v: REGIONS_VERSION },
  });
}

/** Phường / xã / đặc khu của tỉnh. */
export async function listWards(provinceId: number): Promise<Ward[]> {
  if (USE_MOCK)
    return WARDS.filter((w) => w.province_id === provinceId).map(({ code, name }) => ({
      code,
      name,
    }));
  return api.get<WardDto[]>(`/public/regions/provinces/${provinceId}/wards`, {
    auth: false,
    query: { v: REGIONS_VERSION },
  });
}

/** "Dùng vị trí hiện tại": tọa độ → Tỉnh / Phường + số nhà. */
export async function reverseGeocode(lat: number, lng: number): Promise<GeoAddress> {
  if (USE_MOCK) {
    await mockDelay(600);
    return toGeoAddress({
      ...regionOf('Thành phố Hồ Chí Minh', 'Phường Sài Gòn'),
      address_line: '72 Lê Thánh Tôn',
    });
  }
  return toGeoAddress(
    await api.get<GeocodeDto>('/public/regions/reverse-geocode', {
      auth: false,
      query: { lat, lng },
    }),
  );
}
