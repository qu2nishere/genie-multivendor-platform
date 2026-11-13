// Ví & Escrow (API_SPEC 10.10): GET /seller/wallet, sổ giao dịch, POST /seller/withdrawals 🔁 🔐
// (OTP bước 2 `WITHDRAWAL`, header X-OTP-*). Mock: số liệu bản mẫu (vendor/data.jsx WALLET, LEDGER, BANK),
// lệnh rút giữ trong phiên tab; mã OTP 123456.
import { api, request } from '@/shared/api/http';
import { newIdempotencyKey } from '@/shared/api/idempotency';
import { mockDelay, mockFail, mockState, USE_MOCK } from '@/shared/api/mock';
import { sellerMoneyStore } from '@/shared/api/mock/seller';
import { filterLedger, MIN_WITHDRAW, withdrawIssue } from '../domain/wallet.rules';
import type { LedgerEntry, LedgerQuery, Wallet, Withdrawal } from '../domain/wallet.types';

interface WalletDto {
  available_balance: number;
  held_balance: number;
  cod_pending: number;
  pending_withdrawal: number;
  revenue_30d: { value: number; previous: number };
  commission_example: { gross: number; commission_rate: number; commission: number; net: number };
  payout_account: {
    bank_name: string;
    account_holder: string;
    account_number_masked: string;
    is_verified: boolean;
  } | null;
  min_withdrawal: number;
}

interface TxDto {
  id: number;
  created_at: string;
  reference_code: string;
  type: LedgerEntry['type'];
  description: string;
  gross_amount: number;
  commission_amount: number;
  net_amount: number;
  state: LedgerEntry['state'];
}

const toWallet = (d: WalletDto): Wallet => ({
  available: d.available_balance,
  held: d.held_balance,
  codPending: d.cod_pending,
  pendingWithdrawal: d.pending_withdrawal,
  revenue30d: d.revenue_30d,
  commissionExample: {
    gross: d.commission_example.gross,
    rate: d.commission_example.commission_rate,
    commission: d.commission_example.commission,
    net: d.commission_example.net,
  },
  payoutAccount: d.payout_account && {
    bank: d.payout_account.bank_name,
    holder: d.payout_account.account_holder,
    numberMasked: d.payout_account.account_number_masked,
    verified: d.payout_account.is_verified,
  },
  minWithdrawal: d.min_withdrawal,
});

const toEntry = (d: TxDto): LedgerEntry => ({
  id: d.id,
  createdAt: d.created_at,
  reference: d.reference_code,
  type: d.type,
  description: d.description,
  gross: d.gross_amount,
  commission: d.commission_amount,
  net: d.net_amount,
  state: d.state,
});

// ---- Mock ----
const tx = (
  id: number,
  at: string,
  ref: string,
  type: TxDto['type'],
  description: string,
  gross: number,
  commission: number,
  net: number,
  state: TxDto['state'],
): TxDto => ({
  id,
  created_at: `${at}+07:00`,
  reference_code: ref,
  type,
  description,
  gross_amount: gross,
  commission_amount: commission,
  net_amount: net,
  state,
});

const db = mockState('seller.wallet', () => ({
  seq: 13,
  ledger: [
    tx(
      90012,
      '2026-06-08T14:22:00',
      'TR-20260608-000141',
      'SALE',
      'Bán hàng — vào escrow',
      778000,
      38900,
      739100,
      'HELD',
    ),
    tx(
      90011,
      '2026-06-08T09:10:00',
      'TR-20260608-000140',
      'SALE',
      'Bán hàng — vào escrow',
      690000,
      34500,
      655500,
      'HELD',
    ),
    tx(
      90010,
      '2026-06-08T09:00:00',
      'PAYOUT-0012',
      'PAYOUT',
      'Rút tiền về ngân hàng (chờ duyệt)',
      -5_000_000,
      0,
      -5_000_000,
      'PAYOUT_PENDING',
    ),
    tx(
      90009,
      '2026-06-07T18:46:00',
      'TR-20260608-000139',
      'RELEASE',
      'Giải ngân — đơn hoàn tất',
      1_245_000,
      0,
      1_245_000,
      'AVAILABLE',
    ),
    tx(
      90008,
      '2026-06-07T11:03:00',
      'TR-20260608-000137',
      'RELEASE',
      'Giải ngân — đơn hoàn tất',
      459000,
      0,
      459000,
      'AVAILABLE',
    ),
    tx(
      90007,
      '2026-06-06T08:15:00',
      'TR-20260608-000135',
      'REFUND',
      'Hoàn tiền — khách trả hàng',
      -178000,
      -8900,
      -169100,
      'REFUNDED',
    ),
    tx(
      90006,
      '2026-06-05T20:01:00',
      'TR-20260608-000134',
      'SALE',
      'Bán hàng — vào escrow',
      540000,
      27000,
      513000,
      'HELD',
    ),
    tx(
      90005,
      '2026-06-05T13:27:00',
      'TR-20260608-000131',
      'RELEASE',
      'Giải ngân — đơn hoàn tất',
      320000,
      0,
      320000,
      'AVAILABLE',
    ),
    tx(
      90004,
      '2026-05-28T16:40:00',
      'PAYOUT-0011',
      'PAYOUT',
      'Rút tiền về ngân hàng',
      -8_000_000,
      0,
      -8_000_000,
      'PAYOUT_DONE',
    ),
    tx(
      90003,
      '2026-05-26T10:12:00',
      'TR-20260526-000118',
      'RELEASE',
      'Giải ngân — đơn hoàn tất',
      890000,
      0,
      890000,
      'AVAILABLE',
    ),
  ] as TxDto[],
}));

