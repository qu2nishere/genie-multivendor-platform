// API công khai của feature "auth" (mở rộng ở G09.14).
export {
  $loginPrompt,
  requireLogin,
  closeLoginPrompt,
  loginHref,
} from './application/loginPrompt.store';
export { LoginPrompt } from './ui/LoginPrompt';
export { UserMenu, LoginButton } from './ui/UserMenu';
export {
  OTP_LENGTH,
  OTP_TTL,
  RESEND_AFTER,
  MAX_ATTEMPTS,
  LOCK_SECONDS,
  safeNext,
  isEmail,
  isPhoneVN,
  type OtpPurpose,
} from './domain/auth.rules';
export type { OtpChallenge, AuthSession, VerifyResult, SocialProvider } from './domain/auth.types';
export { completeLogin } from './ui/completeLogin';
export {
  finishLogin,
  signOut,
  useLogin,
  useRegister,
  useForgot,
  useRequestOtp,
  useResendOtp,
  useVerifyOtp,
  useResetPassword,
} from './application/useAuth';
export { default as LoginForm, type LoginFormProps } from './ui/LoginForm';
export { default as RegisterForm } from './ui/RegisterForm';
export { default as ForgotForm } from './ui/ForgotForm';
export { default as OAuthCallback } from './ui/OAuthCallback';
export { OtpStep, type OtpStepProps } from './ui/OtpStep';
export { LockedState } from './ui/LockedState';
