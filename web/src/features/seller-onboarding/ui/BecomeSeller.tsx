// Thẻ "Đăng ký mở Shop" của trang /become-seller (prototype BecomeSeller.jsx) — plan G10.65, client:load.
// Khách: nhắc đăng nhập. Đã đăng nhập: form (react-hook-form + zod, điền sẵn họ tên / email / SĐT từ phiên)
// → 9.1 gửi mã email → modal OTP → 9.2 xác nhận → sang /seller/kyc.
import { useStore } from '@nanostores/react';
import { useEffect, useState } from 'react';
import { useForm } from 'react-hook-form';
import { requireLogin } from '@/features/auth';
import { errorMessage, isApiError } from '@/shared/api/errors';
import { USE_MOCK } from '@/shared/api/mock';
import { $session } from '@/shared/auth/session';
import { useT } from '@/shared/hooks/useT';
import { zodResolver } from '@/shared/lib/zodResolver';
import { Providers } from '@/shared/providers/Providers';
import { Button, Checkbox, Icon, Input, Modal, OtpInput, Select, toast } from '@/shared/ui';
import { useConfirmShop, useRegisterShop } from '../application/useRegistration';
import { registrationSchema, type RegistrationValues } from '../domain/registration.schema';
import { BUSINESS_TYPES, type RegistrationChallenge } from '../domain/registration.types';

export interface BecomeSellerProps {
  categories: { id: number; name: string }[];
}

const linkCls = 'text-brand-600';

