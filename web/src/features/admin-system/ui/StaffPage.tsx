// Phân quyền nhân viên (prototype admin/Finance.jsx Rbac) — plan G12.40: "5 nhân viên · 3 chức danh +
// Admin · 13 mã quyền", tìm tên / email, bảng Nhân viên / Chức danh / Quyền (di chuột xem mô tả) / Trạng
// thái; Gửi lại lời mời / Sửa quyền / Khóa (lý do) / Mở khóa / Xóa; Mời nhân viên mới.
import { useEffect, useState } from 'react';
import { useForm } from 'react-hook-form';
import { errorMessage } from '@/shared/api/errors';
import { PERMISSIONS } from '@/shared/auth/permissions';
import { useT } from '@/shared/hooks/useT';
import { cn } from '@/shared/lib/cn';
import { zodResolver } from '@/shared/lib/zodResolver';
import {
  Avatar,
  Badge,
  Button,
  ConfirmDialog,
  EmptyState,
  Icon,
  Input,
  Modal,
  Skeleton,
  toast,
} from '@/shared/ui';
import {
  useDeleteStaff,
  useLockStaff,
  useResendInvitation,
  useStaff,
  useUnlockStaff,
} from '../application/useSystem';
import { staffActions } from '../domain/system.rules';
import { lockStaffSchema, type LockStaffValues } from '../domain/system.schema';
import type { StaffMember } from '../domain/system.types';
import { InviteStaffModal, RbacEditor } from './StaffModals';

const th = 'px-3 py-[11px] whitespace-nowrap font-semibold text-xs text-muted text-left bg-gray-25';
const td = 'px-3 py-3 whitespace-nowrap';

function LockModal({ s, onClose }: { s: StaffMember | null; onClose: () => void }) {
  const { t } = useT();
  const lock = useLockStaff();
  const form = useForm<LockStaffValues>({
    resolver: zodResolver(lockStaffSchema),
    defaultValues: { reason: '' },
  });
  useEffect(() => {
    if (s) form.reset({ reason: '' });
  }, [s, form]);
  const reason = form.watch('reason');
  const submit = form.handleSubmit(
    (v) =>
      s &&
      lock.mutate(
        { id: s.id, reason: v.reason },
        {
          onSuccess: () => {
            toast.success(t('admin-system.staff.locked'));
            onClose();
          },
          onError: (e) => toast.error(errorMessage(e, t)),
        },
      ),
  );
  return (
    <Modal
      open={!!s}
      onClose={onClose}
      title={s ? t('admin-system.staff.lock_title', { name: s.fullName }) : ''}
      footer={
        <>
          <Button variant="ghost" onClick={onClose}>
            {t('common.action.cancel')}
          </Button>
          <Button
            variant="danger"
            iconLeft="lock"
            disabled={!reason.trim()}
            loading={lock.isPending}
            onClick={() => void submit()}
          >
            {t('admin-system.staff.lock_confirm')}
          </Button>
        </>
      }
    >
      <form onSubmit={(ev) => void submit(ev)} noValidate>
        <Input
          label={t('admin-system.staff.lock_reason')}
          placeholder={t('admin-system.staff.lock_placeholder')}
          maxLength={500}
          {...form.register('reason')}
        />
      </form>
    </Modal>
  );
}

