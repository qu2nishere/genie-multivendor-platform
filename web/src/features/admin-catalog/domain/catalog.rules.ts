// Hàm thuần danh mục toàn sàn (API_SPEC 11.11, 16.4).
import type {
  AttributeType,
  CatalogProduct,
  CatalogProductTab,
  CategoryAttribute,
  CategoryNode,
  RootCategory,
} from './catalog.types';

export const ATTR_TYPES: AttributeType[] = [
  'TEXT',
  'NUMBER',
  'BOOLEAN',
  'ENUM',
  'MULTI_ENUM',
  'DATE',
];

/** Biểu tượng chọn được cho danh mục (prototype CAT_ICONS). */
export const CAT_ICONS = [
  'zap',
  'shopping-bag',
  'star',
  'house',
  'gift',
  'ticket',
  'package',
  'shopping-cart',
  'heart',
  'book',
  'dumbbell',
  'gamepad-2',
] as const;

export const CATALOG_PRODUCT_TABS: CatalogProductTab[] = [
  'ALL',
  'PENDING',
  'PUBLISHED',
  'HIDDEN_OR_LOCKED',
];

/** Lý do khóa sản phẩm vi phạm (16.4 — cùng bộ với từ chối sản phẩm). */
export const LOCK_REASONS = [
  'POLICY_VIOLATION',
  'BAD_IMAGE',
  'MISLEADING_DESCRIPTION',
  'UNREASONABLE_PRICE',
  'PROHIBITED_ITEM',
  'MISSING_SAFETY_CERT',
] as const;
export type LockReason = (typeof LOCK_REASONS)[number];

/** Mã thuộc tính: chữ thường, số, gạch dưới, bắt đầu bằng chữ (API 11.11). */
export const ATTR_CODE = /^[a-z][a-z0-9_]*$/;
export const SLUG = /^[a-z0-9]+(?:-[a-z0-9]+)*$/;
/** Khung hoa hồng 3,5–8 % (plan B2 D1 — ENTITY 2.1, backend `CommissionRates`). */
export const MIN_COMMISSION = 3.5;
export const MAX_COMMISSION = 8;

export const needsValues = (t: AttributeType): boolean => t === 'ENUM' || t === 'MULTI_ENUM';

/** "6 tháng, 12 tháng , ,24 tháng" → ["6 tháng", "12 tháng", "24 tháng"] (bỏ trùng). */
export function parseValues(text: string): string[] {
  return [
    ...new Set(
      text
        .split(',')
        .map((s) => s.trim())
        .filter(Boolean),
    ),
  ];
}

/** Xóa được ở mọi cấp khi không còn sản phẩm và ngành con (GNE-CAT-4092; plan 2026-11-24 H15). */
export const canDeleteCategory = (c: Pick<CategoryNode, 'productCount' | 'childCount'>): boolean =>
  c.productCount === 0 && c.childCount === 0;

/** Mọi ngành của cây (gốc → cấp 2 → cấp 3) kèm vị trí. */
export function flattenTree(roots: RootCategory[]): CategoryNode[] {
  return roots.flatMap((r) => {
    const { children: subs, shopCount: _s, ...root } = r;
    return [
      { ...root, level: 1 as const, parentId: null, rootId: r.id, childCount: subs.length },
      ...subs.flatMap(({ children: leaves, ...s }) => [
        { ...s, level: 2 as const, parentId: r.id, rootId: r.id, childCount: leaves.length },
        ...leaves.map((l) => ({
          ...l,
          level: 3 as const,
          parentId: s.id,
          rootId: r.id,
          childCount: 0,
        })),
      ]),
    ];
  });
}

/** Tìm một ngành ở bất kỳ cấp nào (trang Bộ thuộc tính, form Sửa ngành con). */
export const findCategory = (roots: RootCategory[], id: number): CategoryNode | null =>
  flattenTree(roots).find((c) => c.id === id) ?? null;

/** Danh mục cha chọn được: gốc + cấp 2 (cây tối đa 3 cấp). */
export function parentOptions(
  roots: RootCategory[],
): { id: number; label: string; depth: 1 | 2 }[] {
  return roots.flatMap((r) => [
    { id: r.id, label: r.name, depth: 1 as const },
    ...r.children.map((s) => ({ id: s.id, label: `${r.name} › ${s.name}`, depth: 2 as const })),
  ]);
}

export function inProductTab(p: Pick<CatalogProduct, 'status'>, tab: CatalogProductTab): boolean {
  switch (tab) {
    case 'ALL':
      return true;
    case 'PENDING':
      return p.status === 'PENDING_APPROVAL';
    case 'PUBLISHED':
      return p.status === 'PUBLISHED';
    case 'HIDDEN_OR_LOCKED':
      return p.status === 'HIDDEN' || p.status === 'REJECTED';
  }
}

/** Thao tác trên sản phẩm của danh mục con (prototype ProductList). */
export function productActions(
  p: Pick<CatalogProduct, 'status' | 'locked'>,
): ('approve' | 'reject' | 'lock' | 'unlock')[] {
  if (p.status === 'PENDING_APPROVAL') return ['reject', 'approve'];
  if (p.status === 'PUBLISHED') return ['lock'];
  if (p.locked) return ['unlock'];
  return [];
}

/** Trùng mã thuộc tính trong cùng ngành (bỏ qua chính dòng đang sửa). */
export const duplicateCode = (
  rows: Pick<CategoryAttribute, 'code'>[],
  code: string,
  editing: string | null,
): boolean => rows.some((r) => r.code === code && r.code !== editing);
