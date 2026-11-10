import { defineMiddleware } from 'astro:middleware';

// Đọc cookie lang, gắn locale; chặn /seller, /admin khi chưa đăng nhập — CHỈ để chuyển hướng cho tiện (UX).
// Server luôn kiểm @PreAuthorize (NFR-13).
export const onRequest = defineMiddleware(async (context, next) => {
  const lang = context.cookies.get('lang')?.value;
  context.locals.lang = lang === 'en' ? 'en' : 'vi';

  const { pathname } = context.url;
  const needsLogin = pathname.startsWith('/seller') || pathname.startsWith('/admin');
  // Refresh token nằm trong cookie HttpOnly của api domain; ở đây chỉ kiểm cờ đăng nhập phía web (TODO).
  if (needsLogin && !context.cookies.has('genie_logged_in')) {
    return context.redirect(`/auth/login?next=${encodeURIComponent(pathname)}`);
  }
  return next();
});
