import { LEDGER_ACCOUNTS, LEDGER_EVENTS } from '../domain/finance.rules';
import type {
  CodRemittance,
  CodRemittanceDetail,
  CommissionSettings,
  LedgerAccount,
  LedgerEntry,
  LedgerEvent,
  PayoutTransfer,
  Reconciliation,
  StatementLine,
  Withdrawal,
} from '../domain/finance.types';
import type {
  CodRemittanceDetailDto,
  CodRemittanceDto,
  CommissionSettingsDto,
  LedgerEntryDto,
  ReconciliationDto,
  StatementLineDto,
  TransferDto,
  WithdrawalDto,
} from './finance.dto';

export const toWithdrawal = (d: WithdrawalDto): Withdrawal => ({
  id: d.id,
  code: d.code,
  shop: { id: d.shop.id, name: d.shop.name, logoUrl: d.shop.logo_url },
  bankAccountMasked: d.bank_account_masked,
  accountVerified: d.is_account_verified,
  amount: d.amount,
  shopKycStatus: d.shop_kyc_status,
  requestedAt: d.requested_at,
  status: d.status,
});

export const toTransfer = (d: TransferDto): PayoutTransfer => ({
  bankName: d.bank_name,
  accountNumber: d.account_number,
  accountHolder: d.account_holder,
  amount: d.amount,
  transferContent: d.transfer_content,
  qrPayload: d.qr_payload,
});

export const toCommission = (d: CommissionSettingsDto): CommissionSettings => ({
  defaultRate: d.default_rate,
  categories: d.categories.map((c) => ({
    categoryId: c.category_id,
    name: c.name,
    rate: c.rate,
    enabled: c.is_enabled,
  })),
});

const isEvent = (s: string): s is LedgerEvent => (LEDGER_EVENTS as string[]).includes(s);
const isAccount = (s: string): s is LedgerAccount => (LEDGER_ACCOUNTS as string[]).includes(s);

export const toLedgerEntry = (d: LedgerEntryDto): LedgerEntry | null =>
  isEvent(d.event_type) && isAccount(d.account)
    ? {
        seq: d.seq,
        createdAt: d.created_at,
        eventType: d.event_type,
        reference: d.reference,
        account: d.account,
        debit: d.debit,
        credit: d.credit,
      }
    : null;

export const toCodRemittance = (d: CodRemittanceDto): CodRemittance => ({
  id: d.id,
  code: d.code,
  carrier: d.carrier,
  receivedAmount: d.received_amount,
  receivedAt: d.received_at,
  bankReference: d.bank_reference,
  status: d.status,
});

export const toLine = (d: StatementLineDto): StatementLine => ({
  trackingCode: d.tracking_code,
  amount: d.amount,
  fee: d.fee,
});

export const toLineDto = (l: StatementLine): StatementLineDto => ({
  tracking_code: l.trackingCode,
  amount: l.amount,
  fee: l.fee,
});

export const toReconciliation = (d: ReconciliationDto): Reconciliation => ({
  lines: d.lines.map((l) => ({
    trackingCode: l.tracking_code,
    amount: l.statement_amount,
    fee: l.fee,
    orderCode: l.order_code,
    orderAmount: l.order_amount,
    result: l.result,
  })),
  totals: {
    statementNet: d.totals.statement_net,
    receivedAmount: d.totals.received_amount,
    matchedCount: d.totals.matched_count,
    balanced: d.totals.is_balanced,
  },
});

export const toReconciliationDto = (r: Reconciliation): ReconciliationDto => ({
  lines: r.lines.map((l) => ({
    tracking_code: l.trackingCode,
    amount: l.amount,
    statement_amount: l.amount,
    fee: l.fee,
    order_code: l.orderCode,
    order_amount: l.orderAmount,
    result: l.result,
  })),
  totals: {
    statement_net: r.totals.statementNet,
    received_amount: r.totals.receivedAmount,
    matched_count: r.totals.matchedCount,
    is_balanced: r.totals.balanced,
  },
});

export const toCodDetail = (d: CodRemittanceDetailDto): CodRemittanceDetail => ({
  ...toCodRemittance(d),
  sampleLines: (d.sample_lines ?? []).map(toLine),
  statementFileName: d.statement_file_name,
  confirmed:
    d.status === 'DONE' && d.lines && d.totals
      ? toReconciliation({ lines: d.lines, totals: d.totals })
      : null,
});
