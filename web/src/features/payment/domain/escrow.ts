// Đường đi của tiền người mua (DESIGN.md "Money Rail"): trả tiền → Genie giữ → nhận hàng → shop nhận.
export const MONEY_STEPS = ['pay', 'hold', 'receive', 'release'] as const;
export type MoneyStep = (typeof MONEY_STEPS)[number];

/** Vị trí đồng xu trên đường (phần trăm theo tâm từng ô của lưới 4 cột). */
export const coinLeftPct = (step: number): number => (step + 0.5) * (100 / MONEY_STEPS.length);

/** Độ dài phần đã đi của đường (đường nối tâm ô đầu → ô cuối). */
export const fillPct = (step: number): number => (step / (MONEY_STEPS.length - 1)) * 100;

/** Thời gian mỗi bước khi tự chạy (ms) — một lượt tới "Shop nhận tiền" rồi dừng. */
export const AUTO_STEP_MS = 3600;
