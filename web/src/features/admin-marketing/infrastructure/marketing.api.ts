// Marketing sàn (API_SPEC 11.14–11.17). Mock giữ dữ liệu trong phiên tab và báo lỗi như server
// (trùng mã voucher, xóa voucher đã dùng, lên lịch chưa có SP duyệt, banner đủ 8 / ngoài miền,
// email người nhận không tồn tại).
import { api, getPage, request } from '@/shared/api/http';
import { mockDelay, mockFail, paginatePage, USE_MOCK } from '@/shared/api/mock';
import type { Page } from '@/shared/api/types';
import { isoDateVN as todayVN } from '@/shared/lib/format';
import {
  bannerStateOf,
  inVoucherTab,
  MAX_LIVE_PER_POSITION,
  NOTIFICATION_PAGE_SIZE,
  plainText,
  sitePath,
  type VoucherTab,
} from '../domain/marketing.rules';
import type {
  AdminBanner,
  BannerInput,
  BannerPosition,
  FlashCampaign,
  FlashCampaignDetail,
  FlashCampaignInput,
  FlashItemStatus,
  NotificationInput,
  NotificationSummary,
  NotificationTarget,
  PlatformNotification,
  PlatformNotificationDetail,
  PlatformVoucher,
  PlatformVoucherInput,
  PlatformVoucherSummary,
} from '../domain/marketing.types';
import type {
  AdminBannerDto,
  FlashCampaignDetailDto,
  FlashCampaignDto,
  PlatformNotificationDetailDto,
  PlatformNotificationDto,
  PlatformVoucherDto,
  PlatformVoucherInputDto,
} from './marketing.dto';
import {
  toBanner,
  toCampaign,
  toCampaignDetail,
  toNotification,
  toNotificationDetail,
  toVoucher,
  toVoucherInput,
  toVoucherInputDto,
} from './marketing.mapper';
import {
  bannersStore,
  flashStore,
  KNOWN_EMAILS,
  notificationsStore,
  REACH,
  VOUCHER_TOTALS,
  vouchersStore,
  type BannerRow,
} from './marketing.mock';

const nextId = (ids: number[]) => Math.max(0, ...ids) + 1;

// ---------- 11.14 Voucher sàn ----------
export interface VoucherList {
  items: PlatformVoucher[];
  summary: PlatformVoucherSummary;
}

export async function listPlatformVouchers(tab: VoucherTab): Promise<VoucherList> {
  if (USE_MOCK) {
    await mockDelay(180);
    const rows = vouchersStore.get();
    return {
      items: rows.filter((v) => inVoucherTab(v.status, tab)).map(toVoucher),
      summary: {
        activeCount: rows.filter((v) => v.status === 'ACTIVE').length,
        totalUsage: rows.reduce((n, v) => n + v.usage.used, 0),
        totalDiscountUsed: VOUCHER_TOTALS.total_discount_used,
        scheduledCount: rows.filter((v) => v.status === 'SCHEDULED').length,
        remainingBudget: VOUCHER_TOTALS.remaining_budget,
      },
    };
  }
  const res = await getPage<PlatformVoucherDto, PlatformVoucher>('/admin/vouchers', toVoucher, {
    query: { status: tab === 'ALL' ? undefined : tab, size: 50 },
  });
  const s = res.summary as Record<string, number | undefined>;
  return {
    items: res.items,
    summary: {
      activeCount: s.active_count ?? 0,
      totalUsage: s.total_usage ?? 0,
      totalDiscountUsed: s.total_discount_used ?? 0,
      scheduledCount: s.scheduled_count ?? 0,
      remainingBudget: s.remaining_budget ?? 0,
    },
  };
}

export async function getPlatformVoucher(id: number): Promise<PlatformVoucherInput> {
  if (USE_MOCK) {
    await mockDelay(120);
    const v = vouchersStore.get().find((x) => x.id === id);
    if (!v) mockFail('GNE-VCH-4041');
    return toVoucherInput(v.input);
  }
  return toVoucherInput(await api.get<PlatformVoucherInputDto>(`/admin/vouchers/${id}`));
}

