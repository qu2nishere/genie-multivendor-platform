// $Address, $AddressInput, $Region (API_SPEC 3.8, 3.9) — snake_case.
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
  district: RegionDto;
  ward: WardDto;
  address_line: string;
  label: 'HOME' | 'OFFICE';
  is_default: boolean;
}

export interface AddressInputDto {
  recipient_name: string;
  phone_number: string;
  province_id: number;
  district_id: number;
  ward_code: string;
  address_line: string;
  label: 'HOME' | 'OFFICE';
  is_default: boolean;
}

export interface GeocodeDto {
  province: RegionDto;
  district: RegionDto;
  ward: WardDto;
  address_line: string;
}
