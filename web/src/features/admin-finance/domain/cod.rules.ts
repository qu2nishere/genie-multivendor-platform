// Đối soát COD (API_SPEC 11.7; prototype CodReconciliation.jsx parseCsv / matchLine / RESULT) — plan
// G12.29. Client đọc CSV 3 cột (mã vận đơn, số tiền, phí thu hộ) thành JSON rồi gửi preview; server
// đối chiếu. Hàm đối chiếu ở đây dùng cho mock và kiểm tra trước khi gửi.
import type { Reconciliation, ReconciledLine, StatementLine } from './finance.types';

const cell = (c: string) =>
  c
    .replace(/^\uFEFF/, '')
    .replace(/^"|"$/g, '')
    .trim();
const num = (c: string | undefined) => {
  const n = Number((c ?? '').replace(/[.\s₫]/g, '').replace(',', '.'));
  return Number.isFinite(n) ? n : NaN;
};

/**
 * Đọc bảng kê CSV: bỏ dòng tiêu đề (nếu cột 2 không phải số), bỏ dòng trống; dòng sai định dạng
 * (thiếu mã / số tiền không phải số) được trả trong `errors` theo số dòng (đếm từ 1).
 */
export function parseCodCsv(text: string): { lines: StatementLine[]; errors: number[] } {
  const rows = text
    .split(/\r?\n/)
    .map((l) => l.split(',').map(cell))
    .map((cols, i) => ({ cols, n: i + 1 }))
    .filter(({ cols }) => cols.some(Boolean));
  const first = rows[0];
  const body = first && Number.isNaN(num(first.cols[1])) ? rows.slice(1) : rows;
  const lines: StatementLine[] = [];
  const errors: number[] = [];
  for (const { cols, n } of body) {
    const [code, amount, fee] = cols;
    const a = num(amount);
    const f = fee === undefined || fee === '' ? 0 : num(fee);
    if (!code || Number.isNaN(a) || Number.isNaN(f) || a < 0 || f < 0) errors.push(n);
    else lines.push({ trackingCode: code, amount: a, fee: f });
  }
  return { lines, errors };
}

/** Đối chiếu từng dòng với vận đơn COD đã thu (tra theo mã vận đơn). */
export function matchLine(
  line: StatementLine,
  lookup: (code: string) => { orderCode: string; amount: number } | null,
): ReconciledLine {
  const hit = lookup(line.trackingCode);
  if (!hit) return { ...line, orderCode: null, orderAmount: null, result: 'NOT_FOUND' };
  return {
    ...line,
    orderCode: hit.orderCode,
    orderAmount: hit.amount,
    result: hit.amount === line.amount ? 'MATCH' : 'AMOUNT_MISMATCH',
  };
}

/** Tổng bảng kê (tiền − phí) phải bằng số tiền SePay báo đã nhận; xác nhận khi cân và có ≥ 1 dòng khớp. */
export function reconcile(
  lines: StatementLine[],
  receivedAmount: number,
  lookup: (code: string) => { orderCode: string; amount: number } | null,
): Reconciliation {
  const out = lines.map((l) => matchLine(l, lookup));
  const net = lines.reduce((n, l) => n + l.amount - l.fee, 0);
  return {
    lines: out,
    totals: {
      statementNet: net,
      receivedAmount,
      matchedCount: out.filter((l) => l.result === 'MATCH').length,
      balanced: lines.length > 0 && net === receivedAmount,
    },
  };
}

export const canConfirm = (r: Reconciliation | null): boolean =>
  !!r && r.totals.balanced && r.totals.matchedCount > 0;
