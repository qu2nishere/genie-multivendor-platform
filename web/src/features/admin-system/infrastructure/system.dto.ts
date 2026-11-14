// DTO snake_case (API_SPEC 11.10, 11.13, 11.18).
export interface StaffDto {
  id: number;
  full_name: string;
  email: string;
  role: 'STAFF' | 'ADMIN';
  staff_title: 'MODERATOR' | 'CS_SPECIALIST' | 'FINANCE_OFFICER' | 'CUSTOM' | null;
  permissions: string[];
  status: 'ACTIVE' | 'LOCKED' | 'PENDING_ACTIVATION';
  is_self: boolean;
}

export interface PlatformUserDto {
  id: number;
  code: string;
  full_name: string;
  shop_name: string | null;
  email: string;
  phone_number: string;
  role: 'CUSTOMER' | 'VENDOR';
  order_count: number;
  warning_count: number;
  status: 'ACTIVE' | 'LOCKED' | 'SUSPENDED';
  suspension: { reason: string; until: string | null } | null;
}

export interface AuditLogDto {
  id: number;
  created_at: string;
  actor_email: string;
  actor_role: string;
  action: string;
  category: 'AUTH' | 'KYC' | 'FINANCE' | 'MODERATION' | 'SECURITY' | 'CONFIG';
  target: string;
  ip_address: string;
  result: 'SUCCESS' | 'FAILED' | 'DENIED';
}
