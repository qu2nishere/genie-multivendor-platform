// Hồ sơ & bảo mật tài khoản (plan G10.45; API_SPEC 3.2–3.7; prototype data.jsx USER, DEVICES, Account.jsx).

export type Gender = 'MALE' | 'FEMALE' | 'OTHER' | 'PREFER_NOT_TO_SAY';
export const GENDERS: Gender[] = ['MALE', 'FEMALE', 'OTHER', 'PREFER_NOT_TO_SAY'];

export interface DeletionRequest {
  requestedAt: string;
  /** Hồ sơ được ẩn danh lúc này (30 ngày sau khi yêu cầu). */
  anonymizeAt: string;
}

export interface Profile {
  fullName: string;
  email: string;
  emailVerified: boolean;
  phone: string | null;
  gender: Gender | null;
  dateOfBirth: string | null;
  bio: string | null;
  language: 'vi' | 'en';
  avatarUrl: string | null;
  deletion: DeletionRequest | null;
}

export interface ProfilePatch {
  fullName?: string;
  phone?: string | null;
  gender?: Gender | null;
  dateOfBirth?: string | null;
  bio?: string | null;
  language?: 'vi' | 'en';
  avatarObjectKey?: string;
}

export interface LoginMethods {
  email: string;
  password: boolean;
  identities: { provider: 'GOOGLE' | 'APPLE'; email: string; linkedAt: string }[];
}

export type DeviceType = 'DESKTOP' | 'MOBILE' | 'TABLET';

export interface Device {
  id: number;
  name: string;
  type: DeviceType;
  location: string;
  ip: string;
  lastActiveAt: string;
  current: boolean;
}

/** Mục đích OTP bước 2 (0.7) dùng trong khu tài khoản. */
export type StepUpPurpose = 'PASSWORD_CHANGE' | 'ACCOUNT_DELETION' | 'EMAIL_CHANGE';

export interface StepUpChallenge {
  id: string;
  /** Email (đã che) nhận mã. */
  maskedTarget: string;
  expiresAt: string;
  resendAt: string;
}

/** Mã OTP kèm challenge cho thao tác 🔐 (header X-OTP-Challenge-Id + X-OTP-Code). */
export interface StepUpProof {
  challengeId: string;
  code: string;
}
