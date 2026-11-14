// Duyệt sản phẩm (API_SPEC 11.12). Mock: hàng / trạng thái ở kho dùng chung adminModerationStore
// (huy hiệu nav + "Việc cần xử lý" đổi theo), chi tiết ở moderation.mock.ts.
import { api, getPage } from '@/shared/api/http';
import { mockDelay, mockFail, paginatePage, USE_MOCK } from '@/shared/api/mock';
import { adminModerationStore, type MockModerationRow } from '@/shared/api/mock/admin';
import type { Page } from '@/shared/api/types';
import {
  inModerationTab,
  matchesQuery,
  MODERATION_TABS,
  redistribute,
  sortQueue,
  type ProductRejectReason,
} from '../domain/moderation.rules';
import type {
  ModerationCounts,
  ModerationDetail,
  ModerationRow,
  ModerationTab,
  Moderator,
} from '../domain/moderation.types';
import type { ModerationDetailDto, ModerationRowDto, ModeratorDto } from './moderation.dto';
import { MODERATION_EXTRA, moderatorsStore } from './moderation.mock';

const toRow = (d: ModerationRowDto): ModerationRow => ({
  productId: d.product_id,
  name: d.name,
  imageUrl: d.image_url,
  shop: d.shop,
  status: d.status,
  requeued: d.is_requeued,
  assignee: d.assignee,
  rejectedByAi: d.rejected_by_ai,
  submittedAt: d.submitted_at,
});

const toDetail = (d: ModerationDetailDto): ModerationDetail => ({
  ...toRow(d),
  ai: d.ai_result
    ? {
        decision: d.ai_result.decision,
        riskScore: d.ai_result.risk_score,
        violations: d.ai_result.violations,
        reasoning: d.ai_result.reasoning,
      }
    : null,
  decisionReason: d.decision_reason,
  images: d.images,
  documents: d.documents.map((x) => ({ id: x.id, fileName: x.file_name, url: x.url })),
  categoryPath: d.category_path,
  brand: d.brand,
  condition: d.condition,
  origin: d.origin,
  descriptionHtml: d.description_html,
  weightGrams: d.weight_grams,
  dimensionsCm: d.dimensions_cm,
  shipsFrom: d.ships_from,
  shippingPayer: d.shipping_payer,
  variants: d.variants,
});

const toModerator = (d: ModeratorDto): Moderator => ({
  staffId: d.staff_id,
  name: d.name,
  accepting: d.is_accepting,
});

export interface ModerationPage extends Page<ModerationRow> {
  counts: ModerationCounts;
}

const SIZE = 20;

export async function listModeration(
  tab: ModerationTab,
  q = '',
  page = 1,
): Promise<ModerationPage> {
  if (USE_MOCK) {
    await mockDelay(180);
    const all = adminModerationStore.get().map(toRow);
    const found = all.filter((r) => matchesQuery(r, q));
    const counts = Object.fromEntries(
      MODERATION_TABS.map((t) => [t, all.filter((r) => inModerationTab(r.status, t)).length]),
    ) as ModerationCounts;
    const p = paginatePage(sortQueue(found.filter((r) => inModerationTab(r.status, tab))), {
      page,
      size: SIZE,
    });
    return { ...p, counts };
  }
  const res = await getPage<ModerationRowDto, ModerationRow>('/admin/product-moderation', toRow, {
    query: { tab, q: q.trim() || undefined, page, size: SIZE },
  });
  const c = (res.summary.counts ?? {}) as Partial<ModerationCounts>;
  return {
    ...res,
    counts: { PENDING: c.PENDING ?? 0, PUBLISHED: c.PUBLISHED ?? 0, REJECTED: c.REJECTED ?? 0 },
  };
}

function mockDetail(id: number): ModerationDetailDto {
  const row = adminModerationStore.get().find((r) => r.product_id === id);
  const extra = MODERATION_EXTRA[id];
  if (!row || !extra) mockFail('GNE-CAT-4041');
  return { ...row, ...extra };
}

