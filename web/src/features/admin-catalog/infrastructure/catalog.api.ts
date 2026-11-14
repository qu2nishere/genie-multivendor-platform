// Danh mục toàn sàn (API_SPEC 11.11): cây danh mục, thêm / sửa / xóa, bộ thuộc tính, đề xuất của shop,
// sản phẩm theo danh mục (duyệt / từ chối dùng endpoint 11.12, khóa / mở khóa). Mock ghi các kho ở
// catalog.mock.ts và báo lỗi như server (trùng slug, còn sản phẩm, trùng mã thuộc tính).
import { api, getPage, request } from '@/shared/api/http';
import { mockDelay, mockFail, paginatePage, USE_MOCK } from '@/shared/api/mock';
import type { Page } from '@/shared/api/types';
import { slugify } from '@/shared/lib/slug';
import { inProductTab, type LockReason } from '../domain/catalog.rules';
import type {
  CatalogProduct,
  CatalogProductTab,
  CategoryAttribute,
  CategoryInput,
  CategoryProposal,
  CategoryTree,
} from '../domain/catalog.types';
import type {
  AttributeDto,
  CatalogProductDto,
  CatalogSummaryDto,
  CategoryDto,
  ProposalDto,
} from './catalog.dto';
import {
  toAttribute,
  toAttributeDto,
  toCatalogProduct,
  toCategoryInputDto,
  toProposal,
  toRoot,
  toSummary,
} from './catalog.mapper';
import {
  attributesStore,
  catalogProductsStore,
  categoriesStore,
  proposalsStore,
} from './catalog.mock';

// ---------- Cây danh mục ----------
export async function getCategoryTree(): Promise<CategoryTree> {
  if (USE_MOCK) {
    await mockDelay(180);
    const roots = categoriesStore.get();
    return {
      roots: roots.map(toRoot),
      summary: toSummary({
        root_count: roots.length,
        child_count: roots.reduce((n, r) => n + r.children.length, 0),
        product_count: roots.reduce((n, r) => n + r.product_count, 0),
        pending_proposals: proposalsStore.get().length,
      }),
    };
  }
  const res = await request<CategoryDto[]>('/admin/categories');
  return {
    roots: res.data.map(toRoot),
    summary: toSummary(res.summary as unknown as CatalogSummaryDto),
  };
}

const nextId = (roots: CategoryDto[]) =>
  Math.max(
    100,
    ...roots.flatMap((r) => [
      r.id,
      ...r.children.flatMap((s) => [s.id, ...s.children.map((l) => l.id)]),
    ]),
  ) + 1;

function mockAdd(c: CategoryInput): void {
  const roots = categoriesStore.get();
  const id = nextId(roots);
  if (c.parentId === null) {
    if (roots.some((r) => r.slug === c.slug)) mockFail('GNE-CAT-4091');
    categoriesStore.set([
      ...roots,
      {
        id,
        name: c.name,
        slug: c.slug,
        icon: c.icon,
        is_active: c.active,
        commission_rate: c.commissionRate,
        product_count: 0,
        shop_count: 0,
        children: [],
      },
    ]);
    attributesStore.set({ ...attributesStore.get(), [id]: [] });
    return;
  }
  const parentId = c.parentId;
  const isRoot = roots.some((r) => r.id === parentId);
  categoriesStore.set(
    roots.map((r) =>
      isRoot
        ? r.id === parentId
          ? {
              ...r,
              children: [
                ...r.children,
                {
                  id,
                  name: c.name,
                  commission_rate: c.commissionRate,
                  product_count: 0,
                  children: [],
                },
              ],
            }
          : r
        : {
            ...r,
            children: r.children.map((s) =>
              s.id === parentId ? { ...s, children: [...s.children, { id, name: c.name }] } : s,
            ),
          },
    ),
  );
}

export async function createCategory(c: CategoryInput): Promise<void> {
  if (USE_MOCK) {
    await mockDelay(250);
    mockAdd(c);
    return;
  }
  await api.post('/admin/categories', toCategoryInputDto(c));
}

