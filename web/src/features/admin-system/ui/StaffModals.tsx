// Mời nhân viên + Chỉnh quyền (prototype Finance.jsx InviteStaff, RbacEditor) — plan G12.41: chức danh
// nạp bộ quyền mặc định (chỉnh tay → Tùy chỉnh), 13 mã quyền (MANAGE_RBAC không cấp cho Staff), cảnh báo
// tách biệt nhiệm vụ PROCESS_PAYOUT + UPDATE_COMMISSION; email công việc @genie.example.
import { useEffect, useState } from 'react';
import { useForm } from 'react-hook-form';
import { errorMessage, isApiError } from '@/shared/api/errors';
import {
  PERMISSIONS,
  segregationWarning,
  type PermissionCode,
  type StaffTitle,
} from '@/shared/auth/permissions';
import { useT } from '@/shared/hooks/useT';
import { cn } from '@/shared/lib/cn';
import { zodResolver } from '@/shared/lib/zodResolver';
import { Button, Checkbox, Icon, Input, Modal, Select, toast } from '@/shared/ui';
import { useInviteStaff, useUpdatePermissions } from '../application/useSystem';
import { EDIT_TITLES, presetOf, titleOf, togglePerm } from '../domain/system.rules';
import { inviteSchema, type InviteValues } from '../domain/system.schema';
import type { StaffMember } from '../domain/system.types';

type EditTitle = Exclude<StaffTitle, 'ADMIN'>;
const lab = 'block text-sm font-semibold text-strong';

function Segregation({ perms }: { perms: PermissionCode[] }) {
  const { t } = useT();
  if (!segregationWarning(perms)) return null;
  return (
    <div
      role="alert"
      className="flex items-center gap-[9px] bg-amber-50 text-amber-700 rounded-md px-3.5 py-2.5 text-sm"
    >
      <Icon name="triangle-alert" size={16} className="flex-none" />
      {t('admin-system.staff.segregation')}
    </div>
  );
}

export function InviteStaffModal({ open, onClose }: { open: boolean; onClose: () => void }) {
  const { t } = useT();
  const invite = useInviteStaff();
  const [perms, setPerms] = useState<PermissionCode[]>(presetOf('MODERATOR', []));
  const form = useForm<InviteValues>({
    resolver: zodResolver(inviteSchema),
    defaultValues: { fullName: '', email: '', staffTitle: 'MODERATOR' },
  });
  useEffect(() => {
    if (!open) return;
    form.reset({ fullName: '', email: '', staffTitle: 'MODERATOR' });
    setPerms(presetOf('MODERATOR', []));
  }, [open, form]);
  const title = form.watch('staffTitle');
  const e = form.formState.errors;
  const m = (x?: string) => (x ? t(x) : undefined);
  const pickTitle = (x: EditTitle) => {
    form.setValue('staffTitle', x);
    setPerms((p) => presetOf(x, p));
  };
  const submit = form.handleSubmit((v) =>
    invite.mutate(
      {
        fullName: v.fullName,
        email: v.email,
        staffTitle: titleOf(v.staffTitle, perms),
        permissions: perms,
      },
      {
        onSuccess: () => {
          toast.success(t('admin-system.staff.invited', { email: v.email }));
          onClose();
        },
        onError: (err) => {
          if (isApiError(err) && (err.code === 'GNE-ADM-4091' || err.code === 'GNE-ADM-4221'))
            form.setError('email', { message: `errors.${err.code}` });
          else toast.error(errorMessage(err, t));
        },
      },
    ),
  );
  return (
    <Modal
      open={open}
      onClose={onClose}
      width={560}
      dismissible={!form.formState.isDirty}
      title={t('admin-system.staff.invite_title')}
      footer={
        <>
          <Button variant="ghost" onClick={onClose}>
            {t('common.action.cancel')}
          </Button>
          <Button iconLeft="send" loading={invite.isPending} onClick={() => void submit()}>
            {t('admin-system.staff.invite_send')}
          </Button>
        </>
      }
    >
      <form className="flex flex-col gap-4" onSubmit={(ev) => void submit(ev)} noValidate>
        <div className="flex gap-[9px] items-center bg-blue-50 border border-solid border-blue-200 rounded-md px-[13px] py-2.5">
          <Icon name="info" size={16} className="text-blue-600 flex-none" />
          <span className="text-xs text-body">{t('admin-system.staff.invite_note')}</span>
        </div>
        <div className="grid grid-cols-2 gap-3 max-sm:grid-cols-1">
          <Input
            label={t('admin-system.staff.name')}
            placeholder="Nguyễn Văn A"
            error={m(e.fullName?.message)}
            {...form.register('fullName')}
          />
          <Select<EditTitle>
            label={t('admin-system.staff.title')}
            value={title}
            onChange={pickTitle}
            options={EDIT_TITLES.map((x) => ({ value: x, label: t(`admin.perm.title.${x}`) }))}
          />
        </div>
        <Input
          label={t('admin-system.staff.email')}
          iconLeft="mail"
          placeholder="ten@genie.example"
          error={m(e.email?.message)}
          {...form.register('email')}
        />
        <div>
          <div className="flex items-center mb-2">
            <span className={lab}>{t('admin-system.staff.perms_by_title')}</span>
            <span className="ml-auto text-xs text-muted">
              {t('admin-system.staff.perm_count', { n: perms.length, total: PERMISSIONS.length })}
            </span>
          </div>
          <div className="flex flex-wrap gap-1.5">
            {PERMISSIONS.filter((p) => p !== 'MANAGE_RBAC').map((p) => {
              const on = perms.includes(p);
              return (
                <button
                  key={p}
                  type="button"
                  aria-pressed={on}
                  title={t(`admin.perm.${p}`)}
                  onClick={() => setPerms((x) => togglePerm(x, p))}
                  className={cn(
                    'font-mono text-[11px] px-[9px] py-1 rounded-pill border border-solid cursor-pointer inline-flex items-center gap-[5px] focus-visible:outline-none focus-visible:shadow-focus',
                    on
                      ? 'border-blue-500 bg-blue-50 text-blue-600'
                      : 'border-line bg-surface-card text-muted',
                  )}
                >
                  <Icon name={on ? 'check' : 'plus'} size={12} />
                  {p}
                </button>
              );
            })}
          </div>
        </div>
        <Segregation perms={perms} />
      </form>
    </Modal>
  );
}

