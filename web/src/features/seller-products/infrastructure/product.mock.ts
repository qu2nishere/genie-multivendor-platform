// Dữ liệu mẫu (prototype Products.jsx CATEGORY_TREE, BRANDS, SUGGESTED_SPECS, REQUIRED_SPECS,
// requiresDocs, form "Bàn phím cơ"; VendorCatalog.jsx TREE, SEED). Danh sách sản phẩm của shop nằm ở kho
// dùng chung shared/api/mock/seller.ts; chi tiết form giữ trong kho riêng `seller.productDetails`.
import { mockState } from '@/shared/api/mock';
import type { CategoryNode } from '../domain/product.types';
import type { CatalogNodeDto, ListingRulesDto, ProductDetailDto, ProposalDto } from './product.dto';

/** Cây 3 cấp cho ô chọn danh mục; id: gốc 1–5, cấp 2 = gốc×10+j, cấp 3 = cấp2×10+k. */
const RAW: [string, [string, string[]][]][] = [
  [
    'Điện tử',
    [
      [
        'Điện thoại & Phụ kiện',
        ['Điện thoại', 'Ốp lưng & Bao da', 'Miếng dán màn hình', 'Sạc & Cáp', 'Phụ kiện khác'],
      ],
      [
        'Máy tính & Laptop',
        ['Laptop', 'PC & Linh kiện', 'Bàn phím', 'Chuột', 'Phụ kiện PC', 'Webcam'],
      ],
      [
        'Tai nghe & Loa',
        ['Tai nghe có dây', 'Tai nghe Bluetooth', 'Loa Bluetooth', 'Loa thông minh'],
      ],
      ['Camera', ['Mirrorless', 'DSLR', 'Action cam', 'Phụ kiện camera']],
    ],
  ],
  [
    'Thời trang',
    [
      ['Thời trang Nam', ['Áo thun', 'Áo sơ mi', 'Quần jeans', 'Quần short', 'Giày', 'Phụ kiện']],
      ['Thời trang Nữ', ['Đầm & Váy', 'Áo kiểu', 'Quần', 'Giày & Dép', 'Túi xách', 'Phụ kiện']],
      ['Đồ lót & Đồ ngủ', ['Đồ lót nam', 'Đồ lót nữ', 'Đồ ngủ']],
    ],
  ],
  [
    'Làm đẹp',
    [
      ['Chăm sóc da', ['Sữa rửa mặt', 'Kem chống nắng', 'Serum', 'Toner', 'Kem dưỡng']],
      ['Trang điểm', ['Son', 'Phấn', 'Mascara', 'Kẻ mắt', 'Kem nền']],
      ['Chăm sóc tóc', ['Dầu gội', 'Dầu xả', 'Serum tóc']],
    ],
  ],
  [
    'Nhà cửa & Đời sống',
    [
      ['Nội thất', ['Bàn', 'Ghế', 'Kệ sách', 'Đèn']],
      ['Đồ dùng nhà bếp', ['Nồi & Chảo', 'Dao & Thớt', 'Gia vị & Đồ khô']],
      ['Đồ dùng phòng tắm', ['Khăn', 'Đồ dùng vệ sinh']],
    ],
  ],
  [
    'Mẹ & Bé',
    [
      ['Đồ dùng cho bé', ['Tã & Bỉm', 'Sữa & Dinh dưỡng', 'Đồ chơi']],
      ['Đồ dùng cho mẹ', ['Thời trang bầu', 'Đồ dùng sau sinh']],
    ],
  ],
];

export const CATEGORY_TREE: CategoryNode[] = RAW.map(([name, l2], i) => {
  const id = i + 1;
  return {
    id,
    name,
    children: l2.map(([n2, l3], j) => {
      const id2 = id * 10 + j + 1;
      return {
        id: id2,
        name: n2,
        children: l3.map((n3, k) => ({ id: id2 * 10 + k + 1, name: n3 })),
      };
    }),
  };
});

