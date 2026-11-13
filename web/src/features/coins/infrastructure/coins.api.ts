// Điểm Genie (API_SPEC 3.13). Mock giữ trong phiên tab (mockState).
import { api } from '@/shared/api/http';
import { mockDelay, mockState, USE_MOCK } from '@/shared/api/mock';
import { placedStore } from '@/shared/api/mock/placed';
import type { CoinSummary } from '../domain/coins.types';
import type { CoinSummaryDto } from './coins.dto';
import { toCoinSummary } from './coins.mapper';
import { seedCoinSummary } from './coins.mock';

export const coinsDb = mockState<CoinSummaryDto>('coins', seedCoinSummary);

export async function getCoins(): Promise<CoinSummary> {
  if (USE_MOCK) {
    await mockDelay(120);
    const used = placedStore.get().sessions.reduce((n, x) => n + x.coin_discount, 0);
    const d = coinsDb.get();
    return toCoinSummary({ ...d, balance: Math.max(0, d.balance - used) });
  }
  return toCoinSummary(await api.get<CoinSummaryDto>('/me/coins'));
}
