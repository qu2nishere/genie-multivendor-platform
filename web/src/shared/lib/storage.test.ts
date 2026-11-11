import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import { clearUserData, readJson, store, STORAGE_PREFIX, writeJson } from './storage';

class MemoryStorage implements Storage {
  private m = new Map<string, string>();
  get length() {
    return this.m.size;
  }
  clear() {
    this.m.clear();
  }
  getItem(k: string) {
    return this.m.has(k) ? (this.m.get(k) as string) : null;
  }
  key(i: number) {
    return [...this.m.keys()][i] ?? null;
  }
  removeItem(k: string) {
    this.m.delete(k);
  }
  setItem(k: string, v: string) {
    this.m.set(k, v);
  }
}

describe('storage', () => {
  let local: MemoryStorage;
  let session: MemoryStorage;
  beforeEach(() => {
    local = new MemoryStorage();
    session = new MemoryStorage();
    Reflect.set(globalThis, 'window', {
      localStorage: local,
      sessionStorage: session,
      addEventListener: () => undefined,
      removeEventListener: () => undefined,
    });
  });
  afterEach(() => {
    Reflect.deleteProperty(globalThis, 'window');
  });

  it('ghi với tiền tố genie. và đúng vùng lưu', () => {
    store.set('theme', 'hoian');
    store.set('popupSeen', '1');
    expect(local.getItem(STORAGE_PREFIX + 'theme')).toBe('"hoian"');
    expect(session.getItem('genie.popup.seen')).toBe('"1"');
    expect(store.get<string>('theme')).toBe('hoian');
  });

  it('readJson bỏ dữ liệu khác phiên bản hoặc quá hạn', () => {
    writeJson('local', 'cart.guest', { version: 1, expires_at: '2999-01-01T00:00:00Z' });
    expect(readJson('local', 'cart.guest', 1)).not.toBeNull();
    expect(readJson('local', 'cart.guest', 2)).toBeNull();
    expect(local.getItem('genie.cart.guest')).toBeNull();
    writeJson('local', 'cart.guest', { version: 1, expires_at: '2000-01-01T00:00:00Z' });
    expect(readJson('local', 'cart.guest', 1)).toBeNull();
  });

  it('JSON hỏng → null, không ném lỗi', () => {
    local.setItem('genie.wishlist', '{hỏng');
    expect(store.get('wishlist')).toBeNull();
  });

  it('clearUserData giữ theme, xóa nháp checkout', () => {
    store.set('theme', 'genie');
    store.set('checkoutDraft', { a: 1 });
    clearUserData();
    expect(store.get('theme')).toBe('genie');
    expect(store.get('checkoutDraft')).toBeNull();
  });

  it('không có window (SSR) → không lỗi', () => {
    Reflect.deleteProperty(globalThis, 'window');
    expect(() => store.set('theme', 'x')).not.toThrow();
    expect(store.get('theme')).toBeNull();
  });
});
