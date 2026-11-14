// Duyệt KYC (API_SPEC 11.3). Mock: trạng thái ở kho dùng chung adminKycStore (huy hiệu nav + Tổng quan
// đổi theo), chi tiết ở kycDetailStore.
import { api, getPage } from '@/shared/api/http';
import { mockDelay, mockFail, paginatePage, USE_MOCK } from '@/shared/api/mock';
import { adminKycStore, type MockAdminKycStatus } from '@/shared/api/mock/admin';
import type { Page } from '@/shared/api/types';
import { inTab, isWaiting, KYC_TABS, SUPPLEMENT_DAYS } from '../domain/kyc.rules';
import type { KycRejectReason } from '../domain/kyc.rules';
import type { KycCounts, KycDetail, KycDocType, KycSubmission, KycTab } from '../domain/kyc.types';
import type { KycCountsDto, KycDetailDto, KycSubmissionDto } from './kyc.dto';
import { toKycCounts, toKycDetail, toKycSubmission } from './kyc.mapper';
import { kycDetailStore } from './kyc.mock';

export interface KycPage extends Page<KycSubmission> {
  counts: KycCounts;
}

const PAGE_SIZE = 20;

export async function listKycSubmissions(tab: KycTab, page = 1): Promise<KycPage> {
  if (USE_MOCK) {
    await mockDelay(180);
    const all = adminKycStore.get();
    const counts = Object.fromEntries(
      KYC_TABS.map((t) => [t, all.filter((k) => inTab(k.status, t)).length]),
    ) as KycCounts;
    const p = paginatePage(
      all.filter((k) => inTab(k.status, tab)),
      { page, size: PAGE_SIZE },
    );
    return { ...p, items: p.items.map(toKycSubmission), counts };
  }
  const res = await getPage<KycSubmissionDto, KycSubmission>(
    '/admin/kyc-submissions',
    toKycSubmission,
    { query: { status: tab === 'ALL' ? undefined : tab, page, size: PAGE_SIZE } },
  );
  return { ...res, counts: toKycCounts((res.summary.counts ?? {}) as Partial<KycCountsDto>) };
}

function mockDetail(id: number): KycDetailDto {
  const row = adminKycStore.get().find((k) => k.id === id);
  const extra = kycDetailStore.get()[id];
  if (!row || !extra) mockFail('GNE-SYS-4040', { status: 404 });
  return { ...row, ...extra };
}

export async function getKycSubmission(id: number): Promise<KycDetail> {
  if (USE_MOCK) {
    await mockDelay(150);
    return toKycDetail(mockDetail(id));
  }
  return toKycDetail(await api.get<KycDetailDto>(`/admin/kyc-submissions/${id}`));
}

/** Mock: đổi trạng thái + phần chi tiết; chỉ hồ sơ đang chờ mới được quyết định (như server). */
function mockDecide(
  id: number,
  status: MockAdminKycStatus,
  patch: Partial<KycDetailDto>,
  needWaiting: boolean,
): KycDetail {
  const cur = mockDetail(id);
  if (needWaiting && !isWaiting(cur.status)) mockFail('GNE-SYS-4090', { status: 409 });
  adminKycStore.set(adminKycStore.get().map((k) => (k.id === id ? { ...k, status } : k)));
  const details = kycDetailStore.get();
  const {
    id: _i,
    code: _c,
    shop: _s,
    owner_name: _o,
    business_type: _b,
    status: _st,
    submitted_at: _a,
    ...rest
  } = {
    ...cur,
    ...patch,
  };
  kycDetailStore.set({ ...details, [id]: rest });
  return toKycDetail(mockDetail(id));
}

export async function approveKyc(
  id: number,
  shopCommissionRate: number | null,
): Promise<KycDetail> {
  if (USE_MOCK) {
    await mockDelay(250);
    return mockDecide(
      id,
      'VERIFIED',
      { shop_commission_rate: shopCommissionRate, rejection_reason: null, supplement: null },
      true,
    );
  }
  return toKycDetail(
    await api.post<KycDetailDto>(`/admin/kyc-submissions/${id}/approve`, {
      shop_commission_rate: shopCommissionRate,
    }),
  );
}

export async function rejectKyc(
  id: number,
  reason: KycRejectReason,
  note: string | null,
  /** Nhãn lý do đã dịch — mock lưu làm "Lý do từ chối" (server tự dịch theo mã). */
  label: string,
): Promise<KycDetail> {
  if (USE_MOCK) {
    await mockDelay(250);
    return mockDecide(
      id,
      'REJECTED',
      { rejection_reason: note ? `${label}: ${note}` : label, supplement: null },
      true,
    );
  }
  return toKycDetail(
    await api.post<KycDetailDto>(`/admin/kyc-submissions/${id}/reject`, {
      reason_code: reason,
      note,
    }),
  );
}

export async function requestKycSupplement(
  id: number,
  documents: KycDocType[],
  note: string,
): Promise<KycDetail> {
  if (USE_MOCK) {
    await mockDelay(250);
    const due = new Date(Date.now() + SUPPLEMENT_DAYS * 864e5).toISOString();
    return mockDecide(
      id,
      'SUPPLEMENT_REQUESTED',
      { supplement: { documents, note, due_at: due }, rejection_reason: null },
      true,
    );
  }
  return toKycDetail(
    await api.post<KycDetailDto>(`/admin/kyc-submissions/${id}/request-supplement`, {
      documents,
      note,
    }),
  );
}

export async function requeueKyc(id: number): Promise<KycDetail> {
  if (USE_MOCK) {
    await mockDelay(200);
    return mockDecide(id, 'PENDING', { rejection_reason: null, supplement: null }, false);
  }
  return toKycDetail(await api.post<KycDetailDto>(`/admin/kyc-submissions/${id}/requeue`));
}
