// $Address, $AddressInput, $Region (API_SPEC 3.8, 3.9) — snake_case; địa giới 2 cấp (plan AUTH D25).
import type { AddressLabel } from '../domain/address.types';
export interface RegionDto {
  id: number;
  name: string;
}

export interface WardDto {
  code: string;
  name: string;
}

export interface AddressDto {
  id: number;
  recipient_name: string;
  phone_number: string;
  province: RegionDto;
  ward: WardDto;
  address_line: string;
  delivery_note?: string | null;
  label: AddressLabel;
  is_default: boolean;
}

export interface AddressInputDto {
  recipient_name: string;
  phone_number: string;
  province_id: number;
  ward_code: string;
  address_line: string;
  delivery_note: string | null;
  label: AddressLabel;
  is_default: boolean;
}

export interface GeocodeDto {
  province?: RegionDto | null;
  ward?: WardDto | null;
  address_line?: string | null;
}