export async function getModeration(id: number): Promise<ModerationDetail> {
  if (USE_MOCK) {
    await mockDelay(150);
    return toDetail(mockDetail(id));
  }
  return toDetail(await api.get<ModerationDetailDto>(`/admin/product-moderation/${id}`));
}

function patchRow(id: number, fn: (r: MockModerationRow) => MockModerationRow): void {
  adminModerationStore.set(
    adminModerationStore.get().map((r) => (r.product_id === id ? fn(r) : r)),
  );
}

/** Người duyệt mặc định khi quyết định một sản phẩm chưa phân công: người đang đăng nhập (mock: nhân viên đầu). */
const withAssignee = (r: MockModerationRow): MockModerationRow => {
  if (r.assignee) return r;
  const m = moderatorsStore.get()[0];
  return m ? { ...r, assignee: { id: m.staff_id, name: m.name } } : r;
};

export async function approveProduct(id: number): Promise<void> {
  if (USE_MOCK) {
    await mockDelay(250);
    mockDetail(id);
    patchRow(id, (r) =>
      withAssignee({
        ...r,
        status: 'PUBLISHED',
        is_requeued: false,
        decision_reason: null,
        rejected_by_ai: false,
      }),
    );
    return;
  }
  await api.post(`/admin/products/${id}/approve`);
}

export async function rejectProduct(
  id: number,
  reason: ProductRejectReason,
  note: string | null,
  /** Nhãn lý do đã dịch — mock lưu làm lý do hiển thị (server tự dịch theo mã). */
  label: string,
): Promise<void> {
  if (USE_MOCK) {
    await mockDelay(250);
    mockDetail(id);
    patchRow(id, (r) =>
      withAssignee({
        ...r,
        status: 'REJECTED',
        is_requeued: false,
        rejected_by_ai: false,
        decision_reason: note ? `${label}: ${note}` : label,
      }),
    );
    return;
  }
  await api.post(`/admin/products/${id}/reject`, { reason_code: reason, note });
}

export async function requeueProduct(id: number): Promise<void> {
  if (USE_MOCK) {
    await mockDelay(200);
    patchRow(id, (r) => ({ ...r, status: 'PENDING_APPROVAL', is_requeued: true }));
    return;
  }
  await api.post(`/admin/products/${id}/requeue`);
}

export async function assignModerator(id: number, staffId: number): Promise<void> {
  if (USE_MOCK) {
    await mockDelay(150);
    const m = moderatorsStore.get().find((x) => x.staff_id === staffId);
    if (!m) mockFail('GNE-SYS-4040', { status: 404 });
    patchRow(id, (r) => ({ ...r, assignee: { id: m.staff_id, name: m.name } }));
    return;
  }
  await api.put(`/admin/products/${id}/assignee`, { staff_id: staffId });
}

export async function listModerators(): Promise<Moderator[]> {
  if (USE_MOCK) {
    await mockDelay(120);
    return moderatorsStore.get().map(toModerator);
  }
  return (await api.get<ModeratorDto[]>('/admin/moderators')).map(toModerator);
}

export async function saveModeratorAvailability(items: Moderator[]): Promise<void> {
  if (USE_MOCK) {
    await mockDelay(200);
    moderatorsStore.set(
      moderatorsStore.get().map((m) => {
        const hit = items.find((x) => x.staffId === m.staff_id);
        return hit ? { ...m, is_accepting: hit.accepting } : m;
      }),
    );
    return;
  }
  await api.put('/admin/moderators/availability', {
    items: items.map((m) => ({ staff_id: m.staffId, is_accepting: m.accepting })),
  });
}

export async function redistributeQueue(): Promise<number> {
  if (USE_MOCK) {
    await mockDelay(300);
    const staff = moderatorsStore.get().map(toModerator);
    const res = redistribute(adminModerationStore.get(), staff);
    adminModerationStore.set(res.rows);
    return res.reassigned;
  }
  return (await api.post<{ reassigned_count: number }>('/admin/moderators/redistribute'))
    .reassigned_count;
}
