// Hồ sơ & bảo mật (API_SPEC 3.2–3.7). Thao tác 🔐 gửi kèm X-OTP-Challenge-Id + X-OTP-Code (0.7).
// Mock: mã OTP 123456, mật khẩu hiện tại Genie@123, email đã có chủ an.nguyen@gmail.com; giữ trong phiên tab.
import { api } from '@/shared/api/http';
import { mockDelay, mockFail, mockState, USE_MOCK } from '@/shared/api/mock';
import { placedStore } from '@/shared/api/mock/placed';
import { MOCK_USER } from '@/shared/auth/session';
import { maskEmail } from '@/shared/lib/mask';
import { readValue } from '@/shared/lib/storage';
import type {
  DeletionRequest,
  Device,
  Gender,
  LoginMethods,
  Profile,
  ProfilePatch,
  StepUpChallenge,
  StepUpProof,
  StepUpPurpose,
} from '../domain/account.types';

// ---- DTO (snake_case) ----
interface ProfileDto {
  full_name: string;
  email: string;
  email_verified: boolean;
  phone_number: string | null;
  gender: Gender | null;
  date_of_birth: string | null;
  bio: string | null;
  preferred_language: 'vi' | 'en';
  avatar_url: string | null;
  deletion_request: { requested_at: string; anonymize_at: string } | null;
}
interface LoginMethodsDto {
  email: string;
  password: { enabled: boolean };
  identities: { provider: 'GOOGLE' | 'APPLE'; email: string; linked_at: string }[];
}
interface SessionDto {
  id: number;
  device_name: string;
  device_type: 'DESKTOP' | 'MOBILE' | 'TABLET';
  location: string;
  ip_address: string;
  last_active_at: string;
  is_current: boolean;
}
interface ChallengeDto {
  challenge_id: string;
  masked_target: string;
  expires_at: string;
  resend_available_at: string;
}

const toProfile = (d: ProfileDto): Profile => ({
  fullName: d.full_name,
  email: d.email,
  emailVerified: d.email_verified,
  phone: d.phone_number,
  gender: d.gender,
  dateOfBirth: d.date_of_birth,
  bio: d.bio,
  language: d.preferred_language,
  avatarUrl: d.avatar_url,
  deletion: d.deletion_request && {
    requestedAt: d.deletion_request.requested_at,
    anonymizeAt: d.deletion_request.anonymize_at,
  },
});
const toMethods = (d: LoginMethodsDto): LoginMethods => ({
  email: d.email,
  password: d.password.enabled,
  identities: d.identities.map((i) => ({
    provider: i.provider,
    email: i.email,
    linkedAt: i.linked_at,
  })),
});
const toDevice = (d: SessionDto): Device => ({
  id: d.id,
  name: d.device_name,
  type: d.device_type,
  location: d.location,
  ip: d.ip_address,
  lastActiveAt: d.last_active_at,
  current: d.is_current,
});
const toChallenge = (d: ChallengeDto): StepUpChallenge => ({
  id: d.challenge_id,
  maskedTarget: d.masked_target,
  expiresAt: d.expires_at,
  resendAt: d.resend_available_at,
});
const otpHeaders = (p: StepUpProof) => ({
  'X-OTP-Challenge-Id': p.challengeId,
  'X-OTP-Code': p.code,
});

