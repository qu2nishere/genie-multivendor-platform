// Ví & Escrow của shop (API_SPEC 10.10; prototype vendor/Wallet.jsx, data.jsx WALLET, LEDGER, BANK).
export type TxType = 'SALE' | 'RELEASE' | 'PAYOUT' | 'REFUND';
export type TxState = 'HELD' | 'AVAILABLE' | 'PAYOUT_PENDING' | 'PAYOUT_DONE' | 'REFUNDED';

export interface PayoutAccount {
  bank: string;
  holder: string;
  numberMasked: string;
  verified: boolean;
}

export interface Wallet {
  available: number;
  held: number;
  codPending: number;
  pendingWithdrawal: number;
  revenue30d: { value: number; previous: number };
  /** Ví dụ "Cách tính tiền về ví" lấy từ đơn bán gần nhất. */
  commissionExample: { gross: number; rate: number; commission: number; net: number };
  payoutAccount: PayoutAccount | null;
  minWithdrawal: number;
}

export interface LedgerEntry {
  id: number;
  createdAt: string;
  reference: string;
  type: TxType;
  description: string;
  gross: number;
  commission: number;
  net: number;
  state: TxState;
}

export interface LedgerQuery {
  /** "2026-06" */
  month: string;
  type: TxType | null;
  q: string;
}

export interface Withdrawal {
  id: number;
  code: string;
  amount: number;
  status: 'PENDING';
  bankMasked: string;
  requestedAt: string;
}
