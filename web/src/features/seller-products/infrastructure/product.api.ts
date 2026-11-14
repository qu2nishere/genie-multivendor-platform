// Sản phẩm của shop (API_SPEC 10.5), danh mục toàn sàn + đề xuất (10.6), thương hiệu. Mock ghi kho dùng
// chung `sellerProductsStore` (Tổng quan, tìm nhanh đọc) + kho chi tiết form.
import { api, request } from '@/shared/api/http';
import { mockDelay, mockFail, paginatePage, USE_MOCK } from '@/shared/api/mock';
import {
  sellerOrdersStore,
  sellerProductsStore,
  sellerStore,
  type MockSellerProduct,
} from '@/shared/api/mock/seller';
import { missingSpecs, requiresReview } from '../domain/product.rules';
import type {
  Brand,
  CatalogNode,
  CategoryNode,
  CategoryProposal,
  ListingRules,
  ProductDetail,
  ProductForm,
  ProductStatus,
  ProductTab,
  SaveResult,
  SellerProductRow,
} from '../domain/product.types';
import type {
  CatalogNodeDto,
  ListingRulesDto,
  ProductDetailDto,
  ProductRowDto,
  ProposalDto,
} from './product.dto';
import { toCatalog, toDetail, toInput, toProposal, toRow, toRules } from './product.mapper';
import {
  brandsStore,
  CATALOG,
  CATEGORY_TREE,
  listingRules,
  productDetailsStore,
  proposalsStore,
} from './product.mock';

const TABS: ProductTab[] = ['ALL', 'PUBLISHED', 'DRAFT', 'PENDING_APPROVAL', 'REJECTED', 'HIDDEN'];

export interface ProductPage {
  items: SellerProductRow[];
  page: number;
  totalPages: number;
  counts: Record<ProductTab, number>;
}

const rowDto = (p: MockSellerProduct): ProductRowDto => ({
  id: p.id,
  name: p.name,
  image_url: p.image_url,
  variant_count: p.sku_count,
  price_min: p.price,
  stock_total: p.stock,
  sold_count: p.sold,
  status: p.status,
  reject_reason: p.reject_reason,
});

export async function listSellerProducts(q: {
  tab: ProductTab;
  q?: string;
  page?: number;
  size?: number;
}): Promise<ProductPage> {
  if (USE_MOCK) {
    await mockDelay(200);
    const needle = q.q?.trim().toLowerCase() ?? '';
    const all = sellerProductsStore
      .get()
      .filter((p) => !needle || p.name.toLowerCase().includes(needle));
    const counts = Object.fromEntries(
      TABS.map((t) => [t, all.filter((p) => t === 'ALL' || p.status === t).length]),
    ) as Record<ProductTab, number>;
    const pg = paginatePage(
      all.filter((p) => q.tab === 'ALL' || p.status === q.tab),
      { page: q.page ?? 1, size: q.size ?? 20 },
    );
    return {
      items: pg.items.map(rowDto).map(toRow),
      page: pg.page,
      totalPages: pg.total_pages,
      counts,
    };
  }
  const res = await request<ProductRowDto[]>('/seller/products', {
    query: {
      status: q.tab === 'ALL' ? undefined : q.tab,
      q: q.q?.trim() || undefined,
      page: q.page,
      size: q.size,
    },
  });
  const p = res.pagination && 'page' in res.pagination ? res.pagination : null;
  const c = (res.summary?.counts ?? {}) as Partial<Record<ProductTab, number>>;
  return {
    items: res.data.map(toRow),
    page: p?.page ?? 1,
    totalPages: p?.total_pages ?? 1,
    counts: Object.fromEntries(TABS.map((t) => [t, c[t] ?? 0])) as Record<ProductTab, number>,
  };
}

export async function getSellerProduct(id: number): Promise<ProductDetail> {
  if (USE_MOCK) {
    await mockDelay(200);
    const d = productDetailsStore.get()[id];
    if (!d) mockFail('GNE-CAT-4042');
    return toDetail(d);
  }
  return toDetail(await api.get<ProductDetailDto>(`/seller/products/${id}`));
}

