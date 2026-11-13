// "Hồ sơ của tôi" (prototype Account.jsx ProfilePanel) — plan G10.46, G10.53: họ tên, email (đã xác minh,
// liên kết Google, "Thay đổi" → EmailChangeModal; tài khoản chỉ có Google phải thiết lập mật khẩu trước),
// SĐT, giới tính, ngày sinh, giới thiệu, ngôn ngữ, ảnh đại diện (JPG/PNG ≤ 1 MB, nén trước khi tải);
// Xóa tài khoản (OTP bước 2, ẩn danh sau 30 ngày, hủy được) — react-hook-form + zod.
import { useEffect, useRef, useState } from 'react';
import { useForm } from 'react-hook-form';
import { errorMessage, isApiError } from '@/shared/api/errors';
import { UploadError, uploadFile } from '@/shared/api/upload';
import { useT } from '@/shared/hooks/useT';
import { cn } from '@/shared/lib/cn';
import { formatDate } from '@/shared/lib/format';
import { switchLocale } from '@/shared/lib/locale';
import { zodResolver } from '@/shared/lib/zodResolver';
import {
  Avatar,
  Badge,
  Button,
  Icon,
  Input,
  Modal,
  PanelHead,
  Radio,
  Skeleton,
  toast,
} from '@/shared/ui';
import {
  useCancelDeletion,
  useLoginMethods,
  useProfile,
  useRequestDeletion,
  useRequestStepUp,
  useUpdateProfile,
} from '../application/useAccount';
import { profileSchema, type ProfileFormValues } from '../domain/account.schema';
import { GENDERS, type Profile, type StepUpChallenge } from '../domain/account.types';
import { EmailChangeModal } from './EmailChangeModal';
import { StepUpOtp } from './StepUpOtp';

export const panelClass = 'bg-surface-card rounded-lg shadow-sm px-[22px] py-5 max-sm:px-4';

