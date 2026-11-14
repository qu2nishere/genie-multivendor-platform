// Giỏ mẫu của người dùng đã đăng nhập (prototype index.html seedCart) dạng DTO 6.1, và cách mock
// dựng dòng giỏ từ SKU (giỏ khách, đổi phân loại, mua lại).
import { SEED_PRODUCTS, SEED_ROOT_CATEGORIES, SEED_SHOPS } from '@/shared/api/mock/seed';
import { productImg } from '@/shared/lib/assets';
import type { CartDto, CartItemDto, CartShopDto, VariantChoiceDto } from './cart.dto';

/** Một dòng trong kho mock: dòng giỏ + shop của nó. */
export interface MockCartRow {
  shop_id: number;
  item: CartItemDto;
}

// Phân loại mẫu (khớp product.mock): SKU = productId * 100 + màu * 10 + phiên bản.
const COLORS = [
  { label: 'Đen', delta: 0, stock: 38 },
  { label: 'Trắng', delta: 0, stock: 12 },
  { label: 'Xanh navy', delta: 30000, stock: 5 },
  { label: 'Hồng', delta: 20000, stock: 0 },
];
const VERSIONS = [
  { label: 'Tiêu chuẩn', delta: 0 },
  { label: 'Bản Pro', delta: 150000 },
];

const productOf = (id: number) => SEED_PRODUCTS.find((p) => p.id === id);
const categoryName = (categoryId: number) =>
  SEED_ROOT_CATEGORIES.find((c) => c.id === categoryId)?.name ?? null;

export function shopDto(shopId: number): CartShopDto {
  const s = SEED_SHOPS.find((x) => x.id === shopId);
  if (!s) throw new Error(`seed thiếu shop ${shopId}`);
  return { id: s.id, name: s.name, logo_url: null, is_mall: s.is_authentic };
}

/** SKU mock của nhãn phân loại tự do ở giỏ mẫu ("Blue switch", "30ml") — không trùng SKU màu × bản. */
function variantIdOf(productId: number, label: string): number {
  const [color, version] = label.split(' / ');
  const c = COLORS.findIndex((x) => x.label === color);
  const v = Math.max(
    0,
    VERSIONS.findIndex((x) => x.label === version),
  );
  return productId * 100 + (c >= 0 ? c * 10 + v : 99);
}

const seedRow = (
  id: number,
  productId: number,
  variant: string,
  price: number,
  qty: number,
  stock: number,
  snapshot: number | null = null,
): MockCartRow => {
  const p = productOf(productId);
  if (!p) throw new Error(`seed thiếu sản phẩm ${productId}`);
  return {
    shop_id: p.shop_id,
    item: {
      id,
      product_id: p.id,
      variant_id: variantIdOf(p.id, variant),
      product_slug: p.slug,
      category_name: categoryName(p.category_id),
      product_name: p.title,
      image_url: productImg(p.image),
      variant_name: variant,
      unit_price: price,
      original_price: p.original_price,
      price_at_add: snapshot,
      quantity: qty,
      stock,
      is_available: true,
      unavailable_reason: null,
    },
  };
};

/** Giỏ mẫu của prototype (11 dòng, 5 shop; 2 dòng đổi giá so với lúc thêm). */
export const seedCartRows = (): MockCartRow[] => [
  seedRow(7001, 1, 'Đen', 389000, 1, 38),
  seedRow(7002, 18, 'Blue switch', 459000, 1, 25, 429000),
  seedRow(7003, 2, 'Đen', 690000, 1, 20),
  seedRow(7004, 3, 'Trắng / 42', 259000, 2, 12),
  seedRow(7005, 14, 'Đen / 37', 320000, 1, 18, 289000),
  seedRow(7006, 13, '30ml', 178000, 1, 50),
  seedRow(7007, 12, 'Full set', 235000, 1, 30),
  seedRow(7008, 19, '50ml', 420000, 1, 22),
  seedRow(7009, 15, 'Xanh / L', 245000, 1, 40),
  seedRow(7010, 16, 'Trắng / M', 89000, 2, 100),
  seedRow(7011, 10, 'Trắng', 149000, 1, 60),
];

/**
 * Dòng giỏ dựng từ SKU (giỏ khách, đổi phân loại, mua lại). SKU lạ → null; SKU hết hàng vẫn trả
 * dòng nhưng đánh dấu không mua được.
 */
export function rowFromVariant(variantId: number, qty: number, id: number): MockCartRow | null {
  const p = productOf(Math.floor(variantId / 100));
  const c = COLORS[Math.floor((variantId % 100) / 10)];
  const v = VERSIONS[variantId % 10];
  if (!p || !c || !v) return null;
  return {
    shop_id: p.shop_id,
    item: {
      id,
      product_id: p.id,
      variant_id: variantId,
      product_slug: p.slug,
      category_name: categoryName(p.category_id),
      product_name: p.title,
      image_url: productImg(p.image),
      variant_name: `${c.label} / ${v.label}`,
      unit_price: p.price + c.delta + v.delta,
      original_price: p.original_price,
      price_at_add: null,
      quantity: c.stock > 0 ? Math.min(qty, c.stock) : qty,
      stock: c.stock,
      is_available: c.stock > 0,
      unavailable_reason: c.stock > 0 ? null : 'OUT_OF_STOCK',
    },
  };
}

/** Kho mock → phản hồi 6.1 (nhóm theo shop, giữ thứ tự xuất hiện). */
export function toCartDto(rows: MockCartRow[]): CartDto {
  const shops: CartDto['shops'] = [];
  for (const r of rows) {
    const g = shops.find((x) => x.shop.id === r.shop_id);
    if (g) g.items.push(r.item);
    else shops.push({ shop: shopDto(r.shop_id), items: [r.item] });
  }
  return { shops, item_count: rows.length };
}

/** Phân loại của sản phẩm mẫu (5.10): mọi sản phẩm dùng Màu sắc × Phiên bản như product.mock. */
export function variantChoiceDto(productId: number): VariantChoiceDto | null {
  const p = productOf(productId);
  if (!p) return null;
  return {
    option_groups: [
      { name: 'Màu sắc', values: COLORS.map((c) => c.label) },
      { name: 'Phiên bản', values: VERSIONS.map((v) => v.label) },
    ],
    variants: COLORS.flatMap((c, ci) =>
      VERSIONS.map((v, vi) => ({
        id: p.id * 100 + ci * 10 + vi,
        options: [c.label, v.label],
        price: p.price + c.delta + v.delta,
        stock: c.stock,
        image_url: null,
      })),
    ),
    image_url: productImg(p.image),
  };
}
