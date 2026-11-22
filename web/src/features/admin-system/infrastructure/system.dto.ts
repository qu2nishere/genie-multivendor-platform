// DTO snake_case (API_SPEC 11.10, 11.13, 11.18).
export interface StaffDto {
  id: number;
  full_name: string;
  email: string;
  role: 'STAFF' | 'ADMIN';
  staff_title: 'MODERATOR' | 'CS_SPECIALIST' | 'FINANCE_OFFICER' | 'CUSTOM' | null;
  permissions: string[];
  status: 'ACTIVE' | 'LOCKED' | 'PENDING_ACTIVATION' | 'INVITATION_EXPIRED';
  /** Hạn lời mời (chờ kích hoạt / đã hết hạn); null với tài khoản đã kích hoạt — API_SPEC 11.18. */
  invitation_expires_at?: string | null;
  is_self: boolean;
}

export interface PlatformUserDto {
  id: number;
  code: string;
  full_name: string;
  shop_name: string | null;
  email: string;
  phone_number: string | null;
  role: 'CUSTOMER' | 'VENDOR';
  order_count: number;
  warning_count: number;
  status: 'ACTIVE' | 'LOCKED' | 'SUSPENDED';
  /** `reason_code` theo API 16.4 (client dịch); `until = null` = vô thời hạn. */
  suspension: { reason_code: string; note: string | null; until: string | null } | null;
}

export interface AuditLogDto {
  /** system_logs.id — UUID (chuỗi). */
  id: string;
  created_at: string;
  actor_email: string;
  actor_role: string;
  action: string;
  category: 'AUTH' | 'KYC' | 'FINANCE' | 'MODERATION' | 'SECURITY' | 'CONFIG';
  target: string | null;
  ip_address: string | null;
  result: 'SUCCESS' | 'FAILED' | 'DENIED';
  /** Cột gốc — hộp chi tiết (API_SPEC 11.10; plan 2026-11-22 nhật ký banner N4). */
  reference_id: string | null;
  user_agent: string | null;
  /** `{trường: {from, to}}` từ details.changes; null nếu không có. */
  changes: Record<string, { from: string | null; to: string | null }> | null;
}

export interface StaffEmailCheckDto {
  email: string;
  available: boolean;
  reason: 'INVALID' | 'DOMAIN' | 'TAKEN' | null;
  taken_by: 'CUSTOMER' | 'VENDOR' | 'STAFF' | 'ADMIN' | 'OTHER' | null;
}