function ProfileForm({
  p,
  hasPassword,
  google,
}: {
  p: Profile;
  hasPassword: boolean;
  google: boolean;
}) {
  const { t, locale } = useT();
  const update = useUpdateProfile();
  const fileRef = useRef<HTMLInputElement>(null);
  const [avatarBusy, setAvatarBusy] = useState(false);
  const [emailOpen, setEmailOpen] = useState(false);
  const [emailHint, setEmailHint] = useState(false);
  const [delOpen, setDelOpen] = useState(false);
  const cancelDel = useCancelDeletion();
  const form = useForm<ProfileFormValues>({
    resolver: zodResolver(profileSchema),
    defaultValues: {
      fullName: p.fullName,
      phone: p.phone ?? '',
      gender: p.gender,
      dateOfBirth: p.dateOfBirth ?? '',
      bio: p.bio ?? '',
      language: p.language,
    },
  });
  const { errors } = form.formState;
  const err = (m?: string) => (m ? t(m) : undefined);
  const gender = form.watch('gender');
  const lang = form.watch('language');

  const save = form.handleSubmit((v) =>
    update.mutate(
      {
        fullName: v.fullName,
        phone: v.phone || null,
        gender: v.gender as Profile['gender'],
        dateOfBirth: v.dateOfBirth || null,
        bio: v.bio || null,
        language: v.language,
      },
      {
        onSuccess: () => {
          toast.success(t('account.profile.saved'));
          if (v.language !== locale) switchLocale(v.language);
        },
        onError: (e) => toast.error(errorMessage(e, t)),
      },
    ),
  );

  const pickAvatar = async (f: File | undefined) => {
    if (!f) return;
    setAvatarBusy(true);
    try {
      const up = await uploadFile(f, 'AVATAR');
      await update.mutateAsync({ avatarObjectKey: up.objectKey });
      toast.success(t('account.profile.avatar_saved'));
    } catch (e) {
      toast.error(
        e instanceof UploadError ? t('account.profile.avatar_invalid') : errorMessage(e, t),
      );
    } finally {
      setAvatarBusy(false);
    }
  };

  return (
    <div className="grid grid-cols-[minmax(0,1fr)_220px] gap-7 mt-[18px] items-start max-md:grid-cols-1">
      <form className="flex flex-col gap-4" onSubmit={(e) => void save(e)} noValidate>
        <Input
          label={t('account.address.recipient')}
          autoComplete="name"
          error={err(errors.fullName?.message)}
          {...form.register('fullName')}
        />
        <div>
          <div className="font-semibold text-sm text-strong">Email</div>
          <div className="flex items-center gap-2.5 mt-1.5 flex-wrap">
            <span className="text-base text-body">{p.email}</span>
            {p.emailVerified && (
              <Badge variant="mint" icon="badge-check">
                {t('account.profile.verified')}
              </Badge>
            )}
            {google && (
              <span className="inline-flex items-center gap-1.5 font-semibold text-xs text-body border border-solid border-line rounded-pill px-2.5 py-[3px]">
                <Icon name="user-check" size={13} /> {t('account.profile.linked_google')}
              </span>
            )}
            <button
              type="button"
              className="border-0 bg-transparent p-0 text-sm font-semibold text-brand-600 cursor-pointer hover:underline min-h-8"
              onClick={() => (hasPassword ? setEmailOpen(true) : setEmailHint((v) => !v))}
            >
              {t('account.profile.change')}
            </button>
          </div>
          {!hasPassword && emailHint && (
            <p className="m-0 mt-2 flex gap-2 px-3 py-2.5 bg-blue-50 border border-solid border-blue-200 rounded-md text-sm text-body leading-normal">
              <Icon name="info" size={16} className="text-blue-600 flex-none mt-px" />
              <span>
                {t('account.profile.email_need_password_before')}{' '}
                <a href="/account/password" className="font-bold text-blue-600">
                  {t('account.menu.password_set').toLowerCase()}
                </a>{' '}
                {t('account.profile.email_need_password_after')}
              </span>
            </p>
          )}
        </div>
        <Input
          label={t('account.address.phone')}
          placeholder={t('account.profile.phone_empty')}
          inputMode="tel"
          autoComplete="tel"
          error={err(errors.phone?.message)}
          {...form.register('phone')}
        />
        <fieldset className="border-0 p-0 m-0">
          <legend className="p-0 font-semibold text-sm text-strong">
            {t('account.profile.gender')}
          </legend>
          <div className="flex gap-[18px] mt-2 flex-wrap">
            {GENDERS.map((g) => (
              <Radio
                key={g}
                name="gender"
                checked={gender === g}
                onChange={() => form.setValue('gender', g)}
              >
                <span className="text-base text-body">{t(`account.profile.genders.${g}`)}</span>
              </Radio>
            ))}
          </div>
        </fieldset>
        <div className="grid grid-cols-2 gap-3.5 max-sm:grid-cols-1">
          <Input
            type="date"
            label={t('account.profile.dob')}
            hint={!form.watch('dateOfBirth') ? t('account.profile.dob_empty') : undefined}
            error={err(errors.dateOfBirth?.message)}
            {...form.register('dateOfBirth')}
          />
        </div>
        <Input
          label={t('account.profile.bio')}
          maxLength={200}
          error={err(errors.bio?.message)}
          {...form.register('bio')}
        />
        <fieldset className="border-0 p-0 m-0">
          <legend className="p-0 font-semibold text-sm text-strong">
            {t('account.profile.language')}
          </legend>
          <div className="flex gap-2.5 mt-2">
            {(['vi', 'en'] as const).map((l) => (
              <button
                key={l}
                type="button"
                aria-pressed={lang === l}
                onClick={() => form.setValue('language', l)}
                className={cn(
                  'flex items-center gap-2 px-4 py-2.5 rounded-md cursor-pointer text-sm border-[1.5px] border-solid',
                  lang === l
                    ? 'border-primary bg-primary-tint text-brand-700 font-semibold'
                    : 'border-line bg-surface-card text-body font-medium',
                )}
              >
                {t(`account.profile.lang.${l}`)}
                {lang === l && <Icon name="check" size={16} />}
              </button>
            ))}
          </div>
        </fieldset>
        {p.deletion && (
          <p
            role="status"
            className="m-0 flex gap-2 items-center bg-red-50 text-red-600 rounded-md px-3 py-2.5 text-sm"
          >
            <Icon name="clock" size={16} className="flex-none" />
            {t('account.delete.pending', { date: formatDate(p.deletion.anonymizeAt, { locale }) })}
          </p>
        )}
        <div className="flex items-center gap-3 mt-1 flex-wrap">
          <Button type="submit" iconLeft="check" loading={update.isPending && !avatarBusy}>
            {t('account.profile.save')}
          </Button>
          {p.deletion ? (
            <Button
              type="button"
              variant="outline"
              iconLeft="undo-2"
              loading={cancelDel.isPending}
              onClick={() =>
                cancelDel.mutate(undefined, {
                  onSuccess: () => toast.success(t('account.delete.cancelled')),
                  onError: (e) => toast.error(errorMessage(e, t)),
                })
              }
            >
              {t('account.delete.cancel')}
            </Button>
          ) : (
            <Button
              type="button"
              variant="danger"
              iconLeft="trash-2"
              onClick={() => setDelOpen(true)}
            >
              {t('account.delete.open')}
            </Button>
          )}
        </div>
      </form>
      <div className="flex flex-col items-center gap-3.5 border-0 border-l border-solid border-line-subtle pl-7 max-md:border-l-0 max-md:pl-0 max-md:order-first">
        <Avatar name={p.fullName} src={p.avatarUrl} size={104} />
        <input
          ref={fileRef}
          type="file"
          hidden
          accept="image/jpeg,image/png"
          onChange={(e) => {
            void pickAvatar(e.target.files?.[0]);
            e.target.value = '';
          }}
        />
        <Button
          variant="outline"
          iconLeft="upload"
          loading={avatarBusy}
          onClick={() => fileRef.current?.click()}
        >
          {t('account.profile.avatar_pick')}
        </Button>
        <p className="m-0 text-xs text-subtle text-center leading-normal">
          {t('account.profile.avatar_hint')}
        </p>
      </div>
      <EmailChangeModal open={emailOpen} current={p.email} onClose={() => setEmailOpen(false)} />
      <DeleteAccountModal open={delOpen} name={p.fullName} onClose={() => setDelOpen(false)} />
    </div>
  );
}

