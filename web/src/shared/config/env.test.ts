import { describe, expect, it } from 'vitest';
import { env } from './env';

// Test mẫu (G01.02): bảo đảm npm run test không rỗng và env có giá trị mặc định hợp lệ
describe('env', () => {
  it('có PUBLIC_API_URL hợp lệ và cờ mock là boolean', () => {
    expect(() => new URL(env.PUBLIC_API_URL)).not.toThrow();
    expect(typeof env.PUBLIC_USE_MOCK).toBe('boolean');
  });
});
