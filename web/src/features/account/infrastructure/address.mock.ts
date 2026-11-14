// Đơn vị hành chính (cây GHN rút gọn của prototype AddressModal.jsx REGIONS) và sổ địa chỉ mẫu
// (prototype data.jsx ADDRESSES). Mã số sinh theo thứ tự — chỉ dùng cho mock.
import type { AddressDto, RegionDto, WardDto } from './address.dto';

const TREE: Record<string, Record<string, string[]>> = {
  'TP. Hồ Chí Minh': {
    'Quận 1': [
      'P. Bến Nghé',
      'P. Bến Thành',
      'P. Đa Kao',
      'P. Cầu Ông Lãnh',
      'P. Nguyễn Thái Bình',
      'P. Phạm Ngũ Lão',
    ],
    'Quận 3': ['P. Võ Thị Sáu', 'P. 1', 'P. 2', 'P. 3', 'P. 4', 'P. 5'],
    'Quận 7': ['P. Tân Phú', 'P. Tân Phong', 'P. Phú Mỹ', 'P. Tân Thuận Đông', 'P. Bình Thuận'],
    'TP. Thủ Đức': [
      'P. Thảo Điền',
      'P. An Phú',
      'P. Linh Trung',
      'P. Hiệp Bình Chánh',
      'P. Trường Thọ',
    ],
    'Quận Bình Thạnh': ['P. 1', 'P. 13', 'P. 22', 'P. 25', 'P. 26', 'P. 27'],
    'Quận Phú Nhuận': ['P. 1', 'P. 2', 'P. 4', 'P. 7', 'P. 9'],
  },
  'Hà Nội': {
    'Quận Hoàn Kiếm': ['P. Hàng Bạc', 'P. Hàng Bồ', 'P. Tràng Tiền', 'P. Cửa Nam', 'P. Lý Thái Tổ'],
    'Quận Ba Đình': ['P. Điện Biên', 'P. Kim Mã', 'P. Ngọc Hà', 'P. Liễu Giai', 'P. Cống Vị'],
    'Quận Cầu Giấy': ['P. Dịch Vọng', 'P. Mai Dịch', 'P. Nghĩa Tân', 'P. Quan Hoa', 'P. Yên Hòa'],
    'Quận Đống Đa': ['P. Cát Linh', 'P. Láng Hạ', 'P. Ô Chợ Dừa', 'P. Văn Miếu'],
  },
  'Đà Nẵng': {
    'Quận Hải Châu': ['P. Hải Châu 1', 'P. Thanh Bình', 'P. Thuận Phước', 'P. Bình Hiên'],
    'Quận Sơn Trà': ['P. An Hải Bắc', 'P. Mân Thái', 'P. Thọ Quang', 'P. Nại Hiên Đông'],
    'Quận Ngũ Hành Sơn': ['P. Mỹ An', 'P. Khuê Mỹ', 'P. Hòa Hải'],
  },
  'Bình Dương': {
    'TP. Thủ Dầu Một': ['P. Phú Cường', 'P. Chánh Nghĩa', 'P. Phú Hòa', 'P. Hiệp Thành'],
    'TP. Dĩ An': ['P. Dĩ An', 'P. Tân Bình', 'P. Đông Hòa', 'P. An Bình'],
    'TP. Thuận An': ['P. Lái Thiêu', 'P. An Phú', 'P. Bình Hòa'],
  },
  'Đồng Nai': {
    'TP. Biên Hòa': ['P. Trung Dũng', 'P. Quang Vinh', 'P. Tân Phong', 'P. Long Bình'],
  },
  'Hải Phòng': { 'Quận Hồng Bàng': ['P. Hoàng Văn Thụ', 'P. Quang Trung', 'P. Phan Bội Châu'] },
  'Cần Thơ': { 'Quận Ninh Kiều': ['P. Tân An', 'P. An Hội', 'P. Cái Khế', 'P. Xuân Khánh'] },
};

export const PROVINCES: RegionDto[] = [];
export const DISTRICTS: (RegionDto & { province_id: number })[] = [];
export const WARDS: (WardDto & { district_id: number })[] = [];

let districtSeq = 1440;
Object.entries(TREE).forEach(([province, districts], pi) => {
  const province_id = 201 + pi;
  PROVINCES.push({ id: province_id, name: province });
  Object.entries(districts).forEach(([district, wards]) => {
    const district_id = ++districtSeq;
    DISTRICTS.push({ id: district_id, name: district, province_id });
    wards.forEach((w, wi) =>
      WARDS.push({
        code: `${district_id}${String(wi + 1).padStart(2, '0')}`,
        name: w,
        district_id,
      }),
    );
  });
});

/** Tỉnh / Quận / Phường theo tên → dạng DTO. */
export function regionOf(
  province: string,
  district: string,
  ward: string,
): { province: RegionDto; district: RegionDto; ward: WardDto } {
  const p = PROVINCES.find((x) => x.name === province);
  const d = DISTRICTS.find((x) => x.name === district && x.province_id === p?.id);
  const w = WARDS.find((x) => x.name === ward && x.district_id === d?.id);
  if (!p || !d || !w) throw new Error(`mock thiếu đơn vị ${province}/${district}/${ward}`);
  return {
    province: p,
    district: { id: d.id, name: d.name },
    ward: { code: w.code, name: w.name },
  };
}

export const seedAddresses = (): AddressDto[] => [
  {
    id: 1,
    recipient_name: 'Trần Thị Mai',
    phone_number: '0901234567',
    ...regionOf('TP. Hồ Chí Minh', 'Quận 1', 'P. Bến Nghé'),
    address_line: '25 Nguyễn Huệ, Căn hộ 12A',
    label: 'HOME',
    is_default: true,
  },
  {
    id: 2,
    recipient_name: 'Trần Thị Mai (Cơ quan)',
    phone_number: '0902888222',
    ...regionOf('TP. Hồ Chí Minh', 'Quận 1', 'P. Bến Nghé'),
    address_line: 'Tầng 9, Toà nhà Bitexco, 2 Hải Triều',
    label: 'OFFICE',
    is_default: false,
  },
];
