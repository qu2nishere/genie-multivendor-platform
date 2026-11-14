// Cây danh mục, bộ thuộc tính, đề xuất, sản phẩm theo danh mục (prototype admin/Catalog.jsx TREE,
// LEAVES, PENDING, MOCK_PRODUCTS; data.jsx CATEGORY_ATTRS) — hình DTO 11.11. Mã danh mục gốc trùng
// danh mục storefront (Điện tử = 1…) để lọc / liên kết nhất quán.
import { mockState } from '@/shared/api/mock';
import { productImg } from '@/shared/lib/assets';
import type { AttributeDto, CatalogProductDto, CategoryDto, ProposalDto } from './catalog.dto';

type Sub = [number, string, number, number, string[]];
let leafId = 1000;
const root = (
  id: number,
  name: string,
  slug: string,
  icon: string,
  products: number,
  shops: number,
  rate: number,
  active: boolean,
  subs: Sub[],
): CategoryDto => ({
  id,
  name,
  slug,
  icon,
  is_active: active,
  commission_rate: rate,
  product_count: products,
  shop_count: shops,
  children: subs.map(([sid, sname, count, srate, leaves]) => ({
    id: sid,
    name: sname,
    commission_rate: srate,
    product_count: count,
    children: leaves.map((l) => ({ id: ++leafId, name: l })),
  })),
});

export const categoriesStore = mockState<CategoryDto[]>('admin.categories', () => [
  root(1, 'Điện tử', 'dien-tu', 'zap', 12840, 312, 5, true, [
    [11, 'Điện thoại & Phụ kiện', 4210, 4, ['Điện thoại', 'Ốp lưng & Bao da', 'Sạc & Cáp']],
    [12, 'Máy tính & Laptop', 3120, 5, ['Laptop', 'Bàn phím', 'Chuột']],
    [13, 'Âm thanh & Tai nghe', 2980, 6, ['Tai nghe Bluetooth', 'Loa Bluetooth']],
    [14, 'Camera & Máy quay', 2530, 5, ['Mirrorless', 'Action cam']],
  ]),
  root(2, 'Thời trang', 'thoi-trang', 'shopping-bag', 28410, 894, 8, true, [
    [21, 'Thời trang Nam', 9800, 8, ['Áo thun', 'Quần jeans']],
    [22, 'Thời trang Nữ', 12400, 8, ['Đầm & Váy', 'Áo kiểu']],
    [23, 'Giày dép', 6210, 7, ['Sneaker', 'Sandal']],
  ]),
  root(3, 'Làm đẹp', 'lam-dep', 'star', 9120, 421, 10, true, [
    [31, 'Chăm sóc da', 3800, 10, ['Serum', 'Kem chống nắng']],
    [32, 'Trang điểm', 3120, 10, ['Son', 'Phấn']],
    [33, 'Nước hoa', 2200, 9, []],
  ]),
  root(4, 'Nhà cửa & Đời sống', 'nha-cua', 'house', 15200, 540, 6, true, [
    [41, 'Nội thất', 4100, 6, ['Bàn', 'Ghế', 'Đèn']],
    [42, 'Đồ dùng nhà bếp', 5600, 6, ['Nồi & Chảo', 'Dao & Thớt']],
    [43, 'Trang trí', 5500, 7, []],
  ]),
  root(6, 'Mẹ & Bé', 'me-be', 'gift', 6840, 218, 7, true, [
    [61, 'Đồ dùng cho bé', 3400, 7, []],
    [62, 'Thời trang mẹ & bé', 3440, 7, []],
  ]),
  root(9, 'Voucher & Dịch vụ', 'voucher-dich-vu', 'ticket', 320, 28, 0, false, [
    [91, 'Thẻ nạp & Dịch vụ', 320, 0, []],
  ]),
]);

const a = (
  code: string,
  name: string,
  data_type: AttributeDto['data_type'],
  values: string[],
  unit: string | null,
  is_required: boolean,
  is_filterable: boolean,
  is_variant_axis: boolean,
): AttributeDto => ({
  code,
  name,
  data_type,
  values,
  unit,
  is_required,
  is_filterable,
  is_variant_axis,
  is_active: true,
});

export const attributesStore = mockState<Record<number, AttributeDto[]>>(
  'admin.attributes',
  () => ({
    1: [
      a(
        'warranty',
        'Bảo hành',
        'ENUM',
        ['6 tháng', '12 tháng', '24 tháng'],
        null,
        true,
        true,
        false,
      ),
      a('color', 'Màu sắc', 'MULTI_ENUM', ['Đen', 'Trắng', 'Xanh'], null, false, true, true),
      a('battery', 'Thời lượng pin', 'NUMBER', [], 'giờ', false, false, false),
    ],
    2: [
      a('material', 'Chất liệu', 'TEXT', [], null, true, true, false),
      a('size', 'Kích cỡ', 'ENUM', ['S', 'M', 'L', 'XL'], null, true, true, true),
    ],
    3: [a('expiry', 'Hạn sử dụng', 'DATE', [], null, true, false, false)],
  }),
);

export const proposalsStore = mockState<ProposalDto[]>('admin.proposals', () => [
  {
    id: 81,
    name: 'Thú cưng (Pet)',
    shop: { id: 40, name: 'PetWorld VN' },
    parent: null,
    reason: 'Shop bán thức ăn, phụ kiện cho chó mèo; chưa có ngành phù hợp.',
    created_at: '2026-06-08T09:00:00+07:00',
  },
  {
    id: 80,
    name: 'Đồ chơi mô hình',
    shop: { id: 41, name: 'GundamStore' },
    parent: { id: 6, name: 'Mẹ & Bé' },
    reason: 'Mô hình lắp ráp cho trẻ trên 8 tuổi.',
    created_at: '2026-06-07T15:30:00+07:00',
  },
]);

const p = (
  id: number,
  name: string,
  img: string,
  shop: string,
  price: number,
  stock: number,
  status: CatalogProductDto['status'],
  reason: string | null = null,
): CatalogProductDto => ({
  id,
  name,
  image_url: productImg(img),
  shop_name: shop,
  price,
  stock,
  status,
  is_locked: false,
  reason,
});

/** Sản phẩm mẫu của một danh mục con (prototype dùng cùng 4 sản phẩm cho mọi danh mục con). */
export const catalogProductsStore = mockState<CatalogProductDto[]>('admin.catalogProducts', () => [
  p(1, 'Tai nghe Bluetooth ANC', 'headphones', 'TechZone', 389000, 38, 'PUBLISHED'),
  p(2, 'iPhone 15 Pro Max 256GB', 'phone', 'AppleStore SG', 22990000, 15, 'PENDING_APPROVAL'),
  p(3, 'Ốp lưng silicon trong suốt', 'phone', 'PhoneCase VN', 49000, 200, 'PENDING_APPROVAL'),
  p(
    4,
    'Sạc nhanh 65W GaN',
    'laptop',
    'ChargerPro',
    350000,
    80,
    'REJECTED',
    'Thiếu chứng nhận an toàn',
  ),
]);
