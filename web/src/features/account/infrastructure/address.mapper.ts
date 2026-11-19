import type { Address, AddressInput, GeoAddress } from '../domain/address.types';
import type { AddressDto, AddressInputDto, GeocodeDto } from './address.dto';

export const toAddress = (d: AddressDto): Address => ({
  id: d.id,
  recipient: d.recipient_name,
  phone: d.phone_number,
  province: d.province,
  ward: d.ward,
  line: d.address_line,
  note: d.delivery_note ?? null,
  label: d.label,
  isDefault: d.is_default,
});

export const toAddressInputDto = (i: AddressInput): AddressInputDto => ({
  recipient_name: i.recipient,
  phone_number: i.phone,
  province_id: i.provinceId,
  ward_code: i.wardCode,
  address_line: i.line,
  delivery_note: i.note.trim() || null,
  label: i.label,
  is_default: i.isDefault,
});

export const toGeoAddress = (d: GeocodeDto): GeoAddress => ({
  province: d.province ?? null,
  ward: d.ward ?? null,
  line: d.address_line ?? '',
});