export default function StaffPage() {
  const { t } = useT();
  const [q, setQ] = useState('');
  const [term, setTerm] = useState('');
  useEffect(() => {
    const id = setTimeout(() => setTerm(q.trim()), 250);
    return () => clearTimeout(id);
  }, [q]);
  const list = useStaff(term);
  const all = useStaff('');
  const resend = useResendInvitation();
  const unlock = useUnlockStaff();
  const del = useDeleteStaff();
  const [inviting, setInviting] = useState(false);
  const [editing, setEditing] = useState<StaffMember | null>(null);
  const [locking, setLocking] = useState<StaffMember | null>(null);
  const [deleting, setDeleting] = useState<StaffMember | null>(null);
  if (list.isError)
    return (
      <EmptyState
        icon="triangle-alert"
        title={t('common.state.error')}
        text={errorMessage(list.error, t)}
      />
    );
  const rows = list.data ?? [];
  const doUnlock = (s: StaffMember) =>
    unlock.mutate(s.id, {
      onSuccess: () => {
        toast.success(t('admin-system.staff.unlocked'));
        setEditing(null);
      },
      onError: (e) => toast.error(errorMessage(e, t)),
    });
  return (
    <div className="max-w-[1180px]">
      <div className="flex items-center gap-3 mb-3.5 flex-wrap">
        <span className="text-sm text-muted">
          {t('admin-system.staff.summary', {
            count: all.data?.length ?? 0,
            perms: PERMISSIONS.length,
          })}
        </span>
        <Button
          size="sm"
          iconLeft="plus-circle"
          className="ml-auto"
          onClick={() => setInviting(true)}
        >
          {t('admin-system.staff.invite')}
        </Button>
      </div>
      <Input
        aria-label={t('admin-system.staff.search')}
        wrapperClassName="max-w-[380px] mb-3"
        iconLeft="search"
        value={q}
        onChange={(e) => setQ(e.target.value)}
        placeholder={t('admin-system.staff.search_placeholder')}
      />
      <div className="relative bg-surface-card rounded-lg shadow-sm overflow-x-auto">
        {list.isPending ? (
          <Skeleton lines={5} className="m-5" />
        ) : (
          <table className="w-full min-w-[760px] border-collapse">
            <thead>
              <tr>
                <th className={th}>{t('admin-system.staff.col.staff')}</th>
                <th className={th}>{t('admin-system.staff.col.title')}</th>
                <th className={th}>{t('admin-system.staff.col.perms')}</th>
                <th className={th}>{t('admin-system.staff.col.status')}</th>
                <th className={cn(th, 'text-right')}>
                  <span className="sr-only">{t('admin-system.staff.col.action')}</span>
                </th>
              </tr>
            </thead>
            <tbody>
              {rows.length === 0 && (
                <tr>
                  <td colSpan={5} className="p-9 text-center text-base text-muted">
                    {t('admin-system.staff.empty')}
                  </td>
                </tr>
              )}
              {rows.map((s) => {
                const admin = s.role === 'ADMIN';
                const acts = staffActions(s);
                return (
                  <tr
                    key={s.id}
                    className="border-0 border-t border-solid border-line-subtle text-[13px] text-body"
                  >
                    <td className={td}>
                      <div className="flex items-center gap-2.5">
                        <Avatar name={s.fullName} size={34} />
                        <div>
                          <div className="text-[13px] font-semibold text-strong">{s.fullName}</div>
                          <div className="font-mono text-[11px] text-muted">{s.email}</div>
                        </div>
                      </div>
                    </td>
                    <td className={td}>
                      <Badge variant={admin ? 'primary' : 'outline'}>
                        {t(`admin.perm.title.${admin ? 'ADMIN' : (s.staffTitle ?? 'CUSTOM')}`)}
                      </Badge>
                    </td>
                    <td className={td}>
                      <div className="flex gap-1 flex-wrap max-w-[300px] whitespace-normal">
                        {admin ? (
                          <span
                            title={t('admin-system.staff.all_perms_desc')}
                            className="font-mono text-[11px] bg-gray-100 text-body px-1.5 py-0.5 rounded-xs cursor-help"
                          >
                            {t('admin-system.staff.all_perms')}
                          </span>
                        ) : (
                          s.permissions.map((p) => (
                            <span
                              key={p}
                              title={t(`admin.perm.${p}`)}
                              className="font-mono text-[11px] bg-gray-100 text-body px-1.5 py-0.5 rounded-xs cursor-help"
                            >
                              {p}
                            </span>
                          ))
                        )}
                      </div>
                    </td>
                    <td className={td}>
                      {s.status === 'PENDING_ACTIVATION' ? (
                        <Badge variant="warning" icon="clock">
                          {t('admin-system.staff.status.PENDING_ACTIVATION')}
                        </Badge>
                      ) : (
                        <Badge variant={s.status === 'ACTIVE' ? 'success' : 'neutral'}>
                          {t(`admin-system.staff.status.${s.status}`)}
                        </Badge>
                      )}
                    </td>
                    <td className={cn(td, 'text-right')}>
                      {acts.length === 0 ? (
                        <span className="text-xs text-subtle">{t('admin-system.staff.yours')}</span>
                      ) : (
                        <div className="flex gap-1 justify-end">
                          {acts.includes('resend') && (
                            <Button
                              variant="ghost"
                              size="sm"
                              iconLeft="send"
                              onClick={() =>
                                resend.mutate(s.id, {
                                  onSuccess: () =>
                                    toast.success(
                                      t('admin-system.staff.resent', { email: s.email }),
                                    ),
                                  onError: (e) => toast.error(errorMessage(e, t)),
                                })
                              }
                            >
                              {t('admin-system.staff.resend')}
                            </Button>
                          )}
                          {acts.includes('edit') && (
                            <Button
                              variant="ghost"
                              size="sm"
                              iconLeft="pencil"
                              onClick={() => setEditing(s)}
                            >
                              {t('admin-system.staff.edit')}
                            </Button>
                          )}
                          {acts.includes('lock') && (
                            <Button
                              variant="ghost"
                              size="sm"
                              iconLeft="lock"
                              onClick={() => setLocking(s)}
                            >
                              {t('admin-system.staff.lock')}
                            </Button>
                          )}
                          {acts.includes('unlock') && (
                            <Button
                              variant="ghost"
                              size="sm"
                              iconLeft="lock"
                              onClick={() => doUnlock(s)}
                            >
                              {t('admin-system.staff.unlock')}
                            </Button>
                          )}
                          {acts.includes('delete') && (
                            <Button
                              variant="ghost"
                              size="sm"
                              iconLeft="trash-2"
                              className="!text-red-600"
                              onClick={() => setDeleting(s)}
                            >
                              {t('admin-system.staff.delete')}
                            </Button>
                          )}
                        </div>
                      )}
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        )}
      </div>
      <InviteStaffModal open={inviting} onClose={() => setInviting(false)} />
      <RbacEditor
        staff={editing}
        onClose={() => setEditing(null)}
        onLock={(s) => {
          setEditing(null);
          setLocking(s);
        }}
        onUnlock={doUnlock}
      />
      <LockModal s={locking} onClose={() => setLocking(null)} />
      <ConfirmDialog
        open={!!deleting}
        tone="danger"
        title={t('admin-system.staff.delete_title')}
        message={
          <>
            <b className="text-red-600">{t('admin-system.staff.warning')}</b>{' '}
            {t('admin-system.staff.delete_desc', { name: deleting?.fullName ?? '' })}
          </>
        }
        confirmLabel={t('admin-system.staff.delete_confirm')}
        loading={del.isPending}
        onCancel={() => setDeleting(null)}
        onConfirm={() =>
          deleting &&
          del.mutate(deleting.id, {
            onSuccess: () => {
              toast.success(t('admin-system.staff.deleted'));
              setDeleting(null);
            },
            onError: (e) => toast.error(errorMessage(e, t)),
          })
        }
      />
      <p className="mt-3 flex items-center gap-1.5 text-xs text-subtle">
        <Icon name="info" size={13} />
        {t('admin-system.staff.footnote')}
      </p>
    </div>
  );
}