/** Đường dẫn gốc → lá của một danh mục lá. */
export function categoryPath(leafId: number): { id: number; name: string }[] {
  for (const a of CATEGORY_TREE)
    for (const b of a.children ?? [])
      for (const c of b.children ?? [])
        if (c.id === leafId) return [a, b, c].map(({ id, name }) => ({ id, name }));
  return [];
}
export const leafByName = (l2: string, l3: string): number =>
  CATEGORY_TREE.flatMap((a) => a.children ?? [])
    .find((b) => b.name === l2)
    ?.children?.find((c) => c.name === l3)?.id ?? 0;

/** Hoa hồng + số sản phẩm theo ngành cấp 1–2 (VendorCatalog.jsx TREE). */
const RATES: Record<string, [number, number]> = {
  'Điện tử': [5, 1284],
  'Điện thoại & Phụ kiện': [5, 412],
  'Máy tính & Laptop': [4, 386],
  'Tai nghe & Loa': [6, 301],
  Camera: [5, 185],
  'Thời trang': [8, 3120],
  'Thời trang Nam': [8, 1210],
  'Thời trang Nữ': [8, 1640],
  'Đồ lót & Đồ ngủ': [9, 270],
  'Làm đẹp': [7, 980],
  'Chăm sóc da': [7, 540],
  'Trang điểm': [7, 320],
  'Chăm sóc tóc': [6, 120],
  'Nhà cửa & Đời sống': [6, 740],
  'Nội thất': [6, 310],
  'Đồ dùng nhà bếp': [6, 280],
  'Đồ dùng phòng tắm': [6, 150],
  'Mẹ & Bé': [5, 460],
  'Đồ dùng cho bé': [5, 350],
  'Đồ dùng cho mẹ': [5, 110],
};

export const CATALOG: CatalogNodeDto[] = CATEGORY_TREE.map((a) => ({
  id: a.id,
  name: a.name,
  commission_rate: RATES[a.name][0],
  product_count: RATES[a.name][1],
  children: (a.children ?? []).map((b) => ({
    id: b.id,
    name: b.name,
    commission_rate: RATES[b.name][0],
    product_count: RATES[b.name][1],
  })),
}));

const SUGGESTED: Record<string, string[]> = {
  'Điện tử': ['Bảo hành', 'Kết nối', 'Pin', 'Trọng lượng sản phẩm'],
  'Thời trang': ['Chất liệu', 'Xuất xứ vải', 'Hướng dẫn giặt'],
  'Làm đẹp': ['Thành phần', 'Dung tích', 'Hạn sử dụng', 'Loại da phù hợp'],
};
const REQUIRED: Record<string, string[]> = {
  'Điện tử': ['Bảo hành'],
  'Thời trang': ['Chất liệu'],
  'Làm đẹp': ['Hạn sử dụng'],
};

export function listingRules(leafId: number): ListingRulesDto {
  const path = categoryPath(leafId);
  const root = path[0]?.name ?? '';
  const l2 = path[1]?.name ?? root;
  return {
    category_id: leafId,
    commission_rate: RATES[l2]?.[0] ?? 5,
    requires_documents: root === 'Làm đẹp' || path[2]?.name === 'Sữa & Dinh dưỡng',
    attributes: (SUGGESTED[root] ?? []).map((name, i) => ({
      code: `a${i}`,
      name,
      data_type: 'TEXT',
      values: [],
      unit: null,
      is_required: (REQUIRED[root] ?? []).includes(name),
      is_variant_axis: false,
    })),
  };
}

export const brandsStore = mockState<{ id: number; name: string }[]>('seller.brands', () =>
  [
    'Apple',
    'Samsung',
    'Xiaomi',
    'Sony',
    'Logitech',
    'Anker',
    'JBL',
    'No Brand',
    'OEM',
    'Baseus',
    'Ugreen',
  ].map((name, i) => ({ id: i + 1, name })),
);

