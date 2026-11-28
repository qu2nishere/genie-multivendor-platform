// Tự lưu bản nhập dở của form đăng bán vào localStorage (yêu cầu chủ dự án 2026-11-27): lưu sau 1 giây ngừng gõ,
// F5 / mở lại form thì TỰ đổ lại dữ liệu (không hỏi). Luật (hạn, version, tệp tạm hết hạn) ở domain/product.draft.ts.
import { useCallback, useEffect, useRef } from 'react';
import type { UseFormReturn } from 'react-hook-form';
import { currentUser } from '@/shared/auth/session';
import { readValue, removeRaw, writeValue } from '@/shared/lib/storage';
import {
  DRAFT_SCHEMA,
  draftKey,
  draftUsable,
  dropExpiredUploads,
  hasContent,
  type ProductDraft,
} from '../domain/product.draft';
import type { ProductForm } from '../domain/product.types';

const SAVE_DELAY_MS = 1000;
const THUMB_MAX = 240;

/**
 * Ảnh vừa chọn chỉ có URL `blob:` (mất sau F5) → thu nhỏ thành data URL JPEG (~10–20 KB) để bản lưu tạm vẫn
 * hiện được ảnh xem trước. Lỗi → null (ô ảnh hiện trống, khóa tệp vẫn giữ để gửi lên server).
 */
async function blobToThumb(url: string): Promise<string | null> {
  try {
    const bmp = await createImageBitmap(await (await fetch(url)).blob());
    const scale = Math.min(1, THUMB_MAX / Math.max(bmp.width, bmp.height));
    const canvas = document.createElement('canvas');
    canvas.width = Math.round(bmp.width * scale);
    canvas.height = Math.round(bmp.height * scale);
    canvas.getContext('2d')?.drawImage(bmp, 0, 0, canvas.width, canvas.height);
    bmp.close();
    return canvas.toDataURL('image/jpeg', 0.75);
  } catch {
    return null;
  }
}

export interface ProductDraftApi {
  /** Xóa bản nhập dở và ngừng tự lưu — sau khi lưu thành công / người bán chủ động bỏ thay đổi. */
  clear: () => void;
}

export function useProductDraft(
  form: UseFormReturn<ProductForm>,
  opts: {
    productId: number | null;
    productVersion: number | null;
    enabled: boolean;
    /** Khôi phục mà phải bỏ ảnh / tệp đã hết hạn trên kho → báo người bán tải lại. */
    onExpiredMedia?: () => void;
  },
): ProductDraftApi {
  const { productId, productVersion, enabled, onExpiredMedia } = opts;
  const userId = currentUser()?.id ?? null;
  const key = userId === null ? null : draftKey(userId, productId);
  const thumbs = useRef(new Map<string, string | null>());
  const timer = useRef<ReturnType<typeof setTimeout> | null>(null);
  // chưa đổ xong bản cũ vào form thì chưa ghi (tránh ghi đè bản cũ bằng form trống vừa mở)
  const decided = useRef(false);
  const expiredCb = useRef(onExpiredMedia);
  expiredCb.current = onExpiredMedia;
  // đã lưu lên server / người bán chủ động bỏ thay đổi → ngừng tự lưu (tránh ghi lại khi form reset / rời trang)
  const stopped = useRef(false);

  // Mở form (kể cả sau F5): có bản nhập dở hợp lệ → tự đổ lại vào form; bản hỏng / hết hạn / khác version → xóa
  useEffect(() => {
    if (!enabled || !key) {
      decided.current = true;
      return;
    }
    const d = readValue<ProductDraft>('local', key);
    if (draftUsable(d, Date.now(), productVersion)) {
      const { form: f, dropped } = dropExpiredUploads(d.form, d.savedAt, Date.now());
      // setValue từng khối, đánh dấu đã sửa → cảnh báo rời trang / nút lưu hoạt động như khi tự nhập
      (Object.keys(f) as (keyof ProductForm)[]).forEach((k) =>
        form.setValue(k, f[k] as never, { shouldDirty: true }),
      );
      if (dropped) expiredCb.current?.();
    } else if (d) {
      removeRaw('local', key);
    }
    decided.current = true;
  }, [enabled, key, productVersion, form]);

  const persist = useCallback(() => {
    if (!key || !decided.current || stopped.current) return;
    const f = form.getValues();
    if (!hasContent(f)) {
      removeRaw('local', key);
      return;
    }
    const pending: string[] = [];
    const thumb = (url: string | null): string | null => {
      if (!url || !url.startsWith('blob:')) return url;
      if (!thumbs.current.has(url)) pending.push(url);
      return thumbs.current.get(url) ?? null;
    };
    const draft: ProductDraft = {
      schema: DRAFT_SCHEMA,
      savedAt: Date.now(),
      productVersion,
      form: {
        ...f,
        images: f.images.map((i) => ({ ...i, url: thumb(i.url) ?? '' })),
        groups: f.groups.map((g) => ({
          ...g,
          values: g.values.map((v) => ({ ...v, imageUrl: thumb(v.imageUrl) })),
        })),
      },
    };
    writeValue('local', key, draft);
    // ảnh mới: thu nhỏ xong thì ghi lại một lần nữa để bản lưu có ảnh xem trước
    if (pending.length) {
      pending.forEach((u) => thumbs.current.set(u, null));
      void Promise.all(pending.map(async (u) => thumbs.current.set(u, await blobToThumb(u)))).then(
        () => persist(),
      );
    }
  }, [form, key, productVersion]);

  // Theo dõi mọi thay đổi của form → lưu sau 1 giây ngừng gõ; đóng / rời trang thì lưu ngay
  useEffect(() => {
    if (!enabled || !key) return undefined;
    const sub = form.watch(() => {
      if (timer.current) clearTimeout(timer.current);
      timer.current = setTimeout(persist, SAVE_DELAY_MS);
    });
    const flush = () => {
      if (timer.current) {
        clearTimeout(timer.current);
        timer.current = null;
        persist();
      }
    };
    window.addEventListener('pagehide', flush);
    return () => {
      sub.unsubscribe();
      window.removeEventListener('pagehide', flush);
      flush();
    };
  }, [enabled, key, form, persist]);

  const clear = useCallback(() => {
    if (timer.current) clearTimeout(timer.current);
    timer.current = null;
    if (key) removeRaw('local', key);
    stopped.current = true;
  }, [key]);

  return { clear };
}