function mockWallet(): WalletDto {
  const s = db.get();
  const money = sellerMoneyStore.get();
  const held = s.ledger.filter((r) => r.state === 'HELD').reduce((n, r) => n + r.net_amount, 0);
  const sale = s.ledger.find((r) => r.type === 'SALE');
  return {
    available_balance: money.available,
    held_balance: held,
    cod_pending: 1_950_000,
    pending_withdrawal: money.pending_withdrawal,
    revenue_30d: { value: 45_800_000, previous: 38_680_000 },
    commission_example: {
      gross: sale?.gross_amount ?? 0,
      commission_rate: sale
        ? Math.round((sale.commission_amount / sale.gross_amount) * 1000) / 10
        : 5,
      commission: sale?.commission_amount ?? 0,
      net: sale?.net_amount ?? 0,
    },
    payout_account: {
      bank_name: 'Vietcombank',
      account_holder: 'DANG QUOC HUY',
      account_number_masked: '•••• •••• 6712',
      is_verified: true,
    },
    min_withdrawal: MIN_WITHDRAW,
  };
}

export async function getWallet(): Promise<Wallet> {
  if (USE_MOCK) {
    await mockDelay(200);
    return toWallet(mockWallet());
  }
  return toWallet(await api.get<WalletDto>('/seller/wallet'));
}

export async function listTransactions(
  q: LedgerQuery,
): Promise<{ items: LedgerEntry[]; months: string[] }> {
  if (USE_MOCK) {
    await mockDelay(180);
    const all = db.get().ledger.map(toEntry);
    const months = [...new Set(all.map((r) => r.createdAt.slice(0, 7)))].sort().reverse();
    return { items: filterLedger(all, q), months };
  }
  const res = await request<TxDto[]>('/seller/wallet/transactions', {
    query: { month: q.month, type: q.type ?? undefined, q: q.q.trim() || undefined, size: 100 },
  });
  return {
    items: res.data.map(toEntry),
    months: (res.summary?.available_months as string[] | undefined) ?? [q.month],
  };
}

/** 🔁 🔐 Rút tiền: một Idempotency-Key cho mỗi lần xác nhận. */
export async function createWithdrawal(
  amount: number,
  proof: { challengeId: string; code: string },
): Promise<Withdrawal> {
  if (USE_MOCK) {
    await mockDelay(450);
    if (proof.code !== '123456') mockFail('GNE-AUTH-4222', { details: { remaining_attempts: 4 } });
    const w = toWallet(mockWallet());
    const issue = withdrawIssue(amount, w);
    if (issue === 'min') mockFail('GNE-WAL-4221');
    if (issue === 'over') mockFail('GNE-WAL-4001');
    if (issue === 'no_account') mockFail('GNE-WAL-4032');
    const s = db.get();
    const code = `PAYOUT-${String(s.seq).padStart(4, '0')}`;
    const now = new Date();
    // Giữ mốc thời gian của bản mẫu (tháng 06/2026) để giao dịch mới nằm trong tháng đang xem
    const at = `2026-06-08T${String(now.getHours()).padStart(2, '0')}:${String(now.getMinutes()).padStart(2, '0')}:00+07:00`;
    const money = sellerMoneyStore.get();
    sellerMoneyStore.set({
      available: money.available - amount,
      pending_withdrawal: money.pending_withdrawal + amount,
    });
    db.set({
      seq: s.seq + 1,
      ledger: [
        {
          id: 90100 + s.seq,
          created_at: at,
          reference_code: code,
          type: 'PAYOUT',
          description: 'Rút tiền về ngân hàng (chờ duyệt)',
          gross_amount: -amount,
          commission_amount: 0,
          net_amount: -amount,
          state: 'PAYOUT_PENDING',
        },
        ...s.ledger,
      ],
    });
    return {
      id: s.seq,
      code,
      amount,
      status: 'PENDING',
      bankMasked: 'Vietcombank ••6712',
      requestedAt: at,
    };
  }
  const d = await api.post<{
    id: number;
    code: string;
    amount: number;
    status: 'PENDING';
    bank_account_masked: string;
    requested_at: string;
  }>(
    '/seller/withdrawals',
    { amount },
    {
      idempotencyKey: newIdempotencyKey(),
      headers: { 'X-OTP-Challenge-Id': proof.challengeId, 'X-OTP-Code': proof.code },
    },
  );
  return {
    id: d.id,
    code: d.code,
    amount: d.amount,
    status: d.status,
    bankMasked: d.bank_account_masked,
    requestedAt: d.requested_at,
  };
}
