// API công khai của feature "payment" (mở rộng ở G10.23–G10.29).
export { MONEY_STEPS, coinLeftPct, fillPct, type MoneyStep } from './domain/escrow';
export { default as MoneyRail, type MoneyRailProps } from './ui/MoneyRail';
export { HoldNote } from './ui/HoldNote';