/** Sửa danh mục gốc (tên, slug, icon, hoa hồng, hiển thị) — công tắc "Hiển thị" gửi riêng is_active. */
export async function updateCategory(id: number, patch: Partial<CategoryInput>): Promise<void> {
  if (USE_MOCK) {
    await mockDelay(200);
    const roots = categoriesStore.get();
    if (patch.slug && roots.some((r) => r.id !== id && r.slug === patch.slug))
      mockFail('GNE-CAT-4091');
    categoriesStore.set(
      roots.map((r) =>
        r.id === id
          ? {
              ...r,
              name: patch.name ?? r.name,
              slug: patch.slug ?? r.slug,
              icon: patch.icon ?? r.icon,
              commission_rate: patch.commissionRate ?? r.commission_rate,
              is_active: patch.active ?? r.is_active,
            }
          : r,
      ),
    );
    return;
  }
  const body: Record<string, unknown> = {};
  if (patch.name !== undefined) body.name = patch.name;
  if (patch.slug !== undefined) body.slug = patch.slug;
  if (patch.icon !== undefined) body.icon = patch.icon;
  if (patch.commissionRate !== undefined) body.commission_rate = patch.commissionRate;
  if (patch.active !== undefined) body.is_active = patch.active;
  await api.patch(`/admin/categories/${id}`, body);
}

export async function deleteCategory(id: number): Promise<void> {
  if (USE_MOCK) {
    await mockDelay(200);
    const roots = categoriesStore.get();
    const r = roots.find((x) => x.id === id);
    if (!r) mockFail('GNE-CAT-4041');
    if (r.product_count > 0 || r.children.length > 0) mockFail('GNE-CAT-4092');
    categoriesStore.set(roots.filter((x) => x.id !== id));
    return;
  }
  await api.delete(`/admin/categories/${id}`);
}

// ---------- Bộ thuộc tính ----------
export async function listAttributes(categoryId: number): Promise<CategoryAttribute[]> {
  if (USE_MOCK) {
    await mockDelay(150);
    return (attributesStore.get()[categoryId] ?? []).map(toAttribute);
  }
  return (await api.get<AttributeDto[]>(`/admin/categories/${categoryId}/attributes`)).map(
    toAttribute,
  );
}

/** Lưu (thêm hoặc sửa); trả số ngày ân hạn khi thêm thuộc tính bắt buộc (shop bổ sung cho SP cũ). */
export async function saveAttribute(
  categoryId: number,
  a: CategoryAttribute,
  editing: string | null,
): Promise<{ gracePeriodDays: number | null }> {
  if (USE_MOCK) {
    await mockDelay(220);
    const all = attributesStore.get();
    const rows = all[categoryId] ?? [];
    if (rows.some((r) => r.code === a.code && r.code !== editing)) mockFail('GNE-CAT-4095');
    const prev = rows.find((r) => r.code === editing);
    const dto = toAttributeDto({ ...a, active: prev?.is_active ?? true });
    attributesStore.set({
      ...all,
      [categoryId]: editing ? rows.map((r) => (r.code === editing ? dto : r)) : [...rows, dto],
    });
    const becameRequired = a.required && !(prev?.is_required ?? false);
    return { gracePeriodDays: becameRequired ? 14 : null };
  }
  const { code, ...rest } = toAttributeDto(a);
  const { is_active: _active, ...body } = rest;
  const res = editing
    ? await api.put<{ grace_period_days?: number }>(
        `/admin/categories/${categoryId}/attributes/${encodeURIComponent(editing)}`,
        body,
      )
    : await api.post<{ grace_period_days?: number }>(`/admin/categories/${categoryId}/attributes`, {
        code,
        ...body,
      });
  return { gracePeriodDays: res?.grace_period_days ?? null };
}

export async function setAttributeActive(
  categoryId: number,
  code: string,
  active: boolean,
): Promise<void> {
  if (USE_MOCK) {
    await mockDelay(150);
    const all = attributesStore.get();
    attributesStore.set({
      ...all,
      [categoryId]: (all[categoryId] ?? []).map((r) =>
        r.code === code ? { ...r, is_active: active } : r,
      ),
    });
    return;
  }
  await api.patch(`/admin/categories/${categoryId}/attributes/${encodeURIComponent(code)}`, {
    is_active: active,
  });
}