function mockSave(id: number | null, f: ProductForm, submit: boolean): SaveResult {
  const shop = sellerStore.get();
  if (submit && shop.kyc_status !== 'VERIFIED') mockFail('GNE-VND-4031');
  const leaf = f.categoryPath.at(-1)?.id;
  if (submit) {
    const rules = leaf ? toRules(listingRules(leaf)) : null;
    if (rules?.requiresDocuments && f.documents.length === 0)
      mockFail('GNE-CAT-4222', { details: { field: 'document_object_keys' } });
    const miss = missingSpecs(f.specs, rules);
    if (miss.length) mockFail('GNE-CAT-4223', { details: { missing_specs: miss.join(', ') } });
  }
  const details = productDetailsStore.get();
  const prev = id ? details[id] : undefined;
  if (id && !prev) mockFail('GNE-CAT-4042');
  const before = prev ? toDetail(prev) : null;
  const review = !!before && requiresReview(before.status, before, f);
  const status: ProductStatus = !submit
    ? before?.status === 'PUBLISHED'
      ? 'PUBLISHED'
      : 'DRAFT'
    : before?.status === 'PUBLISHED' && !review
      ? 'PUBLISHED'
      : 'PENDING_APPROVAL';
  const newId = id ?? Math.max(0, ...Object.keys(details).map(Number)) + 1;
  const input = toInput(f, submit);
  const dto: ProductDetailDto = {
    id: newId,
    status,
    reject_reason: null,
    images: f.images.map((i) => ({ object_key: i.objectKey, url: i.url })),
    video: f.video && { object_key: f.video.objectKey, file_name: f.video.name },
    documents: f.documents.map((x, i) => ({
      id: i + 1,
      file_name: x.name,
      object_key: x.objectKey,
      url: '',
    })),
    name: input.name,
    category_path: f.categoryPath,
    brand: f.brand,
    condition: f.condition,
    origin: input.origin,
    specs: input.specs,
    description_html: f.descriptionHtml,
    option_groups: f.groups.map((g) => ({
      name: g.name,
      values: g.values.map((v) => ({
        value: v.value,
        image_object_key: v.imageKey,
        image_url: v.imageUrl,
      })),
    })),
    variant_image_group: f.variantImageGroup,
    variants: f.variants.map((v, i) => ({
      id: v.id ?? newId * 100 + i + 1,
      options: v.options,
      sku: v.sku ?? `SP-${newId}-${i + 1}`,
      price: v.price ?? 0,
      stock: v.stock ?? 0,
    })),
    weight_grams: f.weightGrams,
    dimensions_cm: f.dims,
    shipping_payer: f.shippingPayer,
    free_shipping_threshold: input.free_shipping_threshold,
    is_pre_order: f.isPreOrder,
    pre_order_days: input.pre_order_days,
  };
  productDetailsStore.set({ ...details, [newId]: dto });
  const prices = dto.variants.map((v) => v.price).filter((p) => p > 0);
  const row: MockSellerProduct = {
    id: newId,
    name: dto.name || 'Sản phẩm chưa đặt tên',
    image_url: dto.images[0]?.url ?? '/img/products/backpack.jpg',
    status,
    price: prices.length ? Math.min(...prices) : 0,
    stock: dto.variants.reduce((n, v) => n + v.stock, 0),
    sold: sellerProductsStore.get().find((p) => p.id === newId)?.sold ?? 0,
    sku_count: dto.variants.length,
    reject_reason: null,
  };
  const rows = sellerProductsStore.get();
  sellerProductsStore.set(
    rows.some((p) => p.id === newId) ? rows.map((p) => (p.id === newId ? row : p)) : [row, ...rows],
  );
  return { id: newId, status, requiresReview: review };
}

/** `submit = false` lưu nháp; `true` gửi duyệt (Gemini sàng lọc → hàng chờ Moderator). */
export async function saveProduct(
  id: number | null,
  f: ProductForm,
  submit: boolean,
): Promise<SaveResult> {
  if (USE_MOCK) {
    await mockDelay(450);
    return mockSave(id, f, submit);
  }
  const body = toInput(f, submit);
  const d = id
    ? await api.put<{ id: number; status: ProductStatus; requires_review: boolean }>(
        `/seller/products/${id}`,
        body,
      )
    : await api.post<{ id: number; status: ProductStatus; requires_review: boolean }>(
        '/seller/products',
        body,
      );
  return { id: d.id, status: d.status, requiresReview: d.requires_review };
}

export async function setProductHidden(id: number, hidden: boolean): Promise<void> {
  if (USE_MOCK) {
    await mockDelay(200);
    const status: ProductStatus = hidden ? 'HIDDEN' : 'PUBLISHED';
    sellerProductsStore.set(
      sellerProductsStore.get().map((p) => (p.id === id ? { ...p, status } : p)),
    );
    const d = productDetailsStore.get();
    if (d[id]) productDetailsStore.set({ ...d, [id]: { ...d[id], status } });
    return;
  }
  await api.patch(`/seller/products/${id}`, { status: hidden ? 'HIDDEN' : 'PUBLISHED' });
}