/** Xóa tài khoản: kiểm đơn đang xử lý → OTP → ẩn danh sau 30 ngày (3.7). */
function DeleteAccountModal({
  open,
  name,
  onClose,
}: {
  open: boolean;
  name: string;
  onClose: () => void;
}) {
  const { t } = useT();
  const send = useRequestStepUp();
  const del = useRequestDeletion();
  const [blocked, setBlocked] = useState<string[]>([]);
  const [challenge, setChallenge] = useState<StepUpChallenge | null>(null);
  const [code, setCode] = useState('');
  const [err, setErr] = useState<string | null>(null);
  useEffect(() => {
    if (open) {
      setBlocked([]);
      setChallenge(null);
      setCode('');
      setErr(null);
    }
  }, [open]);

  const start = () =>
    send.mutate('ACCOUNT_DELETION', {
      onSuccess: setChallenge,
      onError: (e) => {
        const codes = isApiError(e) ? e.details.active_order_codes : null;
        if (Array.isArray(codes)) setBlocked(codes.map(String));
        else toast.error(errorMessage(e, t));
      },
    });
  const confirm = () =>
    challenge &&
    del.mutate(
      { challengeId: challenge.id, code },
      {
        onSuccess: () => {
          toast.success(t('account.delete.done'));
          onClose();
        },
        onError: (e) => {
          if (isApiError(e) && e.code === 'GNE-AUTH-4221') onClose();
          setErr(errorMessage(e, t));
          setCode('');
        },
      },
    );

  return (
    <Modal
      open={open}
      onClose={onClose}
      title={t('account.delete.title')}
      width={480}
      footer={
        blocked.length ? (
          <Button variant="ghost" onClick={onClose}>
            {t('common.action.close')}
          </Button>
        ) : challenge ? (
          <>
            <Button variant="ghost" onClick={onClose}>
              {t('common.action.cancel')}
            </Button>
            <Button
              variant="danger"
              iconLeft="trash-2"
              disabled={code.length < 6}
              loading={del.isPending}
              onClick={confirm}
            >
              {t('account.delete.confirm')}
            </Button>
          </>
        ) : (
          <>
            <Button variant="ghost" onClick={onClose}>
              {t('common.action.cancel')}
            </Button>
            <Button variant="danger" iconLeft="mail" loading={send.isPending} onClick={start}>
              {t('account.delete.send_otp')}
            </Button>
          </>
        )
      }
    >
      <div className="flex gap-3 items-start">
        <span className="w-10 h-10 rounded-full bg-red-50 text-red-600 grid place-items-center flex-none">
          <Icon name="triangle-alert" size={20} />
        </span>
        <div className="text-base text-body leading-normal">
          {blocked.length ? (
            <>
              {t('account.delete.blocked')}
              <ul className="m-0 mt-1.5 pl-[18px] font-mono text-sm">
                {blocked.map((c) => (
                  <li key={c}>{c}</li>
                ))}
              </ul>
            </>
          ) : (
            t('account.delete.text', { name })
          )}
        </div>
      </div>
      {challenge && !blocked.length && (
        <div className="mt-3.5">
          <StepUpOtp
            challenge={challenge}
            value={code}
            onChange={setCode}
            error={err}
            onResent={setChallenge}
          />
        </div>
      )}
    </Modal>
  );
}

export function ProfilePanel() {
  const { t } = useT();
  const q = useProfile();
  const m = useLoginMethods();
  return (
    <section className={panelClass} aria-labelledby="profile-title">
      <PanelHead
        id="profile-title"
        icon="user"
        title={t('account.profile.title')}
        sub={t('account.profile.sub')}
      />
      {q.data && m.data ? (
        <ProfileForm
          p={q.data}
          hasPassword={m.data.password}
          google={m.data.identities.some((i) => i.provider === 'GOOGLE')}
        />
      ) : (
        <Skeleton lines={6} className="mt-5" />
      )}
    </section>
  );
}
