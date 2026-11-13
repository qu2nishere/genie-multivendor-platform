// Biểu tượng phương thức thanh toán (prototype Checkout.jsx PAYMENTS, CodLogo) — plan G10.26.
// Chỉ VNPay, VietQR, COD (TS 3.5). Màu nét của CodLogo theo token mint.
import { PAYMENT_LOGOS } from '@/shared/lib/assets';
import { Icon } from '@/shared/ui';

export function CodLogo({ size = 30 }: { size?: number }) {
  return (
    <svg viewBox="0 0 100 100" width={size} height={size} aria-hidden="true">
      <rect
        x="10"
        y="25"
        width="80"
        height="50"
        rx="6"
        fill="var(--surface-card)"
        stroke="var(--mint-500)"
        strokeWidth="4"
      />
      <circle
        cx="50"
        cy="50"
        r="14"
        fill="var(--mint-50)"
        stroke="var(--mint-500)"
        strokeWidth="3"
      />
      <text
        x="50"
        y="55"
        fontFamily="var(--font-sans)"
        fontWeight="bold"
        fontSize="16"
        fill="var(--mint-600)"
        textAnchor="middle"
      >
        $
      </text>
    </svg>
  );
}

/** Logo / icon 30×30 của phương thức. */
export function PaymentLogo({ method }: { method: 'VNPAY' | 'VIETQR' | 'COD' }) {
  if (method === 'VNPAY')
    return (
      <img
        src={PAYMENT_LOGOS.vnpay}
        alt=""
        width={30}
        height={30}
        className="w-[30px] h-[30px] object-contain"
      />
    );
  if (method === 'VIETQR') return <Icon name="qr-code" size={26} className="text-blue-600" />;
  return <CodLogo />;
}
