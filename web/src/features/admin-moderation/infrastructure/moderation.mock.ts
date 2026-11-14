// Chi tiết sản phẩm chờ duyệt + nhân viên duyệt (prototype admin/data.jsx MODERATION, MODERATORS).
// Hàng danh sách / trạng thái ở kho dùng chung shared/api/mock/admin.ts (adminModerationStore).
import { mockState } from '@/shared/api/mock';
import { MODERATORS } from '@/shared/api/mock/admin';
import { productImg } from '@/shared/lib/assets';
import type { ModerationDetailDto, ModerationRowDto, ModeratorDto } from './moderation.dto';

export type ModerationExtraDto = Omit<
  ModerationDetailDto,
  keyof ModerationRowDto | 'decision_reason'
>;

const DOC = '/img/mock/product-doc.svg';
const docs = (names: string[]) =>
  names.map((file_name, i) => ({ id: 50 + i, file_name, url: DOC }));
const sku = (rows: [string, string, number, number][]) =>
  rows.map(([s, name, price, stock]) => ({ sku: s, name, price, stock }));

export const MODERATION_EXTRA: Record<number, ModerationExtraDto> = {
  1284: {
    ai_result: { decision: 'APPROVE', risk_score: 12, violations: [], reasoning: null },
    images: [productImg('lamp')],
    documents: docs(['hoa-don-nhap-hang.pdf']),
    category_path: 'Nhà cửa > Nội thất > Đèn',
    brand: 'No Brand',
    condition: 'NEW',
    origin: 'Việt Nam',
    description_html: '<p>Đèn bàn LED cảm ứng, 3 mức sáng, sạc USB-C, pin 8 giờ.</p>',
    weight_grams: 450,
    dimensions_cm: { length: 18, width: 18, height: 40 },
    ships_from: 'Hà Nội',
    shipping_payer: 'BUYER',
    variants: sku([
      ['LAMP-WHT', 'Trắng', 149000, 60],
      ['LAMP-BLK', 'Đen', 149000, 42],
    ]),
  },
  1283: {
    ai_result: {
      decision: 'NEED_REVIEW',
      risk_score: 62,
      violations: ['COUNTERFEIT'],
      reasoning:
        'Ảnh sản phẩm trùng logo thương hiệu lớn nhưng giấy ủy quyền chưa khớp tên shop. Cần người kiểm tra.',
    },
    images: [productImg('perfume')],
    documents: docs(['giay-uy-quyen.pdf', 'phieu-cong-bo-my-pham.pdf']),
    category_path: 'Làm đẹp > Nước hoa',
    brand: 'Eau de Parfum',
    condition: 'NEW',
    origin: 'Pháp',
    description_html: '<p>Nước hoa mini 10ml, hương hoa cỏ, lưu hương 6 giờ.</p>',
    weight_grams: 80,
    dimensions_cm: { length: 4, width: 4, height: 10 },
    ships_from: 'TP. HCM',
    shipping_payer: 'SELLER',
    variants: sku([['PF-10', '10ml', 129000, 300]]),
  },
  1279: {
    ai_result: { decision: 'APPROVE', risk_score: 18, violations: [], reasoning: null },
    images: [productImg('headphones')],
    documents: docs(['ce-certificate.pdf']),
    category_path: 'Điện tử > Tai nghe & Loa > Tai nghe Bluetooth',
    brand: 'Sony',
    condition: 'NEW',
    origin: 'Trung Quốc',
    description_html:
      '<p>Shop đổi ảnh và tên sản phẩm nên cần duyệt lại. Giá và tồn kho không đổi.</p>',
    weight_grams: 250,
    dimensions_cm: { length: 20, width: 18, height: 8 },
    ships_from: 'TP. HCM',
    shipping_payer: 'BUYER',
    variants: sku([
      ['ANC5-BLK', 'Đen', 389000, 38],
      ['ANC5-WHT', 'Trắng', 389000, 12],
    ]),
  },
  1270: {
    ai_result: { decision: 'APPROVE', risk_score: 9, violations: [], reasoning: null },
    images: [productImg('keyboard')],
    documents: docs(['hoa-don-nhap-hang.pdf']),
    category_path: 'Điện tử > Máy tính & Laptop > Bàn phím',
    brand: 'Logitech',
    condition: 'NEW',
    origin: 'Trung Quốc',
    description_html: '<p>Bàn phím cơ 87 phím, switch blue, LED RGB.</p>',
    weight_grams: 900,
    dimensions_cm: { length: 36, width: 14, height: 4 },
    ships_from: 'TP. HCM',
    shipping_payer: 'BUYER',
    variants: sku([['KB-BLUE', 'Blue switch', 459000, 54]]),
  },
  1266: {
    ai_result: {
      decision: 'REJECT',
      risk_score: 91,
      violations: ['PROHIBITED_ITEM', 'BAD_IMAGE'],
      reasoning:
        'Không có giấy tờ chứng nhận hợp quy cho thiết bị sạc; ảnh có dấu hiệu lấy từ website khác.',
    },
    images: [productImg('phone')],
    documents: [],
    category_path: 'Điện tử > Điện thoại & Phụ kiện > Sạc & Cáp',
    brand: 'No Brand',
    condition: 'NEW',
    origin: 'Đang cập nhật',
    description_html: '<p>Sạc nhanh 65W GaN.</p>',
    weight_grams: 120,
    dimensions_cm: { length: 6, width: 6, height: 3 },
    ships_from: 'Bình Dương',
    shipping_payer: 'BUYER',
    variants: sku([['CHG-65', 'Trắng', 89000, 500]]),
  },
};

export const moderatorsStore = mockState<ModeratorDto[]>('admin.moderators', () =>
  MODERATORS.map((m) => ({ ...m })),
);
