import { AsyncLocalStorage } from 'node:async_hooks';
import { defineMiddleware } from 'astro:middleware';
import { LOGGED_IN_COOKIE } from '@/shared/auth/session';
import { LANG_COOKIE, registerServerLocale, toLocale, type Locale } from '@/shared/lib/locale';

// Middleware (plan G07.10; TS 2.4, 5.4):
// 1. Đọc cookie `lang` → locals.lang; mọi t() / format khi SSR lấy đúng ngôn ngữ của request này
//    qua AsyncLocalStorage (không dùng biến toàn cục — tránh lẫn giữa các request song song).
// 2. Chuyển hướng tới /auth/login?next= khi chưa có cờ đăng nhập — CHỈ để tiện cho người dùng (UX);
//    server luôn kiểm @PreAuthorize (NFR-13).

const requestLocale = new AsyncLocalStorage<Locale>();
registerServerLocale(() => requestLocale.getStore());

/** Trang cần đăng nhập. Giỏ hàng cho phép khách vãng lai (giỏ khách, LUU_TRU_DU_LIEU 6.3). */
const PROTECTED = ['/seller', '/admin', '/checkout', '/orders', '/account', '/chat'];

export const needsLogin = (pathname: string): boolean =>
  PROTECTED.some((p) => pathname === p || pathname.startsWith(`${p}/`)) &&
  // Trang kết quả thanh toán chỉ hiển thị theo status, cổng trả về có thể chưa còn phiên
  !pathname.startsWith('/checkout/result');

export const onRequest = defineMiddleware((context, next) => {
  const lang = toLocale(context.cookies.get(LANG_COOKIE)?.value);
  context.locals.lang = lang;

  const { pathname, search } = context.url;
  if (needsLogin(pathname) && !context.cookies.has(LOGGED_IN_COOKIE)) {
    return context.redirect(`/auth/login?next=${encodeURIComponent(pathname + search)}`, 302);
  }
  return requestLocale.run(lang, () => next());
});
