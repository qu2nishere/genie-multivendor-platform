// Sổ địa chỉ + đơn vị hành chính (API_SPEC 3.8–3.9). Mock giữ trong phiên tab (mockState).
import { api } from '@/shared/api/http';
import { mockDelay, mockFail, mockState, USE_MOCK } from '@/shared/api/mock';
import { sortAddresses } from '../domain/address.rules';
import type { Address, AddressInput, GeoAddress, Region, Ward } from '../domain/address.types';
import type { AddressDto, GeocodeDto, RegionDto, WardDto } from './address.dto';
import { toAddress, toAddressInputDto, toGeoAddress } from './address.mapper';
import { DISTRICTS, PROVINCES, regionOf, seedAddresses, WARDS } from './address.mock';

const db = mockState<AddressDto[]>('addresses', seedAddresses);

/** Mock thêm (id null) / sửa; đặt mặc định thì bỏ cờ ở địa chỉ khác; địa chỉ đầu tiên tự mặc định. */
function mockSave(input: AddressInput, id: number | null): AddressDto {
  const p = PROVINCES.find((x) => x.id === input.provinceId);
  const d = DISTRICTS.find((x) => x.id === input.districtId);
  const w = WARDS.find((x) => x.code === input.wardCode);
  if (!p || !d || !w) mockFail('GNE-VAL-4220');
  const list = db.get();
  const old = id != null ? list.find((a) => a.id === id) : undefined;
  if (id != null && !old) mockFail('GNE-VAL-4220');
  const isDefault = input.isDefault || list.length === 0 || !!old?.is_default;
  const body = toAddressInputDto(input);
  const dto: AddressDto = {
    id: id ?? Math.max(0, ...list.map((a) => a.id)) + 1,
    recipient_name: body.recipient_name,
    phone_number: body.phone_number,
    province: p,
    district: { id: d.id, name: d.name },
    ward: { code: w.code, name: w.name },
    address_line: body.address_line,
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

export async function listProvinces(): Promise<Region[]> {
  if (USE_MOCK) return PROVINCES;
  return api.get<RegionDto[]>('/public/regions/provinces', { auth: false });
}

export async function listDistricts(provinceId: number): Promise<Region[]> {
  if (USE_MOCK)
    return DISTRICTS.filter((d) => d.province_id === provinceId).map(({ id, name }) => ({
      id,
      name,
    }));
  return api.get<RegionDto[]>(`/public/regions/provinces/${provinceId}/districts`, {
    auth: false,
  });
}

export async function listWards(districtId: number): Promise<Ward[]> {
  if (USE_MOCK)
    return WARDS.filter((w) => w.district_id === districtId).map(({ code, name }) => ({
      code,
      name,
    }));
  return api.get<WardDto[]>(`/public/regions/districts/${districtId}/wards`, { auth: false });
}

/** "Dùng vị trí hiện tại": tọa độ → Tỉnh / Quận / Phường + số nhà. */
export async function reverseGeocode(lat: number, lng: number): Promise<GeoAddress> {
  if (USE_MOCK) {
    await mockDelay(600);
    return toGeoAddress({
      ...regionOf('TP. Hồ Chí Minh', 'Quận 1', 'P. Bến Nghé'),
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
