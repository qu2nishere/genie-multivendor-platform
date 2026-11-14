// API công khai của feature "auth" (mở rộng ở G09.14).
export {
  $loginPrompt,
  requireLogin,
  closeLoginPrompt,
  loginHref,
} from './application/loginPrompt.store';
export { LoginPrompt } from './ui/LoginPrompt';
export { UserMenu, LoginButton } from './ui/UserMenu';
