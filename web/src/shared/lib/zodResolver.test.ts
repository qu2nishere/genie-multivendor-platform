import { describe, expect, it } from 'vitest';
import { z } from 'zod';
import { zodResolver } from './zodResolver';

const schema = z.object({
  name: z.string().min(1, 'err.name'),
  age: z.number().min(18, 'err.age'),
});
const run = (v: unknown) =>
  zodResolver(schema)(v as z.infer<typeof schema>, undefined, {
    fields: {},
    shouldUseNativeValidation: false,
  });

describe('zodResolver', () => {
  it('hợp lệ → trả values', async () => {
    expect(await run({ name: 'A', age: 20 })).toEqual({
      values: { name: 'A', age: 20 },
      errors: {},
    });
  });
  it('lỗi → khóa i18n theo trường (lấy lỗi đầu)', async () => {
    const r = await run({ name: '', age: 1 });
    expect(r.errors).toMatchObject({ name: { message: 'err.name' }, age: { message: 'err.age' } });
  });
});