function Card({ categories }: BecomeSellerProps) {
  const { t } = useT();
  const session = useStore($session);
  const [mounted, setMounted] = useState(false);
  useEffect(() => setMounted(true), []);
  const user = mounted && session.status === 'authenticated' ? session.user : null;
  const guest = mounted && session.status === 'guest';

  const register = useRegisterShop();
  const confirm = useConfirmShop();
  const [challenge, setChallenge] = useState<RegistrationChallenge | null>(null);
  const [otp, setOtp] = useState('');
  const [otpErr, setOtpErr] = useState<string | null>(null);

  const form = useForm<RegistrationValues>({
    resolver: zodResolver(registrationSchema),
    defaultValues: {
      shopName: '',
      ownerName: '',
      phone: '',
      email: '',
      mainCategoryId: categories[0]?.id ?? 0,
      businessType: 'INDIVIDUAL',
      acceptTerms: false as unknown as true,
    },
  });
  const { errors } = form.formState;
  const err = (m?: string) => (m ? t(m) : undefined);

  // Điền sẵn từ tài khoản khi có phiên
  useEffect(() => {
    if (!user) return;
    const v = form.getValues();
    if (!v.ownerName) form.setValue('ownerName', user.name);
    if (!v.email) form.setValue('email', user.email);
    if (!v.phone && user.phone) form.setValue('phone', user.phone);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [user?.id]);

  const submit = form.handleSubmit((v) =>
    register.mutate(v, {
      onSuccess: (c) => {
        setOtp('');
        setOtpErr(null);
        setChallenge(c);
      },
      onError: (e) => {
        if (isApiError(e) && e.code === 'GNE-VND-4094')
          form.setError('shopName', { message: `errors.${e.code}` });
        else toast.error(errorMessage(e, t));
      },
    }),
  );

  const confirmOtp = () => {
    if (!challenge) return;
    confirm.mutate(
      { challengeId: challenge.id, otp },
      {
        onSuccess: () => {
          toast.success(t('seller-onboarding.landing.done'));
          window.location.assign('/seller/kyc');
        },
        onError: (e) => setOtpErr(errorMessage(e, t)),
      },
    );
  };

  const terms = form.watch('acceptTerms');
  const cat = form.watch('mainCategoryId');
  const type = form.watch('businessType');

  return (
    <div className="bg-surface-card rounded-lg shadow-xl p-6 text-body max-sm:p-4">
      {guest && (
        <div className="flex gap-[9px] items-center bg-blue-50 border border-solid border-blue-200 rounded-md px-3 py-2.5 mb-4">
          <Icon name="info" size={16} className="text-blue-600 flex-none" />
          <span className="text-xs text-body">
            {t('seller-onboarding.landing.need_login_a')}{' '}
            <button
              type="button"
              onClick={() => requireLogin()}
              className="border-0 bg-transparent p-0 font-bold text-blue-600 cursor-pointer"
            >
              {t('seller-onboarding.landing.login')}
            </button>{' '}
            {t('seller-onboarding.landing.need_login_b')}
          </span>
        </div>
      )}
      <h2 className="m-0 mb-1 text-xl font-bold text-strong">
        {t('seller-onboarding.landing.form_title')}
      </h2>
      <p className="m-0 mb-4 text-sm text-muted">{t('seller-onboarding.landing.form_sub')}</p>
      <form className="flex flex-col gap-3" noValidate onSubmit={(e) => void submit(e)}>
        <Input
          label={t('seller-onboarding.landing.shop_name')}
          placeholder="VD: TechZone Official"
          iconLeft="store"
          error={err(errors.shopName?.message)}
          {...form.register('shopName')}
        />
        <div className="grid grid-cols-2 gap-2.5 max-sm:grid-cols-1">
          <Input
            label={t('seller-onboarding.landing.owner')}
            placeholder={t('common.placeholder.person_name')}
            autoComplete="name"
            error={err(errors.ownerName?.message)}
            {...form.register('ownerName')}
          />
          <Input
            label={t('seller-onboarding.landing.phone')}
            placeholder="0901 234 567"
            inputMode="tel"
            autoComplete="tel"
            error={err(errors.phone?.message)}
            {...form.register('phone')}
          />
        </div>
        <Input
          label="Email"
          type="email"
          placeholder="shop@email.com"
          iconLeft="mail"
          autoComplete="email"
          error={err(errors.email?.message)}
          {...form.register('email')}
        />
        <div className="grid grid-cols-2 gap-2.5 max-sm:grid-cols-1">
          <Select
            label={t('seller-onboarding.landing.category')}
            value={cat}
            onChange={(v) => form.setValue('mainCategoryId', v, { shouldValidate: true })}
            options={categories.map((c) => ({ value: c.id, label: c.name, text: c.name }))}
            error={err(errors.mainCategoryId?.message)}
          />
          <Select
            label={t('seller-onboarding.landing.type')}
            value={type}
            onChange={(v) => form.setValue('businessType', v)}
            options={BUSINESS_TYPES.map((b) => ({
              value: b,
              label: t(`seller-onboarding.landing.types.${b}`),
            }))}
          />
        </div>
        <Checkbox
          checked={!!terms}
          onChange={(e) =>
            form.setValue('acceptTerms', e.target.checked as true, { shouldValidate: true })
          }
          className="items-start text-xs"
        >
          <span
            className={
              errors.acceptTerms ? 'text-red-600 leading-normal' : 'text-muted leading-normal'
            }
          >
            {t('seller-onboarding.landing.terms_a')}{' '}
            <a href="/policies/seller-terms" target="_blank" rel="noopener" className={linkCls}>
              {t('seller-onboarding.landing.terms_link')}
            </a>{' '}
            {t('seller-onboarding.landing.terms_b')}{' '}
            <a href="/policies/commission-rates" target="_blank" rel="noopener" className={linkCls}>
              {t('seller-onboarding.landing.commission_link')}
            </a>{' '}
            {t('seller-onboarding.landing.terms_c')}
          </span>
        </Checkbox>
        {errors.acceptTerms && (
          <span className="sr-only" role="alert">
            {t('seller-onboarding.landing.err.terms')}
          </span>
        )}
        {guest ? (
          <Button type="button" block size="lg" iconLeft="user" onClick={() => requireLogin()}>
            {t('seller-onboarding.landing.login_cta')}
          </Button>
        ) : (
          <Button
            type="submit"
            block
            size="lg"
            iconLeft="store"
            loading={register.isPending}
            disabled={!mounted}
          >
            {t('seller-onboarding.landing.submit')}
          </Button>
        )}
        <p className="m-0 text-xs text-subtle text-center">
          {t('seller-onboarding.landing.already')}{' '}
          <a href="/seller" className={linkCls}>
            {t('seller-onboarding.landing.seller_center')}
          </a>
        </p>
      </form>

      <Modal
        open={!!challenge}
        onClose={() => setChallenge(null)}
        width={420}
        title={t('seller-onboarding.landing.otp_title')}
        footer={
          <>
            <Button variant="ghost" onClick={() => setChallenge(null)}>
              {t('common.action.cancel')}
            </Button>
            <Button disabled={otp.length < 6} loading={confirm.isPending} onClick={confirmOtp}>
              {t('common.action.confirm')}
            </Button>
          </>
        }
      >
        <p className="m-0 mb-3 text-sm text-muted">
          {t('seller-onboarding.landing.otp_sent', { email: challenge?.maskedTarget ?? '' })}
          {USE_MOCK && <> ({t('account.otp.demo')})</>}
        </p>
        <OtpInput
          value={otp}
          onChange={(v) => {
            setOtp(v);
            setOtpErr(null);
          }}
          invalid={!!otpErr}
          autoFocus
        />
        {otpErr && (
          <p className="m-0 mt-2 text-xs text-red-600" role="alert">
            {otpErr}
          </p>
        )}
      </Modal>
    </div>
  );
}

export default function BecomeSeller(props: BecomeSellerProps) {
  return (
    <Providers>
      <Card {...props} />
    </Providers>
  );
}
