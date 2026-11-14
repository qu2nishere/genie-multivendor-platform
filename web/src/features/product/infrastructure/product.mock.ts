// Sản phẩm mẫu dạng DTO (prototype data.jsx PRODUCTS + ProductDetail.jsx COLORS / SIZES / SpecsTable /
// ProductDescription), dựng từ bộ seed chung. Lọc / sắp xếp ở đây mô phỏng việc backend làm.
import {
  SEED_PRODUCTS,
  SEED_ROOT_CATEGORIES,
  SEED_SHOPS,
  SEED_SUB_CATEGORIES,
} from '@/shared/api/mock/seed';
import { productImg } from '@/shared/lib/assets';
import { BRAND_SPEC_KEY, type ProductQuery } from '../domain/product.types';
import type { ProductDetailDto, ProductSummaryDto, VariantGroupDto } from './product.dto';

type SeedProduct = (typeof SEED_PRODUCTS)[number];

const shopOf = (id: number) => {
  const s = SEED_SHOPS.find((x) => x.id === id);
  if (!s) throw new Error(`seed thiếu shop ${id}`);
  return s;
};

export function toSummaryDto(p: SeedProduct): ProductSummaryDto {
  const shop = shopOf(p.shop_id);
  return {
    id: p.id,
    slug: p.slug,
    title: p.title,
    image: productImg(p.image),
    price: p.price,
    original_price: p.original_price,
    discount_pct: p.discount_pct,
    rating: p.rating,
    sold_count: p.sold_count,
    shop: { id: shop.id, name: shop.name, on_vacation: !!shop.vacation },
    category_id: p.category_id,
    free_ship: p.free_ship,
    is_authentic: p.is_authentic || shop.is_authentic,
    flash: p.flash_total ? { sold: p.flash_sold ?? 0, total: p.flash_total } : null,
    preorder_days: p.preorder_days,
    brand: p.brand,
    brand_verified: p.brand_verified,
    attribute: p.attribute,
  };
}

/** Phân loại dùng chung cho mọi sản phẩm mẫu (prototype ProductDetail.jsx COLORS, SIZES). */
const VARIANT_GROUPS: VariantGroupDto[] = [
  {
    name: 'Màu sắc',
    controls_stock: true,
    options: [
      { label: 'Đen', swatch: '#1A1815', price_delta: 0, stock: 38 },
      { label: 'Trắng', swatch: '#F1EFEC', price_delta: 0, stock: 12 },
      { label: 'Xanh navy', swatch: '#2A3550', price_delta: 30000, stock: 5 },
      { label: 'Hồng', swatch: '#FF7FA3', price_delta: 20000, stock: 0 },
    ],
  },
  {
    name: 'Phiên bản',
    controls_stock: false,
    options: [
      { label: 'Tiêu chuẩn', price_delta: 0 },
      { label: 'Bản Pro', price_delta: 150000 },
    ],
  },
];

const description = (shopName: string) => `
<p class="lead"><strong>${shopName} — Tai nghe Bluetooth chính hãng</strong></p>
<p>Tai Nghe Bluetooth ANC Pro 5 — Chống Ồn Chủ Động, Pin 40 Giờ, Bluetooth 5.3, Driver 40mm Titanium.</p>
<h3>Thông tin sản phẩm:</h3>
<ul>
  <li>Kết nối: Bluetooth 5.3 + USB-C (chế độ có dây)</li>
  <li>Driver: 40mm Titanium — âm trầm sâu, treble trong</li>
  <li>Chống ồn: ANC chủ động, chế độ Xuyên âm (Ambient)</li>
  <li>Pin: 40 giờ (ANC tắt), 30 giờ (ANC bật)</li>
  <li>Sạc nhanh: 10 phút sạc = 3 giờ nghe</li>
  <li>Trọng lượng: 250g, đệm tai Memory Foam</li>
</ul>
<h3>Cam kết từ ${shopName}:</h3>
<ul class="checks">
  <li>Hàng chính hãng 100%</li>
  <li>Đổi trả miễn phí trong 7 ngày</li>
  <li>Bảo hành 12 tháng tại trung tâm bảo hành</li>
  <li>Giao hàng toàn quốc</li>
</ul>
<h3>Lưu ý khi sử dụng:</h3>
<ul>
  <li>Tránh để tai nghe tiếp xúc với nước</li>
  <li>Sạc đầy pin trước khi sử dụng lần đầu</li>
  <li>Không vặn âm lượng quá lớn để bảo vệ thính giác</li>
</ul>
<hr />
<h3>Bộ sản phẩm bao gồm:</h3>
<ul>
  <li>1× Tai nghe ANC Pro 5</li>
  <li>1× Cáp sạc USB-C (1m)</li>
  <li>1× Bao đựng cứng (hardcase)</li>
  <li>1× Sách hướng dẫn sử dụng</li>
</ul>`;