const img = (n: string, i = 1) => ({
  object_key: `product-media/vendor/1/${n}/${i}.webp`,
  url: `/img/products/${n}.jpg`,
});

const KEYBOARD_DESC =
  '<p><b>BÀN PHÍM CƠ RGB — GÕ SƯỚNG TAY, BỀN BỈ</b></p>' +
  '<p>Bàn phím cơ full-size với switch Cherry MX Blue, hành trình phím rõ ràng, phản hồi nhanh — lý tưởng cho cả làm việc và gaming.</p>' +
  '<p><b>Thông tin sản phẩm:</b></p>' +
  '<ul><li>Chất liệu: Nhựa ABS cao cấp, khung kim loại</li><li>Switch: Cherry MX Blue (clicky)</li><li>Kết nối: USB-C có dây &amp; Bluetooth 5.0</li><li>Đèn nền RGB 16.8 triệu màu, 18 hiệu ứng</li></ul>' +
  '<p><b>Cam kết:</b></p>' +
  '<ul><li>✅ Hàng chính hãng 100%</li><li>✅ Đổi trả miễn phí trong 7 ngày</li><li>✅ Bảo hành 12 tháng · Giao hàng toàn quốc</li></ul>';

const base = (
  id: number,
  name: string,
  pic: string,
  leaf: [string, string],
  status: ProductDetailDto['status'],
  price: number,
  stock: number,
  brand: string,
  extra: Partial<ProductDetailDto> = {},
): ProductDetailDto => ({
  id,
  status,
  reject_reason: null,
  images: [img(pic)],
  video: null,
  documents: [],
  name,
  category_path: categoryPath(leafByName(leaf[0], leaf[1])),
  brand: { id: 0, name: brand },
  condition: 'NEW',
  origin: 'Việt Nam',
  specs: [{ name: 'Bảo hành', value: '12 tháng chính hãng' }],
  description_html: `<p>${name} — hàng chính hãng, bảo hành 12 tháng.</p>`,
  option_groups: [],
  variant_image_group: null,
  variants: [{ id: id * 100 + 1, options: [], sku: `SP-${id}`, price, stock }],
  weight_grams: 400,
  dimensions_cm: { length: 20, width: 15, height: 8 },
  shipping_payer: 'BUYER',
  free_shipping_threshold: null,
  is_pre_order: false,
  pre_order_days: null,
  ...extra,
});

const kb = (opt: [string, string], i: number, stock: number) => ({
  id: 600 + i,
  options: opt,
  sku: `KB-${opt[0].slice(0, 2).toUpperCase()}-${opt[1].toUpperCase()}`,
  price: 459000,
  stock,
});

