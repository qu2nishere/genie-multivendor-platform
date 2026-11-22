import { beforeEach, describe, expect, it } from 'vitest';
import { ApiError } from '@/shared/api/errors';
import {
  activateStaff,
  login,
  register,
  requestOtp,
  requestStaffActivationOtp,
  resetPassword,
  verifyOtp,
} from './auth.api';
import { resetAuthMock } from './auth.mock';

const fail = async (p: Promise<unknown>): Promise<ApiError> => {
  try {
    await p;
  } catch (e) {
    return e as ApiError;
  }
  throw new Error('không ném lỗi');
};

describe('auth.api (mock)', () => {
  beforeEach(() => resetAuthMock());

  it('đăng nhập đúng mật khẩu trả phiên', async () => {
    const s = await login('mai.tran@gmail.com', 'Genie@123');
    expect(s.accessToken).toBeTruthy();
    expect(s.user.email).toBe('mai.tran@gmail.com');
  });

  it('sai mật khẩu báo số lần còn lại, lần thứ 5 khóa (GNE-AUTH-4230)', async () => {
    const e1 = await fail(login('a@b.vn', 'x'));
    expect(e1.code).toBe('GNE-AUTH-4011');
    expect(e1.details.remaining_attempts).toBe(4);
    for (let i = 0; i < 3; i++) await fail(login('a@b.vn', 'x'));
    const locked = await fail(login('a@b.vn', 'x'));
    expect(locked.code).toBe('GNE-AUTH-4230');
    expect(locked.details.locked_by).toBe('PASSWORD');
    // đang khóa thì đúng mật khẩu cũng bị từ chối
    expect((await fail(login('a@b.vn', 'Genie@123'))).code).toBe('GNE-AUTH-4230');
  });

  it('OTP sai trừ lượt, sai lần 5 → GNE-AUTH-4221; OTP mở khóa đăng nhập lại được', async () => {
    const c = await requestOtp('b@b.vn', 'ACCOUNT_UNLOCK');
    const e = await fail(verifyOtp(c.id, '000000'));
    expect(e.code).toBe('GNE-AUTH-4222');
    expect(e.details.remaining_attempts).toBe(4);
    const ok = await verifyOtp(c.id, '123456');
    expect(ok.kind).toBe('session');

    const c2 = await requestOtp('b@b.vn', 'LOGIN');
    for (let i = 0; i < 4; i++) await fail(verifyOtp(c2.id, '000000'));
    expect((await fail(verifyOtp(c2.id, '000000'))).code).toBe('GNE-AUTH-4221');
  });

  it('đăng ký email đã có → GNE-AUTH-4091; quên mật khẩu → reset_token; mật khẩu trùng → 4223', async () => {
    expect(
      (
        await fail(
          register({
            name: 'A',
            email: 'mai.tran@gmail.com',
            phone: '0901234567',
            password: 'Abc@12345',
          }),
        )
      ).code,
    ).toBe('GNE-AUTH-4091');
    const c = await requestOtp('mai.tran@gmail.com', 'PASSWORD_RESET');
    const r = await verifyOtp(c.id, '123456');
    expect(r.kind).toBe('reset');
    if (r.kind === 'reset') {
      expect((await fail(resetPassword(r.resetToken, 'Genie@123'))).code).toBe('GNE-AUTH-4223');
      await expect(resetPassword(r.resetToken, 'Moi@20260')).resolves.toBeUndefined();
    }
  });

  it('kích hoạt nhân viên: xin OTP → OTP đúng + mật khẩu → phiên; dùng lại lời mời → GNE-AUTH-4103', async () => {
    const c = await requestStaffActivationOtp('inv_moi-1');
    expect(c.purpose).toBe('STAFF_ACTIVATION');
    expect(c.emailMasked).toContain('@genie.example');
    const wrong = await fail(
      activateStaff({
        invitationToken: 'inv_moi-1',
        challengeId: c.id,
        otp: '000000',
        password: 'Staff@2026',
      }),
    );
    expect(wrong.code).toBe('GNE-AUTH-4222');
    const s = await activateStaff({
      invitationToken: 'inv_moi-1',
      challengeId: c.id,
      otp: '123456',
      password: 'Staff@2026',
    });
    expect(s.accessToken).toBeTruthy();
    expect((await fail(requestStaffActivationOtp('inv_moi-1'))).code).toBe('GNE-AUTH-4103');
  });

  it('lời mời hết hạn hoặc sai dạng → GNE-AUTH-4103', async () => {
    expect((await fail(requestStaffActivationOtp('inv_expired'))).code).toBe('GNE-AUTH-4103');
    expect((await fail(requestStaffActivationOtp('abc'))).code).toBe('GNE-AUTH-4103');
  });
});