export function toDetailDto(p: SeedProduct): ProductDetailDto {
  const shop = shopOf(p.shop_id);
  return {
    ...toSummaryDto(p),
    sku: `GN-${String(p.id).padStart(5, '0')}`,
    // prototype: ảnh chính + 3 ảnh minh họa cố định
    images: [
      productImg(p.image),
      productImg('banner-tech'),
      productImg('keyboard'),
      productImg('speaker'),
    ],
    review_count: p.sold_count,
    variant_groups: VARIANT_GROUPS,
    // mọi tổ hợp màu × phiên bản: id = productId * 100 + màu * 10 + phiên bản
    variants: VARIANT_GROUPS[0].options.flatMap((_, c) =>
      VARIANT_GROUPS[1].options.map((__, v) => ({
        id: p.id * 100 + c * 10 + v,
        option_indexes: [c, v],
      })),
    ),
    specs: [
      { key: BRAND_SPEC_KEY, value: p.brand },
      { key: 'Chất liệu', value: 'Nhựa ABS + Polycarbonate' },
      { key: 'Loại kết nối', value: 'Bluetooth 5.3, USB-C' },
      { key: 'Tính năng', value: 'Chống ồn chủ động (ANC), Xuyên âm' },
      { key: 'Thời lượng pin', value: '40 giờ (ANC tắt), 30 giờ (ANC bật)' },
      { key: 'Trọng lượng', value: '250g' },
      { key: 'Bảo hành', value: '12 tháng chính hãng' },
      { key: 'Tình trạng', value: 'Mới' },
      { key: 'Xuất xứ', value: 'Trung Quốc' },
      { key: 'Gửi từ', value: shop.location },
    ],
    description_html: description(shop.name),
  };
}

/** Shop có voucher shop đang chạy (từ voucher mẫu của promotions: shop 1–5). */
const SHOPS_WITH_VOUCHER: readonly number[] = [1, 2, 3, 4, 5];

/** Lọc + sắp xếp như backend (prototype Category.jsx / Search.jsx). */
export function queryProducts(q: ProductQuery): SeedProduct[] {
  let list: SeedProduct[] = [...SEED_PRODUCTS];
  const term = q.q?.trim().toLowerCase();
  if (q.categoryId) list = list.filter((p) => p.category_id === q.categoryId);
  if (q.categoryIds?.length) list = list.filter((p) => q.categoryIds?.includes(p.category_id));
  if (q.subcategoryId) {
    const ids: readonly number[] =
      SEED_SUB_CATEGORIES.find((s) => s.id === q.subcategoryId)?.product_ids ?? [];
    list = list.filter((p) => ids.includes(p.id));
  }
  if (q.shopId) list = list.filter((p) => p.shop_id === q.shopId);
  if (term) {
    // prototype: khớp tên sản phẩm hoặc tên danh mục
    const catName = (id: number) => SEED_ROOT_CATEGORIES.find((c) => c.id === id)?.name ?? '';
    list = list.filter((p) => `${p.title} ${catName(p.category_id)}`.toLowerCase().includes(term));
  }
  if (q.minPrice != null) list = list.filter((p) => p.price >= (q.minPrice ?? 0));
  if (q.maxPrice != null) list = list.filter((p) => p.price <= (q.maxPrice ?? Infinity));
  if (q.brands?.length) list = list.filter((p) => q.brands?.includes(p.brand));
  if (q.locations?.length)
    list = list.filter((p) => q.locations?.includes(shopOf(p.shop_id).location));
  if (q.authentic)
    list = list.filter((p) => p.is_authentic || p.brand_verified || shopOf(p.shop_id).is_authentic);
  if (q.freeship) list = list.filter((p) => p.free_ship);
  if (q.hasVoucher) list = list.filter((p) => SHOPS_WITH_VOUCHER.includes(p.shop_id));
  if (q.minRating) list = list.filter((p) => p.rating >= (q.minRating ?? 0));
  if (q.attributes?.length)
    list = list.filter((p) => !!p.attribute && q.attributes?.includes(p.attribute));
  if (q.flash) list = list.filter((p) => p.is_flash);
  if (q.minDiscount) list = list.filter((p) => p.discount_pct >= (q.minDiscount ?? 0));
  switch (q.sort) {
    case 'hot':
      list.sort((a, b) => b.sold_count - a.sold_count);
      break;
    case 'new':
      list.reverse();
      break;
    case 'price_asc':
      list.sort((a, b) => a.price - b.price);
      break;
    case 'price_desc':
      list.sort((a, b) => b.price - a.price);
      break;
    default:
      break;
  }
  return list;
}

export const findSeedProduct = (id: number) => SEED_PRODUCTS.find((p) => p.id === id);

/** Thương hiệu / nơi bán có trong tập (prototype brandList, placeList: lấy trước khi lọc). */
export function facetsOf(list: SeedProduct[]) {
  return {
    brands: Array.from(new Set(list.map((p) => p.brand))),
    locations: Array.from(new Set(list.map((p) => shopOf(p.shop_id).location))),
    total: list.length,
  };
}