// ---- Mock ----
const OTP = '123456';
const ago = (h: number) => new Date(Date.now() - h * 3_600_000).toISOString();
const db = mockState('account', () => ({
  profile: {
    full_name: MOCK_USER.name,
    email: MOCK_USER.email,
    email_verified: true,
    // Tài khoản tạo bằng Google: chưa có SĐT / ngày sinh (prototype)
    phone_number: null,
    gender: null,
    date_of_birth: null,
    bio: null,
    preferred_language: 'vi',
    avatar_url: null,
    deletion_request: null,
  } as ProfileDto,
  password: false,
  google: true,
  sessions: [
    {
      id: 501,
      device_name: 'Windows PC · Chrome 126',
      device_type: 'DESKTOP',
      location: 'TP. Hồ Chí Minh, VN',
      ip_address: '113.161.40.12',
      last_active_at: ago(0),
      is_current: true,
    },
    {
      id: 498,
      device_name: 'iPhone 15 · Genie App',
      device_type: 'MOBILE',
      location: 'TP. Hồ Chí Minh, VN',
      ip_address: '171.244.10.5',
      last_active_at: ago(2),
      is_current: false,
    },
    {
      id: 470,
      device_name: 'Samsung Galaxy · Chrome Mobile',
      device_type: 'MOBILE',
      location: 'Biên Hòa, Đồng Nai',
      ip_address: '116.97.220.31',
      last_active_at: ago(30),
      is_current: false,
    },
    {
      id: 455,
      device_name: 'iPad Air · Safari',
      device_type: 'TABLET',
      location: 'Hà Nội, VN',
      ip_address: '14.165.88.7',
      last_active_at: ago(24 * 9),
      is_current: false,
    },
  ] as SessionDto[],
  challenges: {} as Record<string, { purpose: StepUpPurpose; target: string; tries: number }>,
  pendingEmail: null as string | null,
}));
const save = (patch: Partial<ReturnType<typeof db.get>>) => db.set({ ...db.get(), ...patch });

/** Đơn đang xử lý chặn xóa tài khoản (GNE-USR-4091) — đọc trạng thái đơn mock nếu đã có. */
const ACTIVE = [
  'PENDING_PAYMENT',
  'PAID',
  'CONFIRMED',
  'PREPARING',
  'READY_FOR_PICKUP',
  'DELIVERING',
  'DELIVERED',
  'RETURN_REQUESTED',
];
const SEED_ACTIVE = [
  'TR-20260608-000044',
  'TR-20260608-000041',
  'TR-20260608-000047',
  'TR-20260608-000046',
  'TR-20260608-000028',
  'TR-20260608-000029',
  'TR-20260608-000030',
];
function mockActiveOrders(): string[] {
  const seed = readValue<{ code: string; status: string }[]>('session', 'mock.orders');
  const fromSeed = seed
    ? seed.filter((o) => ACTIVE.includes(o.status)).map((o) => o.code)
    : SEED_ACTIVE;
  const placed = placedStore
    .get()
    .orders.filter((o) => ACTIVE.includes(o.status))
    .map((o) => o.code);
  return [...placed, ...fromSeed];
}

function mockChallenge(purpose: StepUpPurpose, target: string): StepUpChallenge {
  const id = `ch_${Math.random().toString(36).slice(2, 10)}`;
  save({ challenges: { ...db.get().challenges, [id]: { purpose, target, tries: 0 } } });
  return {
    id,
    maskedTarget: maskEmail(target),
    expiresAt: new Date(Date.now() + 300_000).toISOString(),
    resendAt: new Date(Date.now() + 60_000).toISOString(),
  };
}
function mockCheckOtp(p: StepUpProof, purpose: StepUpPurpose) {
  const ch = db.get().challenges[p.challengeId];
  if (!ch || ch.purpose !== purpose) mockFail('GNE-AUTH-4101');
  if (p.code !== OTP) {
    const tries = ch.tries + 1;
    save({ challenges: { ...db.get().challenges, [p.challengeId]: { ...ch, tries } } });
    if (tries >= 5) mockFail('GNE-AUTH-4221');
    mockFail('GNE-AUTH-4222', { details: { remaining_attempts: 5 - tries } });
  }
  const { [p.challengeId]: _, ...rest } = db.get().challenges;
  save({ challenges: rest });
  return ch;
}

// ---- API ----
export async function getProfile(): Promise<Profile> {
  if (USE_MOCK) {
    await mockDelay(120);
    return toProfile(db.get().profile);
  }
  return toProfile(await api.get<ProfileDto>('/me/profile'));
}

