// Định dạng tiền / ngày / số bằng Intl (plan G04.02, G04.03; TS 5.4, NFR-14).
// Mọi chỗ hiển thị tiền, ngày phải đi qua file này — không tự nối "đ" hay gọi toLocaleString.
import { currentLocale, intlTag, type Locale } from './locale';

export const TIME_ZONE = 'Asia/Ho_Chi_Minh';
export const VND_SYMBOL = '₫';

interface Opts {
  locale?: Locale;
}

const cache = new Map<string, Intl.NumberFormat>();
const numberFormat = (tag: string, o?: Intl.NumberFormatOptions): Intl.NumberFormat => {
  const key = tag + JSON.stringify(o ?? {});
  let f = cache.get(key);
  if (!f) {
    f = new Intl.NumberFormat(tag, o);
    cache.set(key, f);
  }
  return f;
};

const tagOf = (o: Opts) => intlTag(o.locale ?? currentLocale());

/** Số nguyên VND → "150.000₫" (vi) / "150,000₫" (en). Làm tròn về đồng (prototype formatVND). */
export function formatVND(amount: number, opts: Opts & { symbol?: boolean } = {}): string {
  const n = Math.round(Number(amount) || 0);
  const grouped = numberFormat(tagOf(opts)).format(n);
  return opts.symbol === false ? grouped : grouped + VND_SYMBOL;
}

/** Tách [phần số, ký hiệu] để component tô ₫ riêng (prototype partsVND). */
export function partsVND(amount: number, opts: Opts = {}): [string, string] {
  return [formatVND(amount, { ...opts, symbol: false }), VND_SYMBOL];
}

/** Số thường có phân nhóm: 1234567 → "1.234.567". */
export function formatNumber(n: number, opts: Opts = {}): string {
  return numberFormat(tagOf(opts)).format(n);
}

/** Phần trăm: 0.125 → "13%" (digits 0) / "12,5%" (digits 1). */
export function formatPercent(ratio: number, opts: Opts & { digits?: number } = {}): string {
  return numberFormat(tagOf(opts), {
    style: 'percent',
    maximumFractionDigits: opts.digits ?? 0,
  }).format(ratio);
}

/**
 * Số gọn kiểu "đã bán": 1200 → "1,2k" (vi) / "1.2k" (en); 2_500_000 → "2,5tr" / "2.5M".
 * Thay chuỗi "1,2k" ghi cứng trong data.jsx của prototype.
 */
export function formatCompact(n: number, opts: Opts = {}): string {
  const l = opts.locale ?? currentLocale();
  const tag = intlTag(l);
  const abs = Math.abs(n);
  const one = (v: number) => numberFormat(tag, { maximumFractionDigits: 1 }).format(v);
  if (abs >= 1_000_000) return one(n / 1_000_000) + (l === 'vi' ? 'tr' : 'M');
  if (abs >= 1_000) return one(n / 1_000) + 'k';
  return numberFormat(tag).format(n);
}

/**
 * Tiền rút gọn cho trục biểu đồ / chỗ hẹp (prototype dash.jsx short):
 * vi "1,84 tỷ" / "45,8 tr" / "950k"; en "1.84B" / "45.8M" / "950k".
 */
export function formatMoneyShort(n: number, opts: Opts = {}): string {
  const l = opts.locale ?? currentLocale();
  const tag = intlTag(l);
  const trim = (v: number, d: number) => numberFormat(tag, { maximumFractionDigits: d }).format(v);
  const a = Math.abs(n);
  if (a >= 1e9) return trim(n / 1e9, 2) + (l === 'vi' ? ' tỷ' : 'B');
  if (a >= 1e6) return trim(n / 1e6, 1) + (l === 'vi' ? ' tr' : 'M');
  if (a >= 1e3) return trim(n / 1e3, 0) + 'k';
  return trim(n, 0);
}

/**
 * Đọc ngược chuỗi gọn ("1,2k", "980", "2.5M", "1.250") → số.
 * Thay parseSold lặp ở Category / Search / Shop của prototype.
 */
