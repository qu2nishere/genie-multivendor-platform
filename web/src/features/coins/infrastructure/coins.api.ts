// Điểm Genie (API_SPEC 3.13). Mock: số dư = mẫu − Điểm dùng khi đặt hàng + Điểm đánh giá có ảnh + giao
// dịch trong phiên (điểm danh, đổi quà) — các kho mock dùng chung giữ trong phiên tab.
import { api } from '@/shared/api/http';
import { mockDelay, mockFail, mockState, USE_MOCK } from '@/shared/api/mock';
import { activityStore } from '@/shared/api/mock/activity';
import { placedStore } from '@/shared/api/mock/placed';
import { rewardOf } from '../domain/coins.rules';
import type { CoinReward, CoinSummary, CoinTxn } from '../domain/coins.types';
import type { CoinSummaryDto } from './coins.dto';
import { toCoinSummary } from './coins.mapper';
import { seedCoinSummary } from './coins.mock';

export const coinsDb = mockState<CoinSummaryDto>('coins', seedCoinSummary);

/** Lịch sử mẫu (prototype COIN_LOG). */
const SEED_TXNS: CoinTxn[] = [
  {
    id: 7781,
    type: 'ORDER_CASHBACK',
    description: 'Hoàn điểm đơn TR-20260608-000041',
    amount: 120,
    createdAt: '2026-06-08T10:00:00+07:00',
  },
  {
    id: 7760,
    type: 'REVIEW_BONUS',
    description: 'Đánh giá sản phẩm có ảnh',
    amount: 50,
    createdAt: '2026-06-06T09:00:00+07:00',
  },
  {
    id: 7712,
    type: 'USED_AT_CHECKOUT',
    description: 'Dùng Điểm giảm giá đơn TR-20260608-000033',
    amount: -200,
    createdAt: '2026-05-28T20:01:00+07:00',
  },
  {
    id: 7700,
    type: 'CHECK_IN',
    description: 'Điểm danh hằng ngày',
    amount: 100,
    createdAt: '2026-05-27T08:00:00+07:00',
  },
  {
    id: 7655,
    type: 'ORDER_CASHBACK',
    description: 'Hoàn điểm đơn TR-20260608-000021',
    amount: 80,
    createdAt: '2026-05-20T13:27:00+07:00',
  },
];
const REWARDS: CoinReward[] = [
  { id: 1, cost: 500, voucherValue: 5000, minOrder: 50000 },
  { id: 2, cost: 1000, voucherValue: 10000, minOrder: 50000 },
  { id: 3, cost: 2000, voucherValue: 20000, minOrder: 50000 },
];

const act = () => {
  const a = activityStore.get();
  return { ...a, coinTxns: a.coinTxns ?? [], redeemed: a.redeemed ?? [], reviews: a.reviews ?? [] };
};
const isToday = (iso: string | null) =>
  !!iso && new Date(iso).toDateString() === new Date().toDateString();

/** Giao dịch mock phát sinh trong phiên (mới nhất trước). */
function sessionTxns(): CoinTxn[] {
  const a = act();
  const placed: CoinTxn[] = placedStore
    .get()
    .sessions.filter((s) => s.coin_discount > 0)
    .map((s) => ({
      id: s.id,
      type: 'USED_AT_CHECKOUT',
      description: `Dùng Điểm giảm giá phiên ${s.code}`,
      amount: -s.coin_discount,
      createdAt: s.created_at,
    }));
  const reviews: CoinTxn[] = a.reviews
    .filter((r) => r.coins_awarded > 0)
    .map((r) => ({
      id: r.id,
      type: 'REVIEW_BONUS',
      description: 'Đánh giá sản phẩm có ảnh',
      amount: r.coins_awarded,
      createdAt: r.created_at,
    }));
  const own: CoinTxn[] = a.coinTxns.map((x) => ({
    id: x.id,
    type: x.type,
    description: x.description,
    amount: x.amount,
    createdAt: x.created_at,
  }));
  return [...placed, ...reviews, ...own].sort((x, y) => y.createdAt.localeCompare(x.createdAt));
}

