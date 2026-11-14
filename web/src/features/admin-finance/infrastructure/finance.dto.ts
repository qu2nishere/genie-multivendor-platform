// DTO snake_case (API_SPEC 11.6–11.9).
import type { MockAdminWithdrawal, MockCodRemittance } from '@/shared/api/mock/admin';

export type WithdrawalDto = Omit<MockAdminWithdrawal, 'transfer' | 'reject_reason'>;

export interface TransferDto {
  bank_name: string;
  account_number: string;
  account_holder: string;
  amount: number;
  transfer_content: string;
  qr_payload: string;
}

export interface CommissionSettingsDto {
  default_rate: number;
  categories: { category_id: number; name: string; rate: number; is_enabled: boolean }[];
}

export interface LedgerEntryDto {
  seq: number;
  created_at: string;
  event_type: string;
  reference: string;
  account: string;
  debit: number | null;
  credit: number | null;
}

export type CodRemittanceDto = Omit<
  MockCodRemittance,
  'sample_lines' | 'statement_file_name' | 'confirmed_lines'
>;

export interface StatementLineDto {
  tracking_code: string;
  amount: number;
  fee: number;
}

export interface ReconciliationDto {
  lines: (StatementLineDto & {
    order_code: string | null;
    statement_amount: number;
    order_amount: number | null;
    result: 'MATCH' | 'AMOUNT_MISMATCH' | 'NOT_FOUND';
  })[];
  totals: {
    statement_net: number;
    received_amount: number;
    matched_count: number;
    is_balanced: boolean;
  };
}

export interface CodRemittanceDetailDto extends CodRemittanceDto {
  statement_file_name: string | null;
  sample_lines?: StatementLineDto[];
  lines?: ReconciliationDto['lines'];
  totals?: ReconciliationDto['totals'];
}