export async function savePlatformVoucher(
  id: number | null,
  v: PlatformVoucherInput,
): Promise<PlatformVoucher> {
  const body = toVoucherInputDto(v);
  if (USE_MOCK) {
    await mockDelay(300);
    const rows = vouchersStore.get();
    if (rows.some((x) => x.code === body.code && x.id !== id)) mockFail('GNE-VCH-4092');
    const today = todayVN();
    const status: PlatformVoucherDto['status'] =
      body.valid_from.slice(0, 10) > today
        ? 'SCHEDULED'
        : body.valid_to.slice(0, 10) < today
          ? 'ENDED'
          : 'ACTIVE';
    const prev = rows.find((x) => x.id === id);
    const row = {
      id: id ?? nextId(rows.map((x) => x.id)),
      title: body.title,
      code: body.code,
      discount_type: body.discount_type,
      discount_value: body.discount_value,
      min_order_value: body.min_order_value,
      max_discount: body.max_discount,
      usage: { used: prev?.usage.used ?? 0, limit: body.usage_limit },
      usage_per_user: body.usage_per_user,
      valid_from: body.valid_from,
      valid_to: body.valid_to,
      funded_by: body.funded_by,
      status: prev?.status === 'DISABLED' ? 'DISABLED' : status,
      discount_used: prev?.discount_used ?? 0,
      input: body,
    } satisfies (typeof rows)[number];
    vouchersStore.set(prev ? rows.map((x) => (x.id === id ? row : x)) : [row, ...rows]);
    return toVoucher(row);
  }
  return toVoucher(
    id
      ? await api.put<PlatformVoucherDto>(`/admin/vouchers/${id}`, body)
      : await api.post<PlatformVoucherDto>('/admin/vouchers', body),
  );
}

/** Tắt (đang chạy) / Huỷ lịch (sắp diễn ra). */
export async function disablePlatformVoucher(id: number): Promise<void> {
  if (USE_MOCK) {
    await mockDelay(200);
    vouchersStore.set(
      vouchersStore.get().map((x) => (x.id === id ? { ...x, status: 'DISABLED' } : x)),
    );
    return;
  }
  await api.patch(`/admin/vouchers/${id}`, { is_active: false });
}

export async function deletePlatformVoucher(id: number): Promise<void> {
  if (USE_MOCK) {
    await mockDelay(200);
    const v = vouchersStore.get().find((x) => x.id === id);
    if (!v) mockFail('GNE-VCH-4041');
    if (v.usage.used > 0) mockFail('GNE-VCH-4093');
    vouchersStore.set(vouchersStore.get().filter((x) => x.id !== id));
    return;
  }
  await api.delete(`/admin/vouchers/${id}`);
}

// ---------- 11.15 Flash Sale ----------
export async function listCampaigns(): Promise<FlashCampaign[]> {
  if (USE_MOCK) {
    await mockDelay(150);
    return flashStore.get().map((c) => toCampaign({ ...c, item_count: c.items.length }));
  }
  return (await api.get<FlashCampaignDto[]>('/admin/flash-sales')).map(toCampaign);
}

export async function getCampaign(id: number): Promise<FlashCampaignDetail> {
  if (USE_MOCK) {
    await mockDelay(150);
    const c = flashStore.get().find((x) => x.id === id);
    if (!c) mockFail('GNE-SYS-4040', { status: 404 });
    return toCampaignDetail({ ...c, item_count: c.items.length });
  }
  return toCampaignDetail(await api.get<FlashCampaignDetailDto>(`/admin/flash-sales/${id}`));
}

const toIsoVN = (local: string) => (local.length === 16 ? `${local}:00+07:00` : local);

export async function createCampaign(f: FlashCampaignInput): Promise<number> {
  const body = {
    name: f.name.trim(),
    starts_at: toIsoVN(f.startsAt),
    ends_at: toIsoVN(f.endsAt),
    max_items_per_shop: f.maxItemsPerShop,
    registration_deadline: f.registrationDeadline,
  };
  if (USE_MOCK) {
    await mockDelay(250);
    const rows = flashStore.get();
    const id = nextId(rows.map((x) => x.id));
    flashStore.set([{ id, ...body, status: 'DRAFT', item_count: 0, items: [] }, ...rows]);
    return id;
  }
  return (await api.post<FlashCampaignDto>('/admin/flash-sales', body)).id;
}

function patchCampaign(
  id: number,
  fn: (c: FlashCampaignDetailDto) => FlashCampaignDetailDto,
): void {
  const rows = flashStore.get();
  const cur = rows.find((x) => x.id === id);
  if (!cur) mockFail('GNE-SYS-4040', { status: 404 });
  flashStore.set(rows.map((x) => (x.id === id ? fn(cur) : x)));
}

export async function scheduleCampaign(id: number): Promise<void> {
  if (USE_MOCK) {
    await mockDelay(220);
    patchCampaign(id, (c) => {
      if (!c.items.some((i) => i.status === 'APPROVED')) mockFail('GNE-PRO-4224');
      return { ...c, status: 'SCHEDULED' };
    });
    return;
  }
  await api.post(`/admin/flash-sales/${id}/schedule`);
}

export async function endCampaign(id: number): Promise<void> {
  if (USE_MOCK) {
    await mockDelay(220);
    patchCampaign(id, (c) => ({ ...c, status: 'ENDED' }));
    return;
  }
  await api.post(`/admin/flash-sales/${id}/end`);
}

