import type { Address, AddressInput, GeoAddress } from '../domain/address.types';
import type { AddressDto, AddressInputDto, GeocodeDto } from './address.dto';

export const toAddress = (d: AddressDto): Address => ({
  id: d.id,
  recipient: d.recipient_name,
  phone: d.phone_number,
  province: d.province,
  district: d.district,
  ward: d.ward,
  line: d.address_line,
  label: d.label,
  isDefault: d.is_default,
});

export const toAddressInputDto = (i: AddressInput): AddressInputDto => ({
  recipient_name: i.recipient,
  phone_number: i.phone,
  province_id: i.provinceId,
  district_id: i.districtId,
  ward_code: i.wardCode,
  address_line: i.line,
  label: i.label,
  is_default: i.isDefault,
});

export const toGeoAddress = (d: GeocodeDto): GeoAddress => ({
  province: d.province,
  district: d.district,
  ward: d.ward,
  line: d.address_line,
});
