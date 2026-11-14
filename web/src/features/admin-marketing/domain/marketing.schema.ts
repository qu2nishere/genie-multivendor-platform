// Form Flash Sale, banner, thông báo sàn (zod; API 11.15–11.17). Form voucher sàn dùng lược đồ chung
// của seller-promotions (`voucherSchemaFor('platform')`).
import { z } from 'zod';
import { BANNER_POSITIONS, plainText, TITLE_MAX } from './marketing.rules';

export const flashSchema = z
  .object({
    name: z
      .string()
      .trim()
      .min(1, 'admin-marketing.flash.err.name')
      .max(100, 'admin-marketing.flash.err.name_long'),
    startsAt: z.string().min(1, 'admin-marketing.flash.err.time'),
    endsAt: z.string().min(1, 'admin-marketing.flash.err.time'),
    maxItemsPerShop: z
      .number({ invalid_type_error: 'admin-marketing.flash.err.max' })
      .int()
      .min(1, 'admin-marketing.flash.err.max')
      .max(100, 'admin-marketing.flash.err.max'),
    registrationDeadline: z.string().min(1, 'admin-marketing.flash.err.deadline'),
  })
  .refine((v) => !v.startsAt || !v.endsAt || v.endsAt > v.startsAt, {
    path: ['endsAt'],
    message: 'admin-marketing.flash.err.order',
  })
  .refine(
    (v) =>
      !v.startsAt || !v.registrationDeadline || v.registrationDeadline <= v.startsAt.slice(0, 10),
    {
      path: ['registrationDeadline'],
      message: 'admin-marketing.flash.err.deadline_after',
    },
  );
export type FlashValues = z.infer<typeof flashSchema>;

export const bannerSchema = z
  .object({
    title: z
      .string()
      .trim()
      .min(1, 'admin-marketing.banner.err.title')
      .max(120, 'admin-marketing.banner.err.title'),
    hasImage: z.boolean().refine((x) => x, 'admin-marketing.banner.err.image'),
    targetUrl: z.string().trim().min(1, 'admin-marketing.banner.err.link'),
    position: z.enum(BANNER_POSITIONS as [string, ...string[]]),
    categoryId: z.number().nullable(),
    sortOrder: z.number().int().min(1).max(999),
    startsAt: z.string().min(1, 'admin-marketing.banner.err.date'),
    endsAt: z.string().min(1, 'admin-marketing.banner.err.date'),
    active: z.boolean(),
  })
  .refine((v) => !v.startsAt || !v.endsAt || v.endsAt > v.startsAt, {
    path: ['endsAt'],
    message: 'admin-marketing.banner.err.date_order',
  })
  .refine((v) => v.position !== 'CATEGORY_TOP' || v.categoryId !== null, {
    path: ['categoryId'],
    message: 'admin-marketing.banner.err.category',
  });
export type BannerValues = z.infer<typeof bannerSchema>;

export const notificationSchema = z
  .object({
    target: z.enum(['ALL', 'CUSTOMER', 'VENDOR', 'SPECIFIC_USERS']),
    emailsText: z.string(),
    title: z
      .string()
      .trim()
      .min(1, 'admin-marketing.notif.err.title')
      .max(TITLE_MAX, 'admin-marketing.notif.err.title_long'),
    contentHtml: z
      .string()
      .refine((h) => plainText(h).length > 0, 'admin-marketing.notif.err.content'),
    sendAt: z.string(),
  })
  .refine((v) => v.target !== 'SPECIFIC_USERS' || v.emailsText.trim().length > 0, {
    path: ['emailsText'],
    message: 'admin-marketing.notif.err.emails',
  });
export type NotificationValues = z.infer<typeof notificationSchema>;
