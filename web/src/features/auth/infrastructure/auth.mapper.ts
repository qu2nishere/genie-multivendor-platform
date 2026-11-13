import { USE_MOCK } from '@/shared/api/mock';
import { MOCK_USER, type SessionUser } from '@/shared/auth/session';
import type { AuthSession, OtpChallenge } from '../domain/auth.types';
import type { AuthTokensDto, OtpChallengeDto } from './auth.dto';

export const toChallenge = (d: OtpChallengeDto): OtpChallenge => ({
  id: d.challenge_id,
  purpose: d.purpose,
  emailMasked: d.email_masked,
  expiresIn: d.expires_in,
  resendAfter: d.resend_after,
});

/** $AuthTokens chỉ có thông tin tối thiểu; phiên đầy đủ (quyền, shop) nạp lại khi refresh. */
export function toSession(d: AuthTokensDto, email = ''): AuthSession<SessionUser> {
  const user: SessionUser = USE_MOCK
    ? MOCK_USER
    : {
        id: d.user.id,
        name: d.user.full_name,
        email,
        avatar_url: d.user.avatar_url,
        roles: [d.user.role],
        permissions: [],
      };
  return { accessToken: d.access_token, user };
}
