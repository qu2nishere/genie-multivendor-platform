// Cổng KYC (prototype index.html KycGate; API_SPEC 0.9) — plan G11.05: shop chưa VERIFIED thì mọi màn
// trừ Hồ sơ KYC và Cài đặt bị khóa; nút sang /seller/kyc.
import { Link } from 'react-router-dom';
import { kycGroup, type KycStatus } from '@/features/seller-settings';
import { useT } from '@/shared/hooks/useT';
import { buttonClass, Icon } from '@/shared/ui';

export function KycGate({ status }: { status: KycStatus }) {
  const { t } = useT();
  const g = kycGroup(status);
  const bad = g === 'REJECTED' || g === 'SUPPLEMENT';
  const key =
    g === 'REJECTED'
      ? 'rejected'
      : g === 'SUPPLEMENT'
        ? 'supplement'
        : g === 'WAITING'
          ? 'waiting'
          : 'locked';
  return (
    <div className="max-w-[640px] mx-auto my-8 text-center">
      <div
        className={`w-[84px] h-[84px] rounded-full grid place-items-center mx-auto mb-5 ${
          bad ? 'bg-red-50 text-red-600' : 'bg-amber-50 text-amber-600'
        }`}
      >
        <Icon name={bad ? 'triangle-alert' : 'lock'} size={42} />
      </div>
      <h2 className="m-0 mb-2.5 text-2xl font-bold text-strong">
        {t(`seller.shell.gate.${key}.title`)}
      </h2>
      <p className="m-0 mx-auto mb-6 max-w-[480px] text-base text-muted leading-relaxed">
        {t(`seller.shell.gate.${key}.desc`)}
      </p>
      <Link to="/kyc" className={buttonClass({ size: 'lg' })}>
        <Icon name="shield-check" size={20} />
        {t('seller.shell.gate.cta')}
      </Link>
    </div>
  );
}
