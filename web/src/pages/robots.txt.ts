// robots.txt (plan G08.45) — sinh động để dòng Sitemap là URL tuyệt đối theo tên miền đang chạy.
// Chặn khu vực người bán / quản trị / tài khoản / thanh toán; trang tìm kiếm đã noindex, follow.
import type { APIRoute } from 'astro';
import { env } from '@/shared/config/env';

const DISALLOW = [
  '/seller',
  '/admin',
  '/account',
  '/checkout',
  '/cart',
  '/orders',
  '/chat',
  '/auth',
  '/dev',
];

export const GET: APIRoute = ({ site }) => {
  const base = (site?.toString() ?? env.PUBLIC_WEB_URL).replace(/\/$/, '');
  const body = [
    'User-agent: *',
    'Allow: /',
    ...DISALLOW.map((p) => `Disallow: ${p}`),
    '',
    `Sitemap: ${base}/sitemap.xml`,
    '',
  ].join('\n');
  return new Response(body, { headers: { 'Content-Type': 'text/plain; charset=utf-8' } });
};