export async function deleteProduct(id: number): Promise<void> {
  if (USE_MOCK) {
    await mockDelay(250);
    const p = sellerProductsStore.get().find((x) => x.id === id);
    // Còn đơn đang xử lý chứa sản phẩm → không xóa được (409 GNE-CAT-4094). Đơn mẫu chỉ có tên rút gọn:
    // coi là cùng sản phẩm khi mọi từ của tên trên đơn đều có trong tên sản phẩm.
    const words = (s: string) => s.toLowerCase().split(/\s+/).filter(Boolean);
    const name = new Set(words(p?.name ?? ''));
    const open = sellerOrdersStore
      .get()
      .some(
        (o) =>
          ['NEW', 'CONFIRMED', 'PREPARING', 'DELIVERING'].includes(o.status) &&
          o.items.some((i) => words(i.product_name).every((w) => name.has(w))),
      );
    if (open) mockFail('GNE-CAT-4094');
    sellerProductsStore.set(sellerProductsStore.get().filter((x) => x.id !== id));
    const { [id]: _gone, ...rest } = productDetailsStore.get();
    productDetailsStore.set(rest);
    return;
  }
  await api.delete(`/seller/products/${id}`);
}

const toNode = (d: {
  id: number;
  name: string;
  children?: { id: number; name: string }[];
}): CategoryNode => ({
  id: d.id,
  name: d.name,
  children: d.children?.map(toNode),
});

/** Cây 3 cấp cho ô chọn danh mục (5.1 GET /public/categories). */
export async function getCategoryTree(): Promise<CategoryNode[]> {
  if (USE_MOCK) {
    await mockDelay(120);
    return CATEGORY_TREE;
  }
  return (await api.get<CategoryNode[]>('/public/categories')).map(toNode);
}

export async function getListingRules(categoryId: number): Promise<ListingRules> {
  if (USE_MOCK) {
    await mockDelay(120);
    return toRules(listingRules(categoryId));
  }
  return toRules(await api.get<ListingRulesDto>(`/seller/categories/${categoryId}/listing-rules`));
}

export async function searchBrands(q: string): Promise<Brand[]> {
  if (USE_MOCK) {
    await mockDelay(80);
    const n = q.trim().toLowerCase();
    return brandsStore
      .get()
      .filter((b) => b.name.toLowerCase().includes(n))
      .slice(0, 10);
  }
  return api.get<Brand[]>('/seller/brands', { query: { q: q.trim(), limit: 10 } });
}

/** Thêm thương hiệu (chưa xác minh); trùng tên trả bản có sẵn. */
export async function createBrand(name: string): Promise<Brand> {
  if (USE_MOCK) {
    await mockDelay(150);
    const all = brandsStore.get();
    const hit = all.find((b) => b.name.toLowerCase() === name.trim().toLowerCase());
    if (hit) return hit;
    const b = { id: Math.max(...all.map((x) => x.id)) + 1, name: name.trim() };
    brandsStore.set([...all, b]);
    return b;
  }
  return api.post<Brand>('/seller/brands', { name: name.trim() });
}

export async function listCatalog(): Promise<CatalogNode[]> {
  if (USE_MOCK) {
    await mockDelay(150);
    return CATALOG.map(toCatalog);
  }
  return (await api.get<CatalogNodeDto[]>('/seller/categories')).map(toCatalog);
}

export async function listProposals(): Promise<CategoryProposal[]> {
  if (USE_MOCK) {
    await mockDelay(150);
    return proposalsStore.get().map(toProposal);
  }
  return (await api.get<ProposalDto[]>('/seller/category-proposals')).map(toProposal);
}

export async function createProposal(v: {
  name: string;
  parentId: number | null;
  reason: string;
}): Promise<CategoryProposal> {
  if (USE_MOCK) {
    await mockDelay(300);
    const names = CATALOG.flatMap((c) => [c, ...(c.children ?? [])]);
    const dup = names.find((n) => n.name.toLowerCase() === v.name.trim().toLowerCase());
    if (dup) mockFail('GNE-CAT-4091', { details: { existing_category: dup.name } });
    const all = proposalsStore.get();
    if (all.filter((p) => p.status === 'PENDING').length >= 5) mockFail('GNE-CAT-4221');
    const parent = v.parentId ? CATALOG.find((c) => c.id === v.parentId) : null;
    const id = Math.max(...all.map((p) => p.id)) + 1;
    const p: ProposalDto = {
      id,
      code: `CP-${String(id).padStart(3, '0')}`,
      name: v.name.trim(),
      parent: parent ? { id: parent.id, name: parent.name } : null,
      status: 'PENDING',
      reject_reason: null,
      created_at: new Date().toISOString(),
    };
    proposalsStore.set([p, ...all]);
    return toProposal(p);
  }
  return toProposal(
    await api.post<ProposalDto>('/seller/category-proposals', {
      name: v.name.trim(),
      parent_id: v.parentId,
      reason: v.reason.trim(),
    }),
  );
}