export async function updateProfile(p: ProfilePatch): Promise<Profile> {
  if (USE_MOCK) {
    await mockDelay(300);
    const cur = db.get().profile;
    const next: ProfileDto = {
      ...cur,
      full_name: p.fullName ?? cur.full_name,
      phone_number: p.phone !== undefined ? p.phone : cur.phone_number,
      gender: p.gender !== undefined ? p.gender : cur.gender,
      date_of_birth: p.dateOfBirth !== undefined ? p.dateOfBirth : cur.date_of_birth,
      bio: p.bio !== undefined ? p.bio : cur.bio,
      preferred_language: p.language ?? cur.preferred_language,
      avatar_url: p.avatarObjectKey ? `/img/products/cosmetics.jpg` : cur.avatar_url,
    };
    save({ profile: next });
    return toProfile(next);
  }
  return toProfile(
    await api.patch<ProfileDto>('/me/profile', {
      full_name: p.fullName,
      phone_number: p.phone,
      gender: p.gender,
      date_of_birth: p.dateOfBirth,
      bio: p.bio,
      preferred_language: p.language,
      avatar_object_key: p.avatarObjectKey,
    }),
  );
}

/** POST /me/email-change-requests → OTP gửi tới email MỚI. */
export async function requestEmailChange(email: string): Promise<StepUpChallenge> {
  if (USE_MOCK) {
    await mockDelay(300);
    if (email === 'an.nguyen@gmail.com') mockFail('GNE-AUTH-4094');
    if (email === db.get().profile.email) mockFail('GNE-AUTH-4224');
    if (!db.get().password) mockFail('GNE-AUTH-4095');
    save({ pendingEmail: email });
    return mockChallenge('EMAIL_CHANGE', email);
  }
  return toChallenge(
    await api.post<ChallengeDto>('/me/email-change-requests', { new_email: email }),
  );
}

export async function confirmEmailChange(p: StepUpProof): Promise<{ email: string }> {
  if (USE_MOCK) {
    await mockDelay(300);
    const ch = mockCheckOtp(p, 'EMAIL_CHANGE');
    save({ profile: { ...db.get().profile, email: ch.target }, pendingEmail: null });
    return { email: ch.target };
  }
  return api.post<{ email: string }>(`/me/email-change-requests/${p.challengeId}/confirm`, {
    otp: p.code,
  });
}

export async function getLoginMethods(): Promise<LoginMethods> {
  if (USE_MOCK) {
    await mockDelay(100);
    const s = db.get();
    return toMethods({
      email: s.profile.email,
      password: { enabled: s.password },
      identities: s.google
        ? [{ provider: 'GOOGLE', email: MOCK_USER.email, linked_at: '2024-03-02T10:00:00+07:00' }]
        : [],
    });
  }
  return toMethods(await api.get<LoginMethodsDto>('/me/login-methods'));
}

/**
 * Liên kết Google. Thật: trình duyệt sang /auth/oauth/google/authorize?intent=link rồi quay về với
 * `code` → POST /me/identities/GOOGLE. Mock: liên kết ngay.
 */
export async function linkGoogle(code: string | null): Promise<void> {
  if (USE_MOCK) {
    await mockDelay(300);
    save({ google: true });
    return;
  }
  await api.post('/me/identities/GOOGLE', {
    code,
    redirect_uri: `${window.location.origin}/account/logins`,
  });
}

export async function unlinkGoogle(): Promise<void> {
  if (USE_MOCK) {
    await mockDelay(300);
    if (!db.get().password) mockFail('GNE-AUTH-4093');
    save({ google: false });
    return;
  }
  await api.delete('/me/identities/GOOGLE');
}

/** POST /me/password/verify — kiểm tra mật khẩu hiện tại khi đang gõ. */
export async function verifyPassword(password: string): Promise<boolean> {
  if (USE_MOCK) {
    await mockDelay(250);
    return password === 'Genie@123';
  }
  return (await api.post<{ valid: boolean }>('/me/password/verify', { password })).valid;
}