export function parseCompact(s: string | number): number {
  if (typeof s === 'number') return s;
  const m = String(s)
    .trim()
    .match(/^([\d.,]+)\s*(k|tr|m)?$/i);
  if (!m) return 0;
  const unit = (m[2] ?? '').toLowerCase();
  let digits = m[1];
  if (unit) {
    // có đơn vị: dấu . hoặc , là thập phân ("1,2k", "2.5M")
    digits = digits.replace(',', '.');
  } else {
    // không đơn vị: . hoặc , theo sau đúng 3 chữ số là phân nhóm ("1.250", "1,250")
    digits = digits.replace(/[.,](?=\d{3}(?:\D|$))/g, '').replace(',', '.');
  }
  const n = parseFloat(digits);
  if (Number.isNaN(n)) return 0;
  if (unit === 'k') return n * 1_000;
  if (unit === 'tr' || unit === 'm') return n * 1_000_000;
  return n;
}

export type DateInput = Date | string | number;
const toDate = (d: DateInput): Date => (d instanceof Date ? d : new Date(d));

/** "08/06/2026" (vi) / "06/08/2026" (en), theo giờ Việt Nam. */
export function formatDate(d: DateInput, opts: Opts = {}): string {
  return new Intl.DateTimeFormat(tagOf(opts), {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    timeZone: TIME_ZONE,
  }).format(toDate(d));
}

/** "14:05 08/06/2026" (vi) / "06/08/2026, 2:05 PM" (en). */
export function formatDateTime(d: DateInput, opts: Opts = {}): string {
  const l = opts.locale ?? currentLocale();
  const date = toDate(d);
  if (l === 'vi') {
    const time = new Intl.DateTimeFormat('vi-VN', {
      hour: '2-digit',
      minute: '2-digit',
      hourCycle: 'h23',
      timeZone: TIME_ZONE,
    }).format(date);
    return `${time} ${formatDate(date, { locale: l })}`;
  }
  return new Intl.DateTimeFormat('en-US', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: 'numeric',
    minute: '2-digit',
    timeZone: TIME_ZONE,
  }).format(date);
}

const UNITS: [Intl.RelativeTimeFormatUnit, number][] = [
  ['year', 365 * 24 * 3600],
  ['month', 30 * 24 * 3600],
  ['week', 7 * 24 * 3600],
  ['day', 24 * 3600],
  ['hour', 3600],
  ['minute', 60],
];

/** "3 phút trước" / "3 minutes ago"; dưới 45 giây → "vừa xong" / "just now". */
export function formatRelative(d: DateInput, opts: Opts & { now?: number } = {}): string {
  const l = opts.locale ?? currentLocale();
  const now = opts.now ?? Date.now();
  const diff = Math.round((toDate(d).getTime() - now) / 1000);
  if (Math.abs(diff) < 45) return l === 'vi' ? 'vừa xong' : 'just now';
  const rtf = new Intl.RelativeTimeFormat(intlTag(l), { numeric: 'auto' });
  for (const [unit, sec] of UNITS) {
    if (Math.abs(diff) >= sec) return rtf.format(Math.round(diff / sec), unit);
  }
  return rtf.format(Math.round(diff / 60), 'minute');
}

/** Giây → "mm:ss" (đếm ngược OTP, giữ chỗ 15 phút). */
export function formatMmSs(totalSeconds: number): string {
  const s = Math.max(0, Math.floor(totalSeconds));
  return `${String(Math.floor(s / 60)).padStart(2, '0')}:${String(s % 60).padStart(2, '0')}`;
}

/** Giây → "hh:mm:ss" (Flash Sale đếm đến nửa đêm). */
export function formatHhMmSs(totalSeconds: number): string {
  const s = Math.max(0, Math.floor(totalSeconds));
  const h = Math.floor(s / 3600);
  const m = Math.floor((s % 3600) / 60);
  return [h, m, s % 60].map((v) => String(v).padStart(2, '0')).join(':');
}
