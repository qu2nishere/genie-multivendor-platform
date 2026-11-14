// Lưu ở trình duyệt với tiền tố `genie.` (plan G04.06; LUU_TRU_DU_LIEU mục 6).
// An toàn khi SSR (không có window) và khi storage bị chặn / đầy (ẩn danh): không bao giờ ném lỗi.
// KHÔNG lưu: token, họ tên + SĐT + địa chỉ, CCCD, số tài khoản, OTP, giá / số dư làm căn cứ (6.7).

export const STORAGE_PREFIX = 'genie.';

/** Danh mục khóa (LUU_TRU_DU_LIEU 6.2) — chỉ dùng các khóa trong bảng này. */
export const STORAGE_KEYS = {
  cartGuest: { area: 'local', key: 'cart.guest' },
  checkoutDraft: { area: 'session', key: 'checkout.draft' },
  checkoutIdem: { area: 'session', key: 'checkout.idem' },
  popupSeen: { area: 'session', key: 'popup.seen' },
  theme: { area: 'local', key: 'theme' },
  wishlist: { area: 'local', key: 'wishlist' },
  searchRecent: { area: 'local', key: 'search.recent' },
  recentViewed: { area: 'local', key: 'recent.viewed' },
  uiSeller: { area: 'local', key: 'ui.seller' },
  uiAdmin: { area: 'local', key: 'ui.admin' },
} as const satisfies Record<string, { area: StorageArea; key: string }>;

export type StorageArea = 'local' | 'session';
export type StorageKeyName = keyof typeof STORAGE_KEYS;

function areaOf(kind: StorageArea): Storage | null {
  try {
    if (typeof window === 'undefined') return null;
    return kind === 'local' ? window.localStorage : window.sessionStorage;
  } catch {
    return null; // trình duyệt chặn truy cập storage
  }
}

const fullKey = (k: string) => STORAGE_PREFIX + k;

export function readRaw(kind: StorageArea, key: string): string | null {
  try {
    return areaOf(kind)?.getItem(fullKey(key)) ?? null;
  } catch {
    return null;
  }
}

export function writeRaw(kind: StorageArea, key: string, value: string): void {
  try {
    areaOf(kind)?.setItem(fullKey(key), value);
  } catch {
    /* đầy bộ nhớ / bị chặn: bỏ qua */
  }
}

export function removeRaw(kind: StorageArea, key: string): void {
  try {
    areaOf(kind)?.removeItem(fullKey(key));
  } catch {
    /* bỏ qua */
  }
}

/** Đọc JSON thuần (không phong bì). Lỗi phân tích → null. */
export function readValue<T>(kind: StorageArea, key: string): T | null {
  const raw = readRaw(kind, key);
  if (raw == null) return null;
  try {
    return JSON.parse(raw) as T;
  } catch {
    return null;
  }
}

export function writeValue(kind: StorageArea, key: string, value: unknown): void {
  writeRaw(kind, key, JSON.stringify(value));
}

/** Phong bì có phiên bản + hạn (6.5). */
export interface Versioned {
  version: number;
  expires_at?: string;
}

/** Khác phiên bản hoặc quá hạn → xóa khóa và trả null. */
export function readJson<T extends Versioned>(
  kind: StorageArea,
  key: string,
  version: number,
): T | null {
  const data = readValue<T>(kind, key);
  if (!data || typeof data !== 'object') return null;
  if (data.version !== version) {
    removeRaw(kind, key);
    return null;
  }
  if (data.expires_at && Date.parse(data.expires_at) < Date.now()) {
    removeRaw(kind, key);
    return null;
  }
  return data;
}

export function writeJson(kind: StorageArea, key: string, value: Versioned): void {
  writeValue(kind, key, value);
}

/** Đọc / ghi theo tên khóa trong danh mục. */
export const store = {
  get<T>(name: StorageKeyName): T | null {
    const { area, key } = STORAGE_KEYS[name];
    return readValue<T>(area, key);
  },
  set(name: StorageKeyName, value: unknown): void {
    const { area, key } = STORAGE_KEYS[name];
    writeValue(area, key, value);
  },
  remove(name: StorageKeyName): void {
    const { area, key } = STORAGE_KEYS[name];
    removeRaw(area, key);
  },
};

/** Gọi khi đăng xuất (6.6). Giữ lại cookie `lang`, `genie.theme`, `genie.ui.*`. */
export function clearUserData(): void {
  (['checkoutDraft', 'checkoutIdem', 'searchRecent', 'recentViewed'] as const).forEach((k) =>
    store.remove(k),
  );
}

/** Nghe tab khác đổi một khóa (sự kiện `storage`) — dùng cho mini-cart, theme, yêu thích. */
export function onStorageChange(name: StorageKeyName, cb: () => void): () => void {
  if (typeof window === 'undefined') return () => undefined;
  const target = fullKey(STORAGE_KEYS[name].key);
  const handler = (e: StorageEvent) => {
    if (e.key === target) cb();
  };
  window.addEventListener('storage', handler);
  return () => window.removeEventListener('storage', handler);
}
