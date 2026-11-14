// Tài chính sàn (API_SPEC 11.6–11.9; prototype admin/Finance.jsx, CodReconciliation.jsx).

// ---------- 11.6 Rút tiền ----------
export type WithdrawalStatus = 'PENDING' | 'APPROVED' | 'PROCESSING' | 'COMPLETED' | 'REJECTED';
export type ShopKycStatus = 'VERIFIED' | 'SUPPLEMENT_REQUESTED' | 'PENDING' | 'REJECTED';

export interface Withdrawal {
  id: number;
  code: string;
  shop: { id: number; name: string; logoUrl: string | null };
  bankAccountMasked: string;
  accountVerified: boolean;
  amount: number;
  shopKycStatus: ShopKycStatus;
  requestedAt: string;
  status: WithdrawalStatus;
}

export interface WithdrawalSummary {
  pendingCount: number;
  payableAmount: number;
  blockedAmount: number;
}

export interface PayoutTransfer {
  bankName: string;
  accountNumber: string;
  accountHolder: string;
  amount: number;
  transferContent: string;
  qrPayload: string;
}

// ---------- 11.8 Hoa hồng ----------
export interface CommissionSettings {
  defaultRate: number;
  categories: { categoryId: number; name: string; rate: number; enabled: boolean }[];
}

// ---------- 11.9 Sổ cái ----------
export type LedgerEvent =
  | 'ESCROW_IN'
  | 'COMMISSION'
  | 'PLATFORM_VOUCHER'
  | 'FREESHIP_COST'
  | 'COIN_COST'
  | 'ESCROW_RELEASE'
  | 'PAYOUT_ACCRUE'
  | 'PAYOUT_SETTLE'
  | 'REFUND'
  | 'GATEWAY_FEE';

export type LedgerAccount =
  'ESCROW_LIABILITY' | 'PLATFORM_REVENUE' | 'SELLER_PAYABLE' | 'PLATFORM_EXPENSE' | 'CASH';

export interface LedgerEntry {
  seq: number;
  createdAt: string;
  eventType: LedgerEvent;
  reference: string;
  account: LedgerAccount;
  debit: number | null;
  credit: number | null;
}

// ---------- 11.7 Đối soát COD ----------
export type Carrier = 'GHN' | 'GHTK' | 'VIETTEL_POST' | 'JNT_EXPRESS' | 'GENIE_EXPRESS';

export interface CodRemittance {
  id: number;
  code: string;
  carrier: Carrier;
  receivedAmount: number;
  receivedAt: string;
  bankReference: string;
  status: 'WAITING' | 'DONE';
}

/** Một dòng bảng kê của hãng (3 cột CSV). */
export interface StatementLine {
  trackingCode: string;
  amount: number;
  fee: number;
}

export type MatchResult = 'MATCH' | 'AMOUNT_MISMATCH' | 'NOT_FOUND';

export interface ReconciledLine extends StatementLine {
  orderCode: string | null;
  orderAmount: number | null;
  result: MatchResult;
}

export interface Reconciliation {
  lines: ReconciledLine[];
  totals: {
    statementNet: number;
    receivedAmount: number;
    matchedCount: number;
    balanced: boolean;
  };
}

export interface CodRemittanceDetail extends CodRemittance {
  /** Bảng kê mẫu của hãng (nút "Dùng bảng kê mẫu"; chỉ có ở mock / môi trường thử). */
  sampleLines: StatementLine[];
  statementFileName: string | null;
  /** Phiên đã xác nhận: kết quả đối chiếu đã lưu. */
  confirmed: Reconciliation | null;
}
