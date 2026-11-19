// Đơn vị hành chính 2 cấp (địa giới từ 01/07/2025 — plan AUTH D25) rút gọn cho mock, mã ĐVHC thật theo
// Quyết định 19/2025/QĐ-TTg (cùng mã với server-spring regions.json), và sổ địa chỉ mẫu (prototype data.jsx ADDRESSES).
import type { AddressDto, RegionDto, WardDto } from './address.dto';

const TREE: { id: number; name: string; wards: [string, string][] }[] = [
  {
    id: 79,
    name: 'Thành phố Hồ Chí Minh',
    wards: [
      ['26740', 'Phường Sài Gòn'],
      ['26743', 'Phường Bến Thành'],
      ['26737', 'Phường Tân Định'],
      ['27154', 'Phường Bàn Cờ'],
      ['27139', 'Phường Xuân Hoà'],
      ['27478', 'Phường Tân Thuận'],
      ['26824', 'Phường Thủ Đức'],
      ['27094', 'Phường An Khánh'],
      ['26929', 'Phường Bình Thạnh'],
      ['25747', 'Phường Thủ Dầu Một'],
      ['25942', 'Phường Dĩ An'],
    ],
  },
  {
    id: 1,
    name: 'Thành phố Hà Nội',
    wards: [
      ['00004', 'Phường Ba Đình'],
      ['00070', 'Phường Hoàn Kiếm'],
      ['00082', 'Phường Cửa Nam'],
      ['00166', 'Phường Cầu Giấy'],
      ['00235', 'Phường Đống Đa'],
      ['00103', 'Phường Tây Hồ'],
    ],
  },
  {
    id: 48,
    name: 'Thành phố Đà Nẵng',
    wards: [
      ['20242', 'Phường Hải Châu'],
      ['20209', 'Phường Thanh Khê'],
      ['20263', 'Phường Sơn Trà'],
      ['20285', 'Phường Ngũ Hành Sơn'],
      ['20410', 'Phường Hội An'],
    ],
  },
  {
    id: 75,
    name: 'Thành phố Đồng Nai',
    wards: [
      ['26041', 'Phường Trấn Biên'],
      ['26068', 'Phường Biên Hoà'],
      ['26017', 'Phường Tam Hiệp'],
      ['26020', 'Phường Long Bình'],
      ['25210', 'Phường Đồng Xoài'],
    ],
  },
  {
    id: 31,
    name: 'Thành phố Hải Phòng',
    wards: [
      ['11311', 'Phường Hồng Bàng'],
      ['11329', 'Phường Ngô Quyền'],
      ['11383', 'Phường Lê Chân'],
      ['10525', 'Phường Hải Dương'],
    ],
  },
  {
    id: 92,
    name: 'Thành phố Cần Thơ',
    wards: [
      ['31135', 'Phường Ninh Kiều'],
      ['31120', 'Phường Cái Khế'],
      ['31147', 'Phường Tân An'],
      ['31150', 'Phường An Bình'],
    ],
  },
];

export const PROVINCES: RegionDto[] = TREE.map(({ id, name }) => ({ id, name }));
export const WARDS: (WardDto & { province_id: number })[] = TREE.flatMap((p) =>
  p.wards.map(([code, name]) => ({ code, name, province_id: p.id })),
);

/** Tỉnh / Phường theo tên → dạng DTO. */
export function regionOf(province: string, ward: string): { province: RegionDto; ward: WardDto } {
  const p = PROVINCES.find((x) => x.name === province);
  const w = WARDS.find((x) => x.name === ward && x.province_id === p?.id);
  if (!p || !w) throw new Error(`mock thiếu đơn vị ${province}/${ward}`);
  return { province: p, ward: { code: w.code, name: w.name } };
}

export const seedAddresses = (): AddressDto[] => [
  {
    id: 1,
    recipient_name: 'Trần Thị Mai',
    phone_number: '0901234567',
    ...regionOf('Thành phố Hồ Chí Minh', 'Phường Sài Gòn'),
    address_line: '25 Nguyễn Huệ, Căn hộ 12A',
    label: 'HOME',
    is_default: true,
  },
  {
    id: 2,
    recipient_name: 'Trần Thị Mai (Cơ quan)',
    phone_number: '0902888222',
    ...regionOf('Thành phố Hồ Chí Minh', 'Phường Sài Gòn'),
    address_line: 'Tầng 9, Toà nhà Bitexco, 2 Hải Triều',
    label: 'OFFICE',
    is_default: false,
  },
];