function mockSummary(): CoinSummaryDto {
  const d = coinsDb.get();
  const delta = sessionTxns().reduce((n, x) => n + x.amount, 0);
  const checked = isToday(act().checkedInAt);
  return {
    ...d,
    balance: Math.max(0, d.balance + delta),
    check_in: { ...d.check_in, checked_in_today: checked },
  };
}

export async function getCoins(): Promise<CoinSummary> {
  if (USE_MOCK) {
    await mockDelay(120);
    return toCoinSummary(mockSummary());
  }
  return toCoinSummary(await api.get<CoinSummaryDto>('/me/coins'));
}

export async function listCoinTxns(): Promise<CoinTxn[]> {
  if (USE_MOCK) {
    await mockDelay(150);
    return [...sessionTxns(), ...SEED_TXNS];
  }
  const r = await api.get<
    { id: number; type: string; description: string; amount: number; created_at: string }[]
  >('/me/coins/transactions', { query: { limit: 50 } });
  return r.map((x) => ({ ...x, createdAt: x.created_at }));
}

export async function listCoinRewards(): Promise<CoinReward[]> {
  if (USE_MOCK) return REWARDS;
  const r = await api.get<
    { id: number; coin_cost: number; voucher_value: number; min_order_value: number }[]
  >('/public/coin-rewards', { auth: false });
  return r.map((x) => ({
    id: x.id,
    cost: x.coin_cost,
    voucherValue: x.voucher_value,
    minOrder: x.min_order_value,
  }));
}

/** POST /me/coins/check-ins — đã điểm danh hôm nay → 409 GNE-PRO-4091. */
export async function checkIn(): Promise<{ reward: number }> {
  if (USE_MOCK) {
    await mockDelay(300);
    const a = act();
    if (isToday(a.checkedInAt)) mockFail('GNE-PRO-4091');
    const d = coinsDb.get();
    const day = d.check_in.streak_days + 1;
    const reward = rewardOf(
      {
        ...d.check_in,
        streakDays: d.check_in.streak_days,
        checkedInToday: false,
        nextCheckInAt: '',
        rewards: d.check_in.rewards,
      },
      day,
    );
    const now = new Date().toISOString();
    coinsDb.set({ ...d, check_in: { ...d.check_in, streak_days: day % 7 === 0 ? 7 : day % 7 } });
    activityStore.set({
      ...a,
      checkedInAt: now,
      coinTxns: [
        ...a.coinTxns,
        {
          id: Date.now(),
          type: 'CHECK_IN',
          description: 'Điểm danh hằng ngày',
          amount: reward,
          created_at: now,
        },
      ],
    });
    return { reward };
  }
  return api.post<{ reward: number }>('/me/coins/check-ins');
}

/** POST /me/coins/redemptions — không đủ Điểm → 422 GNE-PRO-4222. Voucher vào Kho Voucher. */
export async function redeemReward(r: CoinReward): Promise<{ code: string }> {
  if (USE_MOCK) {
    await mockDelay(300);
    if (mockSummary().balance < r.cost) mockFail('GNE-PRO-4222');
    const a = act();
    const code = `DIEM${r.voucherValue / 1000}K${a.redeemed.length + 1}`;
    const now = new Date().toISOString();
    activityStore.set({
      ...a,
      redeemed: [
        ...a.redeemed,
        {
          id: `r${a.redeemed.length + 1}`,
          code,
          value: r.voucherValue,
          cost: r.cost,
          created_at: now,
        },
      ],
      coinTxns: [
        ...a.coinTxns,
        {
          id: Date.now(),
          type: 'VOUCHER_REDEEM',
          description: `Đổi voucher ${code}`,
          amount: -r.cost,
          created_at: now,
        },
      ],
    });
    return { code };
  }
  const res = await api.post<{ voucher: { code: string } }>('/me/coins/redemptions', {
    reward_id: r.id,
  });
  return { code: res.voucher.code };
}
