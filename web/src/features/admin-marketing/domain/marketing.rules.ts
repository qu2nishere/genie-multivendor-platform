// Hàm thuần marketing sàn (API_SPEC 11.14–11.17; prototype Vouchers / FlashSale / Banners /
// PlatformNotifications .jsx).
import type {
  AdminBanner,
  BannerPosition,
  FlashCampaignDetail,
  FlashItem,
  ManagedVoucherStatus,
  NotificationTarget,
  PlatformVoucher,
} from './marketing.types';

// ---------- Voucher sàn ----------
export type VoucherTab = 'ALL' | 'ACTIVE' | 'SCHEDULED' | 'ENDED';
export const VOUCHER_TABS: VoucherTab[] = ['ALL', 'ACTIVE', 'SCHEDULED', 'ENDED'];

export const inVoucherTab = (s: ManagedVoucherStatus, tab: VoucherTab): boolean =>
  tab === 'ALL' || s === tab;

/** Thao tác: chạy / sắp diễn ra → Nhân bản, Sửa, Tắt (sắp diễn ra: Huỷ); kết thúc → Nhân bản, Xoá (khi chưa dùng). */
export function voucherActions(
  v: Pick<PlatformVoucher, 'status' | 'usage'>,
): ('duplicate' | 'edit' | 'disable' | 'cancel' | 'delete')[] {
  if (v.status === 'ENDED' || v.status === 'DISABLED')
    return v.usage.used === 0 ? ['duplicate', 'delete'] : ['duplicate'];
  return ['duplicate', 'edit', v.status === 'SCHEDULED' ? 'cancel' : 'disable'];
}

export const usagePercent = (u: { used: number; limit: number }): number =>
  u.limit > 0 ? Math.min(100, Math.round((u.used / u.limit) * 100)) : 0;

// ---------- Flash Sale ----------
/** Giá Flash phải thấp hơn giá tham chiếu 30 ngày ≥ 15% (GNE-PRO-4221). */
export const MIN_FLASH_DISCOUNT = 15;

export const flashDiscount = (i: Pick<FlashItem, 'referencePrice' | 'flashPrice'>): number =>
  i.referencePrice > 0 ? Math.round((1 - i.flashPrice / i.referencePrice) * 100) : 0;

export const approvedCount = (c: Pick<FlashCampaignDetail, 'items'>): number =>
  c.items.filter((i) => i.status === 'APPROVED').length;

/** Duyệt / từ chối từng sản phẩm chỉ khi chiến dịch còn Nháp và sản phẩm đang chờ. */
export const canReviewItem = (
  c: Pick<FlashCampaignDetail, 'status'>,
  i: Pick<FlashItem, 'status'>,
) => c.status === 'DRAFT' && i.status === 'PENDING';

/** Thao tác chiến dịch: Nháp → Lên lịch (cần ≥ 1 SP đã duyệt); Đang chạy → Kết thúc sớm. */
export function campaignAction(
  c: Pick<FlashCampaignDetail, 'status' | 'items'>,
): { kind: 'schedule'; enabled: boolean } | { kind: 'end' } | null {
  if (c.status === 'DRAFT') return { kind: 'schedule', enabled: approvedCount(c) > 0 };
  if (c.status === 'ACTIVE') return { kind: 'end' };
  return null;
}

// ---------- Banner ----------
export const BANNER_POSITIONS: BannerPosition[] = [
  'HOME_TOP_CAROUSEL',
  'HOME_TOP_SIDE',
  'HOME_MIDDLE',
  'CATEGORY_TOP',
  'FLASH_SALE_TOP',
  'POPUP',
];

/** Tối đa banner đang hiển thị mỗi vị trí (GNE-BNR-4221). */
export const MAX_LIVE_PER_POSITION = 8;

export type LinkType = 'category' | 'product' | 'shop' | 'flash' | 'discover' | 'campaign';
export const LINK_TYPES: LinkType[] = [
  'category',
  'product',
  'shop',
  'flash',
  'discover',
  'campaign',
];
export const FLASH_LINK = '/#flash-sale';
export const DISCOVER_LINK = '/discover';

