// DTO snake_case (API_SPEC 11.14–11.17).
export interface PlatformVoucherDto {
  id: number;
  title: string;
  code: string;
  discount_type: 'PERCENTAGE' | 'FIXED_AMOUNT' | 'FREESHIP';
  discount_value: number;
  min_order_value: number;
  max_discount: number | null;
  usage: { used: number; limit: number };
  usage_per_user: number;
  valid_from: string;
  valid_to: string;
  funded_by: 'PLATFORM' | 'CO_SPONSOR';
  status: 'ACTIVE' | 'SCHEDULED' | 'ENDED' | 'DISABLED';
}

export interface PlatformVoucherInputDto {
  title: string;
  code: string;
  discount_type: 'PERCENTAGE' | 'FIXED_AMOUNT' | 'FREESHIP';
  discount_value: number;
  max_discount: number | null;
  min_order_value: number;
  usage_limit: number;
  usage_per_user: number;
  budget: number | null;
  valid_from: string;
  valid_to: string;
  scope: 'ALL' | 'CATEGORY';
  category_ids: number[];
  excluded_category_ids: number[];
  funded_by: 'PLATFORM' | 'CO_SPONSOR';
  platform_share_percent: number | null;
}

export interface FlashCampaignDto {
  id: number;
  name: string;
  starts_at: string;
  ends_at: string;
  status: 'DRAFT' | 'SCHEDULED' | 'ACTIVE' | 'ENDED';
  item_count: number;
}

export interface FlashItemDto {
  id: number;
  product_id: number;
  product_name: string;
  shop_name: string;
  reference_price: number;
  flash_price: number;
  flash_stock: number;
  status: 'PENDING' | 'APPROVED' | 'REJECTED';
  meets_discount_rule: boolean;
}

export interface FlashCampaignDetailDto extends FlashCampaignDto {
  max_items_per_shop: number;
  registration_deadline: string;
  items: FlashItemDto[];
}

export interface AdminBannerDto {
  id: string;
  title: string;
  image_url: string;
  mobile_image_url: string | null;
  target_url: string;
  position:
    | 'HOME_TOP_CAROUSEL'
    | 'HOME_TOP_SIDE'
    | 'HOME_MIDDLE'
    | 'CATEGORY_TOP'
    | 'FLASH_SALE_TOP'
    | 'POPUP';
  category: { id: number; name: string; slug: string } | null;
  sort_order: number;
  starts_at: string;
  ends_at: string;
  is_active: boolean;
  state: 'LIVE' | 'SCHEDULED' | 'ENDED' | 'OFF';
  impressions: number;
  clicks: number;
}

export interface PlatformNotificationDto {
  id: number;
  code: string;
  title: string;
  content_preview: string;
  image_url: string | null;
  target: 'ALL' | 'CUSTOMER' | 'VENDOR' | 'SPECIFIC_USERS';
  created_by: string;
  send_at: string;
  is_scheduled: boolean;
  read_count: number;
  reach_count: number;
}

export interface PlatformNotificationDetailDto extends PlatformNotificationDto {
  content_html: string;
  recipient_emails: string[];
}