/** POST /me/otp-challenges — gửi mã bước 2 (xóa tài khoản kiểm đơn đang xử lý trước). */
export async function requestStepUp(purpose: StepUpPurpose): Promise<StepUpChallenge> {
  if (USE_MOCK) {
    await mockDelay(300);
    if (purpose === 'ACCOUNT_DELETION') {
      const active = mockActiveOrders();
      if (active.length) mockFail('GNE-USR-4091', { details: { active_order_codes: active } });
    }
    return mockChallenge(purpose, db.get().profile.email);
  }
  return toChallenge(await api.post<ChallengeDto>('/me/otp-challenges', { purpose }));
}

export async function resendStepUp(challengeId: string): Promise<StepUpChallenge> {
  if (USE_MOCK) {
    await mockDelay(200);
    const ch = db.get().challenges[challengeId];
    if (!ch) mockFail('GNE-AUTH-4101');
    return { ...mockChallenge(ch.purpose, ch.target), id: challengeId };
  }
  return toChallenge(await api.post<ChallengeDto>(`/me/otp-challenges/${challengeId}/resend`));
}

/** PUT /me/password 🔐 — mật khẩu hiện tại sai → 401 GNE-AUTH-4011. */
export async function changePassword(
  current: string | null,
  next: string,
  proof: StepUpProof,
): Promise<void> {
  if (USE_MOCK) {
    await mockDelay(400);
    if (db.get().password && current !== 'Genie@123') mockFail('GNE-AUTH-4011');
    mockCheckOtp(proof, 'PASSWORD_CHANGE');
    save({ password: true, sessions: db.get().sessions.filter((s) => s.is_current) });
    return;
  }
  await api.put(
    '/me/password',
    { current_password: current ?? undefined, new_password: next },
    { headers: otpHeaders(proof) },
  );
}

export async function listDevices(): Promise<Device[]> {
  if (USE_MOCK) {
    await mockDelay(120);
    return db.get().sessions.map(toDevice);
  }
  return (await api.get<SessionDto[]>('/me/sessions')).map(toDevice);
}

/** DELETE /me/sessions/{id} hoặc ?scope=others. */
export async function logoutDevices(id: number | 'others'): Promise<void> {
  if (USE_MOCK) {
    await mockDelay(250);
    save({
      sessions: db.get().sessions.filter((s) => s.is_current || (id !== 'others' && s.id !== id)),
    });
    return;
  }
  if (id === 'others') await api.delete('/me/sessions', { query: { scope: 'others' } });
  else await api.delete(`/me/sessions/${id}`);
}

/** PUT /me/deletion-request 🔐 — ẩn danh sau 30 ngày, hủy được trong thời gian đó. */
export async function requestDeletion(proof: StepUpProof): Promise<DeletionRequest> {
  if (USE_MOCK) {
    await mockDelay(400);
    mockCheckOtp(proof, 'ACCOUNT_DELETION');
    const now = Date.now();
    const d = {
      requested_at: new Date(now).toISOString(),
      anonymize_at: new Date(now + 30 * 86_400_000).toISOString(),
    };
    save({ profile: { ...db.get().profile, deletion_request: d } });
    return { requestedAt: d.requested_at, anonymizeAt: d.anonymize_at };
  }
  const d = await api.put<{ requested_at: string; anonymize_at: string }>(
    '/me/deletion-request',
    undefined,
    { headers: otpHeaders(proof) },
  );
  return { requestedAt: d.requested_at, anonymizeAt: d.anonymize_at };
}

export async function cancelDeletion(): Promise<void> {
  if (USE_MOCK) {
    await mockDelay(250);
    save({ profile: { ...db.get().profile, deletion_request: null } });
    return;
  }
  await api.delete('/me/deletion-request');
}
