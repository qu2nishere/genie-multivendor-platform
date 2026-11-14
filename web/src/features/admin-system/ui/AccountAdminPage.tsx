// Tài khoản quản trị (prototype admin/Finance.jsx AccountAdmin) — plan G12.44: hồ sơ (ảnh đại diện, sửa
// tên — PATCH /admin/me), vai trò / số quyền / trạng thái; đổi mật khẩu bắt buộc OTP và phiên đăng nhập
// dùng lại `PasswordForm`, `DevicesPanel` của khu tài khoản (cùng API /me/password, /me/sessions — 11.1).
import { useRef, useState } from 'react';
import { useForm } from 'react-hook-form';
import { DevicesPanel, PasswordForm } from '@/features/account';
import { errorMessage } from '@/shared/api/errors';
import { uploadFile, UploadError } from '@/shared/api/upload';
import { PERMISSIONS } from '@/shared/auth/permissions';
import { useT } from '@/shared/hooks/useT';
import { maxMb } from '@/shared/lib/upload.rules';
import { zodResolver } from '@/shared/lib/zodResolver';
import { Avatar, Badge, Button, Icon, Input, Skeleton, Spinner, toast } from '@/shared/ui';
import { useAdminMe } from '../application/useAdminMe';
import { useUpdateAdminMe } from '../application/useSystem';
import { nameSchema, type NameValues } from '../domain/system.schema';

const card = 'bg-surface-card rounded-lg shadow-sm p-[22px] max-md:p-4';

function Info({ k, v, tone }: { k: string; v: string; tone?: string }) {
  return (
    <div className="border border-solid border-line-subtle rounded-md px-3.5 py-3">
      <div className="text-xs text-muted">{k}</div>
      <div className={`text-base font-bold mt-[3px] ${tone ?? 'text-strong'}`}>{v}</div>
    </div>
  );
}

export default function AccountAdminPage() {
  const { t } = useT();
  const me = useAdminMe();
  const update = useUpdateAdminMe();
  const fileRef = useRef<HTMLInputElement>(null);
  const [editing, setEditing] = useState(false);
  const [uploading, setUploading] = useState(false);
  const form = useForm<NameValues>({
    resolver: zodResolver(nameSchema),
    defaultValues: { fullName: '' },
  });
  if (!me.data) return <Skeleton lines={10} />;
  const u = me.data;
  const role =
    u.role === 'ADMIN'
      ? t('admin.perm.title.ADMIN')
      : t(`admin.perm.title.${u.staffTitle ?? 'CUSTOM'}`);
  const err = form.formState.errors.fullName?.message;

  const pick = async (f: File) => {
    setUploading(true);
    try {
      const r = await uploadFile(f, 'AVATAR');
      update.mutate(
        { avatarObjectKey: r.objectKey, avatarPreview: r.previewUrl },
        {
          onSuccess: () => toast.success(t('admin-system.account.avatar_saved')),
          onError: (e) => toast.error(errorMessage(e, t)),
        },
      );
    } catch (e) {
      toast.error(
        e instanceof UploadError
          ? t(`admin-system.account.err.upload_${e.issue}`, { mb: maxMb('AVATAR') })
          : errorMessage(e, t),
      );
    } finally {
      setUploading(false);
    }
  };

  const saveName = form.handleSubmit((v) =>
    update.mutate(
      { fullName: v.fullName },
      {
        onSuccess: () => {
          toast.success(t('admin-system.account.name_saved'));
          setEditing(false);
        },
        onError: (e) => toast.error(errorMessage(e, t)),
      },
    ),
  );

  return (
    <div className="max-w-[1180px] flex flex-col gap-4">
      <section className={card} aria-labelledby="acc-name">
        <div className="flex items-center gap-4 flex-wrap">
          <button
            type="button"
            onClick={() => fileRef.current?.click()}
            aria-label={t('admin-system.account.change_avatar')}
            title={t('admin-system.account.change_avatar')}
            className="relative border-0 bg-transparent p-0 cursor-pointer rounded-full focus-visible:outline-none focus-visible:shadow-focus"
          >
            <Avatar name={u.fullName} src={u.avatarUrl} size={64} />
            {uploading && (
              <span className="absolute inset-0 grid place-items-center bg-white/60 rounded-full">
                <Spinner size={20} />
              </span>
            )}
          </button>
          <input
            ref={fileRef}
            type="file"
            hidden
            accept="image/jpeg,image/png"
            onChange={(e) => {
              const f = e.target.files?.[0];
              e.target.value = '';
              if (f) void pick(f);
            }}
          />
          <div className="flex-1 min-w-[220px]">
            <div className="flex items-center gap-2.5 flex-wrap">
              {editing ? (
                <form
                  className="flex items-start gap-2"
                  onSubmit={(e) => void saveName(e)}
                  noValidate
                >
                  <Input
                    aria-label={t('admin-system.account.name')}
                    wrapperClassName="w-[240px] max-sm:w-full"
                    maxLength={100}
                    error={err ? t(err) : undefined}
                    {...form.register('fullName')}
                  />
                  <Button size="sm" type="submit" loading={update.isPending} className="mt-1">
                    {t('admin-system.account.save')}
                  </Button>
                  <Button
                    size="sm"
                    variant="ghost"
                    className="mt-1"
                    onClick={() => setEditing(false)}
                  >
                    {t('common.action.cancel')}
                  </Button>
                </form>
              ) : (
                <>
                  <h2 id="acc-name" className="m-0 text-xl font-bold text-strong">
                    {u.fullName}
                  </h2>
                  <Button
                    size="sm"
                    variant="ghost"
                    iconLeft="pencil"
                    onClick={() => {
                      form.reset({ fullName: u.fullName });
                      setEditing(true);
                    }}
                  >
                    {t('admin-system.account.edit')}
                  </Button>
                </>
              )}
              <Badge variant="primary">{role}</Badge>
            </div>
            <div className="flex items-center gap-2 mt-[5px] flex-wrap">
              <span className="font-mono text-[13px] text-muted">{u.email}</span>
              <Badge variant="mint" icon="badge-check">
                {t('admin-system.account.verified')}
              </Badge>
            </div>
          </div>
        </div>
        <div className="grid grid-cols-3 gap-3 mt-4 max-md:grid-cols-1">
          <Info k={t('admin-system.account.role')} v={role} />
          <Info
            k={t('admin-system.account.perms')}
            v={`${u.permissions.length}/${PERMISSIONS.length}`}
          />
          <Info
            k={t('admin-system.account.status')}
            v={t(`admin-system.staff.status.${u.status}`)}
            tone={u.status === 'ACTIVE' ? 'text-mint-600' : 'text-red-600'}
          />
        </div>
      </section>

      <section className={card} id="admin-password" aria-labelledby="acc-pw">
        <div className="flex items-center gap-2 mb-3.5">
          <Icon name="shield-check" size={19} className="text-blue-600" />
          <h2 id="acc-pw" className="m-0 text-base font-bold text-strong">
            {t('admin-system.account.password')}
          </h2>
        </div>
        <aside className="flex gap-2.5 items-start bg-surface-sunken rounded-md px-4 py-3 mb-4">
          <Icon name="info" size={16} className="text-blue-600 mt-0.5 shrink-0" />
          <p className="m-0 text-sm text-muted leading-relaxed">
            <b className="text-strong">{t('admin-system.account.otp_title')}.</b>{' '}
            {t('admin-system.account.otp_desc')}
          </p>
        </aside>
        <PasswordForm hasPassword />
      </section>

      <DevicesPanel passwordHref="#admin-password" />
    </div>
  );
}
