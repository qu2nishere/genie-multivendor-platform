// DTO snake_case (API_SPEC 11.4) — cùng hình kho mock dùng chung (bỏ trường nội bộ của mock).
import type { MockAdminDispute, MockAdminRefund } from '@/shared/api/mock/admin';

export type DisputeDto = Omit<MockAdminDispute, 'refund_account' | 'verdict'>;
export type RefundDto = MockAdminRefund;