export async function reviewFlashItem(
  campaignId: number,
  itemId: number,
  status: Exclude<FlashItemStatus, 'PENDING'>,
): Promise<void> {
  if (USE_MOCK) {
    await mockDelay(180);
    patchCampaign(campaignId, (c) => {
      if (c.status !== 'DRAFT') mockFail('GNE-SYS-4090', { status: 409 });
      return {
        ...c,
        items: c.items.map((i) => {
          if (i.id !== itemId) return i;
          if (status === 'APPROVED' && !i.meets_discount_rule) mockFail('GNE-PRO-4221');
          return { ...i, status };
        }),
      };
    });
    return;
  }
  await api.patch(`/admin/flash-sales/${campaignId}/items/${itemId}`, { status });
}

// ---------- 11.16 Banner ----------
const withState = (b: BannerRow): AdminBannerDto => ({
  ...b,
  state: bannerStateOf(
    { active: b.is_active, startsAt: b.starts_at, endsAt: b.ends_at },
    todayVN(),
  ),
});

export async function listBanners(position: BannerPosition | null): Promise<AdminBanner[]> {
  if (USE_MOCK) {
    await mockDelay(180);
    return bannersStore
      .get()
      .filter((b) => !position || b.position === position)
      .map(withState)
      .map(toBanner);
  }
  const res = await getPage<AdminBannerDto, AdminBanner>('/admin/banners', toBanner, {
    query: { position: position ?? undefined, size: 60 },
  });
  return res.items;
}

/** Danh mục (cho "Đầu trang danh mục") — mock cần slug để dựng trang xem. */
export async function saveBanner(
  id: string | null,
  b: BannerInput,
  category: { id: number; name: string; slug: string } | null,
): Promise<void> {
  const target = sitePath(b.targetUrl);
  if (USE_MOCK) {
    await mockDelay(300);
    if (!target) mockFail('GNE-BNR-4222');
    const rows = bannersStore.get();
    const prev = rows.find((x) => x.id === id);
    const image = b.imagePreview ?? prev?.image_url ?? null;
    if (!image) mockFail('GNE-VAL-4220');
    const next: BannerRow = {
      id: id ?? `BN-${Date.now().toString(36).toUpperCase()}`,
      title: b.title.trim(),
      image_url: image,
      mobile_image_url: b.mobilePreview ?? prev?.mobile_image_url ?? null,
      target_url: target,
      position: b.position,
      category: b.position === 'CATEGORY_TOP' ? category : null,
      sort_order: b.sortOrder,
      starts_at: b.startsAt,
      ends_at: b.endsAt,
      is_active: b.active,
      impressions: prev?.impressions ?? 0,
      clicks: prev?.clicks ?? 0,
    };
    const live = rows
      .filter((x) => x.position === b.position && x.id !== id)
      .map(withState)
      .filter((x) => x.state === 'LIVE').length;
    if (withState(next).state === 'LIVE' && live >= MAX_LIVE_PER_POSITION) mockFail('GNE-BNR-4221');
    bannersStore.set(prev ? rows.map((x) => (x.id === id ? next : x)) : [next, ...rows]);
    return;
  }
  const body = {
    title: b.title.trim(),
    ...(b.imageObjectKey ? { image_object_key: b.imageObjectKey } : {}),
    mobile_image_object_key: b.mobileImageObjectKey,
    target_url: target ?? b.targetUrl,
    position: b.position,
    category_id: b.position === 'CATEGORY_TOP' ? b.categoryId : null,
    sort_order: b.sortOrder,
    starts_at: `${b.startsAt}T00:00:00+07:00`,
    ends_at: `${b.endsAt}T23:59:59+07:00`,
    is_active: b.active,
  };
  if (id) await api.put(`/admin/banners/${encodeURIComponent(id)}`, body);
  else await api.post('/admin/banners', body);
}

export async function setBannerActive(id: string, active: boolean): Promise<void> {
  if (USE_MOCK) {
    await mockDelay(150);
    bannersStore.set(
      bannersStore.get().map((x) => (x.id === id ? { ...x, is_active: active } : x)),
    );
    return;
  }
  await api.patch(`/admin/banners/${encodeURIComponent(id)}`, { is_active: active });
}

export async function deleteBanner(id: string): Promise<void> {
  if (USE_MOCK) {
    await mockDelay(150);
    bannersStore.set(bannersStore.get().filter((x) => x.id !== id));
    return;
  }
  await api.delete(`/admin/banners/${encodeURIComponent(id)}`);
}

// ---------- 11.17 Thông báo sàn ----------
export interface NotificationPage extends Page<PlatformNotification> {
  summary: NotificationSummary;
}

