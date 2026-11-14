// Dạng dữ liệu API xác thực (API_SPEC §2, $OtpChallenge, $AuthTokens).
import type { OtpPurpose } from '../domain/auth.rules';

export interface OtpChallengeDto {
  challenge_id: string;
  purpose: OtpPurpose;
  email_masked: string;
  expires_in: number;
  resend_after: number;
}

export interface AuthUserDto {
  id: number;
  role: 'CUSTOMER' | 'VENDOR' | 'STAFF' | 'ADMIN';
  full_name: string;
  avatar_url: string | null;
}

export interface AuthTokensDto {
  access_token: string;
  token_type: 'Bearer';
  expires_in: number;
  user: AuthUserDto;
}

export interface ResetTokenDto {
  reset_token: string;
  expires_in: number;
}

export interface OAuthResultDto {
  result: 'LOGGED_IN' | 'LINKED' | 'CREATED';
  tokens: AuthTokensDto;
}
