// Biểu tượng nhà cung cấp đăng nhập mạng xã hội — dùng chung cho nút "Tiếp tục với …" (trang đăng nhập / đăng ký)
// và Tài khoản › Liên kết đăng nhập (UC-AUTH-03).
// Zalo: ô vuông bo góc xanh Zalo + bong bóng chat (biểu tượng trung tính, KHÔNG phải logo chính thức). Muốn dùng logo
// chính thức thì lấy từ bộ nhận diện của Zalo (developers.zalo.me) và thay phần thân của ZaloMark.
import { Icon } from './icon/Icon';

export type SocialLogoProvider = 'google' | 'zalo';

/** Logo Google nhiều màu. */
export function GoogleLogo({ size = 18 }: { size?: number }) {
  return (
    <svg width={size} height={size} viewBox="0 0 18 18" aria-hidden="true">
      <path
        fill="#4285F4"
        d="M17.64 9.2c0-.64-.06-1.25-.16-1.84H9v3.48h4.84a4.14 4.14 0 0 1-1.8 2.72v2.26h2.92c1.71-1.57 2.68-3.89 2.68-6.62z"
      />
      <path
        fill="#34A853"
        d="M9 18c2.43 0 4.47-.8 5.96-2.18l-2.92-2.26c-.8.54-1.84.86-3.04.86-2.34 0-4.32-1.58-5.03-3.7H.96v2.33A9 9 0 0 0 9 18z"
      />
      <path fill="#FBBC05" d="M3.97 10.72a5.4 5.4 0 0 1 0-3.44V4.95H.96a9 9 0 0 0 0 8.1l3-2.33z" />
      <path
        fill="#EA4335"
        d="M9 3.58c1.32 0 2.5.45 3.44 1.35l2.58-2.58A9 9 0 0 0 .96 4.95l3 2.33C4.68 5.16 6.66 3.58 9 3.58z"
      />
    </svg>
  );
}

/** Biểu tượng Zalo (trung tính): nền xanh #0068FF, bong bóng chat trắng. */
export function ZaloMark({ size = 18 }: { size?: number }) {
  return (
    <span
      aria-hidden="true"
      className="inline-grid flex-none place-items-center bg-[#0068FF] text-white"
      style={{ width: size, height: size, borderRadius: Math.round(size * 0.28) }}
    >
      <Icon name="message-circle" size={Math.round(size * 0.66)} strokeWidth={2.4} />
    </span>
  );
}

export function SocialLogo({
  provider,
  size = 18,
}: {
  provider: SocialLogoProvider;
  size?: number;
}) {
  return provider === 'google' ? <GoogleLogo size={size} /> : <ZaloMark size={size} />;
}