export async function listPlatformNotifications(
  target: NotificationTarget | null,
  q: string,
  page: number,
): Promise<NotificationPage> {
  if (USE_MOCK) {
    await mockDelay(180);
    const all = notificationsStore.get();
    const needle = q.trim().toLowerCase();
    const rows = all
      .filter((n) => !target || n.target === target)
      .filter((n) => `${n.title} ${plainText(n.content_html)}`.toLowerCase().includes(needle));
    const p = paginatePage(rows, { page, size: NOTIFICATION_PAGE_SIZE });
    return {
      ...p,
      items: p.items.map(toNotification),
      summary: {
        sentThisMonth: all.filter((n) => !n.is_scheduled).length,
        totalReach: all.reduce((n, x) => n + x.reach_count, 0),
        totalReads: all.reduce((n, x) => n + x.read_count, 0),
      },
    };
  }
  const res = await request<PlatformNotificationDto[]>('/admin/platform-notifications', {
    query: {
      target: target ?? undefined,
      q: q.trim() || undefined,
      page,
      size: NOTIFICATION_PAGE_SIZE,
    },
  });
  const p = res.pagination && 'page' in res.pagination ? res.pagination : null;
  const s = (res.summary ?? {}) as Record<string, number | undefined>;
  return {
    items: res.data.map(toNotification),
    page: p?.page ?? page,
    size: p?.size ?? NOTIFICATION_PAGE_SIZE,
    total_items: p?.total_items ?? res.data.length,
    total_pages: p?.total_pages ?? 1,
    summary: {
      sentThisMonth: s.sent_this_month ?? 0,
      totalReach: s.total_reach ?? 0,
      totalReads: s.total_reads ?? 0,
    },
  };
}

export async function getPlatformNotification(id: number): Promise<PlatformNotificationDetail> {
  if (USE_MOCK) {
    await mockDelay(120);
    const n = notificationsStore.get().find((x) => x.id === id);
    if (!n) mockFail('GNE-SYS-4040', { status: 404 });
    return toNotificationDetail(n);
  }
  return toNotificationDetail(
    await api.get<PlatformNotificationDetailDto>(`/admin/platform-notifications/${id}`),
  );
}

export async function savePlatformNotification(
  id: number | null,
  n: NotificationInput,
  /** Mock: tên người gửi hiển thị. */
  sender: string,
): Promise<void> {
  const body = {
    title: n.title.trim(),
    content_html: n.contentHtml,
    image_object_key: n.imageObjectKey,
    target: n.target,
    recipient_emails: n.target === 'SPECIFIC_USERS' ? n.recipientEmails : [],
    send_at: n.sendAt,
  };
  if (USE_MOCK) {
    await mockDelay(300);
    if (n.target === 'SPECIFIC_USERS') {
      const unknown = n.recipientEmails.filter((e) => !KNOWN_EMAILS.includes(e));
      if (n.recipientEmails.length === 0 || unknown.length)
        mockFail('GNE-NOT-4221', { details: { unknown_emails: unknown.join(', ') } });
    }
    const rows = notificationsStore.get();
    const prev = rows.find((x) => x.id === id);
    if (prev && !prev.is_scheduled) mockFail('GNE-NOT-4091');
    const nid = id ?? nextId(rows.map((x) => x.id));
    const reach = n.target === 'SPECIFIC_USERS' ? n.recipientEmails.length : REACH[n.target];
    const row: PlatformNotificationDetailDto = {
      id: nid,
      code: `PN-${nid}`,
      title: body.title,
      content_preview: plainText(n.contentHtml).slice(0, 160),
      content_html: n.contentHtml,
      image_url: n.imagePreview ?? prev?.image_url ?? null,
      target: n.target,
      created_by: prev?.created_by ?? sender,
      send_at: n.sendAt ? toIsoVN(n.sendAt) : new Date().toISOString(),
      is_scheduled: !!n.sendAt,
      read_count: 0,
      reach_count: reach,
      recipient_emails: body.recipient_emails,
    };
    notificationsStore.set(prev ? rows.map((x) => (x.id === id ? row : x)) : [row, ...rows]);
    return;
  }
  const payload = { ...body, send_at: n.sendAt ? toIsoVN(n.sendAt) : null };
  if (id) await api.put(`/admin/platform-notifications/${id}`, payload);
  else await api.post('/admin/platform-notifications', payload);
}

export async function deletePlatformNotification(id: number): Promise<void> {
  if (USE_MOCK) {
    await mockDelay(150);
    notificationsStore.set(notificationsStore.get().filter((x) => x.id !== id));
    return;
  }
  await api.delete(`/admin/platform-notifications/${id}`);
}
