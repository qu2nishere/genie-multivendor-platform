// Bộ chuyển lược đồ zod → resolver của react-hook-form (R8; không thêm @hookform/resolvers).
// Thông điệp lỗi giữ nguyên là khóa i18n — UI tự dịch.
import type { FieldErrors, FieldValues, Resolver } from 'react-hook-form';
import type { ZodType } from 'zod';

export function zodResolver<T extends FieldValues>(schema: ZodType<T>): Resolver<T> {
  return async (values) => {
    const r = schema.safeParse(values);
    if (r.success) return { values: r.data, errors: {} };
    const errors: Record<string, { type: string; message: string }> = {};
    for (const issue of r.error.issues) {
      const key = issue.path.join('.') || 'root';
      if (!errors[key]) errors[key] = { type: issue.code, message: issue.message };
    }
    return { values: {}, errors: errors as unknown as FieldErrors<T> };
  };
}