/** Đọc liên kết đích (route mới, plan mục 4.5) thành loại + giá trị cho ô chọn. */
export function parseLink(link: string): { type: LinkType; value: string } {
  const l = link.trim();
  if (!l) return { type: 'category', value: '' };
  if (l === FLASH_LINK) return { type: 'flash', value: '' };
  if (l === DISCOVER_LINK) return { type: 'discover', value: '' };
  const path = l.split(/[?#]/)[0] ?? '';
  let m = path.match(/^\/c\/([a-z0-9-]+)$/);
  if (m?.[1]) return { type: 'category', value: m[1] };
  m = path.match(/^\/p\/[a-z0-9-]*-(\d+)$/);
  if (m?.[1]) return { type: 'product', value: m[1] };
  m = path.match(/^\/shop\/(\d+)$/);
  if (m?.[1]) return { type: 'shop', value: m[1] };
  return { type: 'campaign', value: l };
}

/**
 * Chỉ nhận đường dẫn trong miền sàn (GNE-BNR-4222): chặn //other.com, /\\other.com, javascript:,
 * https://other.com… Trả đường dẫn chuẩn (pathname + query + hash) hoặc null.
 */
export function sitePath(link: string, origin = 'https://genie.vn'): string | null {
  const l = link.trim();
  if (!l || (/^[a-z][a-z0-9+.-]*:/i.test(l) && !l.startsWith(origin))) return null;
  try {
    const u = new URL(l, origin);
    return u.origin === origin ? u.pathname + u.search + u.hash : null;
  } catch {
    return null;
  }
}

type SlotKey = Pick<AdminBanner, 'position'> & { categoryId: number | null; id?: string };
const slotOf = (b: Pick<AdminBanner, 'position' | 'category' | 'id'>): SlotKey => ({
  position: b.position,
  categoryId: b.category?.id ?? null,
  id: b.id,
});

/** Cùng ô hiển thị: cùng vị trí (và cùng danh mục với "Đầu trang danh mục"), khác banner. */
export function sameSlot(b: SlotKey, f: SlotKey): boolean {
  return (
    b.position === f.position &&
    (f.position !== 'CATEGORY_TOP' || b.categoryId === f.categoryId) &&
    b.id !== f.id
  );
}

/** Thứ tự kế tiếp của một ô: lớn nhất đang dùng + 1 (số nhỏ hiện trước). */
export function nextOrder(
  rows: Pick<AdminBanner, 'position' | 'category' | 'id' | 'sortOrder'>[],
  f: SlotKey,
): number {
  return (
    rows.filter((b) => sameSlot(slotOf(b), f)).reduce((m, b) => Math.max(m, b.sortOrder), 0) + 1
  );
}

export const ctr = (b: Pick<AdminBanner, 'impressions' | 'clicks'>): number | null =>
  b.impressions > 0 ? b.clicks / b.impressions : null;

/** Trang storefront hiện banner (nút "Xem"). */
export function previewPath(b: Pick<AdminBanner, 'position' | 'category'>): string {
  if (b.position === 'CATEGORY_TOP') return b.category ? `/c/${b.category.slug}` : '/';
  if (b.position === 'FLASH_SALE_TOP' || b.position === 'HOME_MIDDLE') return '/#flash-sale';
  return '/';
}

/** Banner chỉ xóa được khi đã tắt hoặc đã kết thúc (GNE-BNR-4091). */
export const canDeleteBanner = (b: Pick<AdminBanner, 'state'>): boolean =>
  b.state === 'OFF' || b.state === 'ENDED';

/** Trạng thái theo ngày (server tính; mock dùng chung quy tắc với storefront). */
export function bannerStateOf(
  b: { active: boolean; startsAt: string; endsAt: string },
  today: string,
): AdminBanner['state'] {
  if (!b.active) return 'OFF';
  if (b.endsAt && b.endsAt.slice(0, 10) < today) return 'ENDED';
  if (b.startsAt && b.startsAt.slice(0, 10) > today) return 'SCHEDULED';
  return 'LIVE';
}

// ---------- Thông báo sàn ----------
export const NOTIFICATION_TARGETS: NotificationTarget[] = [
  'ALL',
  'VENDOR',
  'CUSTOMER',
  'SPECIFIC_USERS',
];
export const NOTIFICATION_PAGE_SIZE = 5;
export const TITLE_MAX = 100;

const EMAIL = /^[^\s@,]+@[^\s@,]+\.[^\s@,]+$/;

/** "a@x.vn, b@y.vn ,," → danh sách email (bỏ trống, trùng); sai định dạng trả ở `invalid`. */
export function parseEmails(text: string): { emails: string[]; invalid: string[] } {
  const all = [
    ...new Set(
      text
        .split(/[,;\s]+/)
        .map((s) => s.trim().toLowerCase())
        .filter(Boolean),
    ),
  ];
  return { emails: all.filter((e) => EMAIL.test(e)), invalid: all.filter((e) => !EMAIL.test(e)) };
}

/** Chữ thuần từ HTML nội dung (bỏ thẻ, gộp khoảng trắng) — dùng cho xem trước và kiểm rỗng. */
export const plainText = (html: string): string =>
  html
    .replace(/<[^>]*>/g, ' ')
    .replace(/&nbsp;/g, ' ')
    .replace(/\s+/g, ' ')
    .trim();

export const readRate = (n: { readCount: number; reachCount: number }): number =>
  n.reachCount > 0 ? Math.round((n.readCount / n.reachCount) * 100) : 0;
