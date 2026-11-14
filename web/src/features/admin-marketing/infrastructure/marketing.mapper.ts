import type {
  AdminBanner,
  FlashCampaign,
  FlashCampaignDetail,
  PlatformNotification,
  PlatformNotificationDetail,
  PlatformVoucher,
  PlatformVoucherInput,
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

export const toVoucher = (d: PlatformVoucherDto): PlatformVoucher => ({
  id: d.id,
  title: d.title,
  code: d.code,
  discountType: d.discount_type,
  discountValue: d.discount_value,
  minOrderValue: d.min_order_value,
  maxDiscount: d.max_discount,
  usage: d.usage,
  usagePerUser: d.usage_per_user,
  validFrom: d.valid_from,
  validTo: d.valid_to,
  fundedBy: d.funded_by,
  status: d.status,
});

/** "YYYY-MM-DD" (giờ VN) ⇄ ISO đầu / cuối ngày. */
const dayStart = (d: string) => `${d}T00:00:00+07:00`;
const dayEnd = (d: string) => `${d}T23:59:59+07:00`;
const dayOf = (iso: string) => iso.slice(0, 10);

export const toVoucherInputDto = (v: PlatformVoucherInput): PlatformVoucherInputDto => ({
  title: v.title.trim(),
  code: v.code,
  discount_type: v.discountType,
  discount_value: v.discountType === 'FREESHIP' ? 0 : (v.discountValue ?? 0),
  max_discount: v.maxDiscount,
  min_order_value: v.minOrderValue ?? 0,
  usage_limit: v.usageLimit ?? 0,
  usage_per_user: v.usagePerUser ?? 1,
  budget: v.budget,
  valid_from: dayStart(v.validFrom),
  valid_to: dayEnd(v.validTo),
  scope: v.scope,
  category_ids: v.scope === 'CATEGORY' ? v.categoryIds : [],
  excluded_category_ids: v.excludedCategoryIds,
  funded_by: v.fundedBy,
  platform_share_percent: v.fundedBy === 'CO_SPONSOR' ? v.platformSharePercent : null,
});

export const toVoucherInput = (d: PlatformVoucherInputDto): PlatformVoucherInput => ({
  title: d.title,
  code: d.code,
  discountType: d.discount_type,
  discountValue: d.discount_type === 'FREESHIP' ? null : d.discount_value,
  maxDiscount: d.max_discount,
  minOrderValue: d.min_order_value,
  usageLimit: d.usage_limit,
  usagePerUser: d.usage_per_user,
  budget: d.budget,
  validFrom: dayOf(d.valid_from),
  validTo: dayOf(d.valid_to),
  scope: d.scope,
  categoryIds: d.category_ids,
  excludedCategoryIds: d.excluded_category_ids,
  fundedBy: d.funded_by,
  platformSharePercent: d.platform_share_percent,
});

export const toCampaign = (d: FlashCampaignDto): FlashCampaign => ({
  id: d.id,
  name: d.name,
  startsAt: d.starts_at,
  endsAt: d.ends_at,
  status: d.status,
  itemCount: d.item_count,
});

export const toCampaignDetail = (d: FlashCampaignDetailDto): FlashCampaignDetail => ({
  ...toCampaign(d),
  maxItemsPerShop: d.max_items_per_shop,
  registrationDeadline: d.registration_deadline,
  items: d.items.map((i) => ({
    id: i.id,
    productId: i.product_id,
    productName: i.product_name,
    shopName: i.shop_name,
    referencePrice: i.reference_price,
    flashPrice: i.flash_price,
    flashStock: i.flash_stock,
    status: i.status,
    meetsDiscountRule: i.meets_discount_rule,
  })),
});

export const toBanner = (d: AdminBannerDto): AdminBanner => ({
  id: d.id,
  title: d.title,
  imageUrl: d.image_url,
  mobileImageUrl: d.mobile_image_url,
  targetUrl: d.target_url,
  position: d.position,
  category: d.category,
  sortOrder: d.sort_order,
  startsAt: d.starts_at,
  endsAt: d.ends_at,
  active: d.is_active,
  state: d.state,
  impressions: d.impressions,
  clicks: d.clicks,
});

export const toNotification = (d: PlatformNotificationDto): PlatformNotification => ({
  id: d.id,
  code: d.code,
  title: d.title,
  contentPreview: d.content_preview,
  imageUrl: d.image_url,
  target: d.target,
  createdBy: d.created_by,
  sendAt: d.send_at,
  scheduled: d.is_scheduled,
  readCount: d.read_count,
  reachCount: d.reach_count,
});

export const toNotificationDetail = (
  d: PlatformNotificationDetailDto,
): PlatformNotificationDetail => ({
  ...toNotification(d),
  contentHtml: d.content_html,
  recipientEmails: d.recipient_emails,
});