export function RbacEditor({
  staff,
  onClose,
  onLock,
  onUnlock,
}: {
  staff: StaffMember | null;
  onClose: () => void;
  onLock: (s: StaffMember) => void;
  onUnlock: (s: StaffMember) => void;
}) {
  const { t } = useT();
  const save = useUpdatePermissions();
  const [title, setTitle] = useState<EditTitle>('CUSTOM');
  const [perms, setPerms] = useState<PermissionCode[]>([]);
  useEffect(() => {
    if (!staff) return;
    setTitle(staff.staffTitle ?? 'CUSTOM');
    setPerms(staff.permissions);
  }, [staff]);
  const pickTitle = (x: EditTitle) => {
    setTitle(x);
    setPerms((p) => presetOf(x, p));
  };
  return (
    <Modal
      open={!!staff}
      onClose={onClose}
      width={560}
      title={staff ? t('admin-system.staff.edit_title', { name: staff.fullName }) : ''}
      footer={
        staff && (
          <div className="flex items-center gap-2.5 w-full flex-wrap">
            {staff.status === 'ACTIVE' && (
              <Button variant="danger" iconLeft="lock" onClick={() => onLock(staff)}>
                {t('admin-system.staff.lock_account')}
              </Button>
            )}
            <div className="ml-auto flex gap-2.5">
              <Button variant="ghost" onClick={onClose}>
                {t('common.action.cancel')}
              </Button>
              <Button
                iconLeft="check"
                loading={save.isPending}
                onClick={() =>
                  save.mutate(
                    { id: staff.id, title: titleOf(title, perms), permissions: perms },
                    {
                      onSuccess: (warnings) => {
                        toast.success(t('admin-system.staff.saved'));
                        if (warnings.includes('SEGREGATION_OF_DUTIES'))
                          toast.info(t('admin-system.staff.segregation_logged'));
                        onClose();
                      },
                      onError: (err) => toast.error(errorMessage(err, t)),
                    },
                  )
                }
              >
                {t('admin-system.staff.save')}
              </Button>
            </div>
          </div>
        )
      }
    >
      {staff && (
        <div className="flex flex-col gap-4">
          <div>
            <Select<EditTitle>
              label={t('admin-system.staff.title')}
              value={title}
              onChange={pickTitle}
              options={EDIT_TITLES.map((x) => ({ value: x, label: t(`admin.perm.title.${x}`) }))}
            />
            <p className="m-0 mt-1.5 text-xs text-subtle">{t('admin-system.staff.title_hint')}</p>
          </div>
          <div role="group" aria-labelledby="rbac-perms">
            <div className="flex items-center mb-2">
              <span id="rbac-perms" className={lab}>
                {t('admin-system.staff.perms')}
              </span>
              <span className="ml-auto text-xs text-muted">
                {t('admin-system.staff.perm_count', { n: perms.length, total: PERMISSIONS.length })}
              </span>
            </div>
            <div className="flex flex-col border border-solid border-line-subtle rounded-md overflow-hidden">
              {PERMISSIONS.map((p, i) => {
                const on = perms.includes(p);
                const locked = p === 'MANAGE_RBAC';
                return (
                  <Checkbox
                    key={p}
                    checked={on}
                    disabled={locked}
                    onChange={() => setPerms((x) => togglePerm(x, p))}
                    className={cn(
                      '!items-start px-3.5 py-[11px]',
                      i > 0 && 'border-0 border-t border-solid border-line-subtle',
                      on ? 'bg-blue-50' : 'bg-surface-card',
                      locked && 'opacity-60',
                    )}
                  >
                    <span className="min-w-0">
                      <span
                        className={cn(
                          'block font-mono text-xs font-semibold',
                          on ? 'text-blue-600' : 'text-strong',
                        )}
                      >
                        {p}
                      </span>
                      <span className="block text-xs text-muted mt-px">{t(`admin.perm.${p}`)}</span>
                    </span>
                  </Checkbox>
                );
              })}
            </div>
          </div>
          <Segregation perms={perms} />
          {staff.status === 'LOCKED' && (
            <div className="flex items-center gap-[9px] bg-surface-sunken rounded-md px-3.5 py-2.5">
              <Icon name="lock" size={16} className="text-muted" />
              <span className="flex-1 text-sm text-body">
                {t('admin-system.staff.locked_note')}
              </span>
              <Button size="sm" variant="secondary" onClick={() => onUnlock(staff)}>
                {t('admin-system.staff.unlock')}
              </Button>
            </div>
          )}
        </div>
      )}
    </Modal>
  );
}