// ---------- Đề xuất danh mục ----------
export async function listProposals(): Promise<CategoryProposal[]> {
  if (USE_MOCK) {
    await mockDelay(150);
    return proposalsStore.get().map(toProposal);
  }
  return (
    await api.get<ProposalDto[]>('/admin/category-proposals', { query: { status: 'PENDING' } })
  ).map(toProposal);
}

export async function approveProposal(id: number, commissionRate: number | null): Promise<void> {
  if (USE_MOCK) {
    await mockDelay(250);
    const p = proposalsStore.get().find((x) => x.id === id);
    if (!p) mockFail('GNE-SYS-4040', { status: 404 });
    const parent = categoriesStore.get().find((r) => r.id === p.parent?.id);
    mockAdd({
      name: p.name,
      slug: slugify(p.name),
      parentId: parent ? parent.id : null,
      icon: 'package',
      commissionRate: commissionRate ?? parent?.commission_rate ?? 5,
      active: true,
    });
    proposalsStore.set(proposalsStore.get().filter((x) => x.id !== id));
    return;
  }
  await api.post(`/admin/category-proposals/${id}/approve`, { commission_rate: commissionRate });
}

export async function rejectProposal(id: number, reason: string): Promise<void> {
  if (USE_MOCK) {
    await mockDelay(200);
    proposalsStore.set(proposalsStore.get().filter((x) => x.id !== id));
    return;
  }
  await api.post(`/admin/category-proposals/${id}/reject`, { reason });
}

// ---------- Sản phẩm theo danh mục ----------
export interface CatalogProductPage extends Page<CatalogProduct> {
  pendingCount: number;
}

export async function listCatalogProducts(
  categoryId: number,
  tab: CatalogProductTab,
  page = 1,
): Promise<CatalogProductPage> {
  if (USE_MOCK) {
    await mockDelay(180);
    const all = catalogProductsStore.get().map(toCatalogProduct);
    const p = paginatePage(
      all.filter((x) => inProductTab(x, tab)),
      { page, size: 20 },
    );
    return { ...p, pendingCount: all.filter((x) => inProductTab(x, 'PENDING')).length };
  }
  const res = await getPage<CatalogProductDto, CatalogProduct>(
    '/admin/products',
    toCatalogProduct,
    {
      query: { category_id: categoryId, tab, page, size: 20 },
    },
  );
  const counts = (res.summary.counts ?? {}) as Partial<Record<CatalogProductTab, number>>;
  return { ...res, pendingCount: counts.PENDING ?? 0 };
}

function patchProduct(id: number, fn: (p: CatalogProductDto) => CatalogProductDto): void {
  catalogProductsStore.set(catalogProductsStore.get().map((p) => (p.id === id ? fn(p) : p)));
}

export type ProductCommand =
  | { kind: 'approve'; id: number }
  | { kind: 'reject'; id: number; reason: LockReason; label: string }
  | { kind: 'lock'; id: number; reason: LockReason; label: string }
  | { kind: 'unlock'; id: number };

export async function runProductCommand(c: ProductCommand): Promise<void> {
  if (USE_MOCK) {
    await mockDelay(220);
    patchProduct(c.id, (p) => {
      switch (c.kind) {
        case 'approve':
          return { ...p, status: 'PUBLISHED', reason: null };
        case 'reject':
          return { ...p, status: 'REJECTED', reason: c.label };
        case 'lock':
          return { ...p, status: 'HIDDEN', is_locked: true, reason: c.label };
        case 'unlock':
          return { ...p, status: 'PUBLISHED', is_locked: false, reason: null };
      }
    });
    return;
  }
  switch (c.kind) {
    case 'approve':
      await api.post(`/admin/products/${c.id}/approve`);
      return;
    case 'reject':
      await api.post(`/admin/products/${c.id}/reject`, { reason_code: c.reason, note: null });
      return;
    case 'lock':
      await api.post(`/admin/products/${c.id}/lock`, { reason_code: c.reason });
      return;
    case 'unlock':
      await api.post(`/admin/products/${c.id}/unlock`);
  }
}
