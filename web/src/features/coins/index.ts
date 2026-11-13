// API công khai của feature "coins" — nơi DUY NHẤT feature khác được import.
export type { CoinSummary } from './domain/coins.types';
export { getCoins } from './infrastructure/coins.api';
export { useCoins, COINS_KEY } from './application/useCoins';