export const productDetailsStore = mockState<Record<number, ProductDetailDto>>(
  'seller.productDetails',
  () => ({
    1: base(
      1,
      'Tai nghe Bluetooth ANC Pro 5',
      'headphones',
      ['Tai nghe & Loa', 'Tai nghe Bluetooth'],
      'PUBLISHED',
      389000,
      38,
      'Sony',
      {
        option_groups: [
          { name: 'Màu sắc', values: [{ value: 'Đen' }, { value: 'Trắng' }] },
          { name: 'Phiên bản', values: [{ value: 'Pro' }, { value: 'Lite' }] },
        ],
        variant_image_group: 'Màu sắc',
        variants: [
          { id: 101, options: ['Đen', 'Pro'], sku: 'ANC-DE-PRO', price: 389000, stock: 12 },
          { id: 102, options: ['Đen', 'Lite'], sku: 'ANC-DE-LIT', price: 329000, stock: 10 },
          { id: 103, options: ['Trắng', 'Pro'], sku: 'ANC-TR-PRO', price: 389000, stock: 9 },
          { id: 104, options: ['Trắng', 'Lite'], sku: 'ANC-TR-LIT', price: 329000, stock: 7 },
        ],
      },
    ),
    2: base(
      2,
      'Đèn bàn LED cảm ứng 3 chế độ',
      'lamp',
      ['Nội thất', 'Đèn'],
      'PENDING_APPROVAL',
      149000,
      60,
      'No Brand',
      {
        specs: [],
      },
    ),
    3: base(
      3,
      'Sạc nhanh 65W (thiếu giấy tờ)',
      'phone',
      ['Điện thoại & Phụ kiện', 'Sạc & Cáp'],
      'REJECTED',
      280000,
      25,
      'Anker',
      {
        reject_reason: 'Thiếu chứng nhận an toàn',
      },
    ),
    4: base(
      4,
      'Đồng hồ thông minh Watch S2',
      'watch',
      ['Điện thoại & Phụ kiện', 'Phụ kiện khác'],
      'PUBLISHED',
      690000,
      12,
      'Xiaomi',
    ),
    5: base(
      5,
      'Loa bluetooth mini IPX7',
      'speaker',
      ['Tai nghe & Loa', 'Loa Bluetooth'],
      'PUBLISHED',
      320000,
      0,
      'JBL',
    ),
    6: base(
      6,
      'Bàn phím cơ RGB switch blue Full-size',
      'keyboard',
      ['Máy tính & Laptop', 'Bàn phím'],
      'DRAFT',
      459000,
      54,
      'Logitech',
      {
        images: [img('keyboard', 1), img('keyboard', 2), img('keyboard', 3)],
        documents: [
          {
            id: 31,
            file_name: 'hoa-don-nhap-hang.pdf',
            object_key: 'product-docs/vendor/1/6/hoa-don-nhap-hang.pdf',
            url: '',
          },
        ],
        specs: [
          { name: 'Chất liệu', value: 'Nhựa ABS + Polycarbonate' },
          { name: 'Loại switch', value: 'Cherry MX Blue' },
          { name: 'Layout', value: 'Full-size (104 phím)' },
          { name: 'Kết nối', value: 'USB-C, Bluetooth 5.0' },
          { name: 'Đèn nền', value: 'RGB 16.8 triệu màu' },
          { name: 'Pin', value: '4000mAh' },
          { name: 'Bảo hành', value: '12 tháng chính hãng' },
        ],
        description_html: KEYBOARD_DESC,
        option_groups: [
          { name: 'Màu sắc', values: [{ value: 'Đen' }, { value: 'Trắng' }] },
          { name: 'Switch', values: [{ value: 'Blue' }, { value: 'Red' }] },
        ],
        variant_image_group: 'Màu sắc',
        variants: [
          kb(['Đen', 'Blue'], 1, 20),
          kb(['Đen', 'Red'], 2, 14),
          kb(['Trắng', 'Blue'], 3, 12),
          kb(['Trắng', 'Red'], 4, 8),
        ],
        weight_grams: 850,
        dimensions_cm: { length: 30, width: 20, height: 10 },
      },
    ),
    7: base(
      7,
      'Camera Mirrorless X-100 (bản mới)',
      'camera',
      ['Camera', 'Mirrorless'],
      'DRAFT',
      12990000,
      8,
      'Sony',
      {
        specs: [],
      },
    ),
  }),
);

export const proposalsStore = mockState<ProposalDto[]>('seller.proposals', () => [
  {
    id: 81,
    code: 'CP-081',
    name: 'Thiết bị đeo thông minh',
    parent: { id: 1, name: 'Điện tử' },
    status: 'PENDING',
    reject_reason: null,
    created_at: '2026-06-06T10:00:00+07:00',
  },
  {
    id: 64,
    code: 'CP-064',
    name: 'Phụ kiện gaming',
    parent: { id: 1, name: 'Điện tử' },
    status: 'APPROVED',
    reject_reason: null,
    created_at: '2026-05-18T09:00:00+07:00',
  },
]);
