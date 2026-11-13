// "Sổ địa chỉ" (prototype Account.jsx AddressPanel) — plan G10.48: danh sách (mặc định đứng đầu), thêm /
// sửa (AddressForm trong hộp), xóa (không xóa được địa chỉ mặc định — GNE-USR-4092), đặt mặc định.
import { useState } from 'react';
import { errorMessage } from '@/shared/api/errors';
import { useT } from '@/shared/hooks/useT';
import {
  Button,
  ConfirmDialog,
  EmptyState,
  Icon,
  Modal,
  PanelHead,
  Skeleton,
  toast,
} from '@/shared/ui';
import {
  useAddresses,
  useDeleteAddress,
  useSetDefaultAddress,
} from '../../application/useAddresses';
import { formatPhone, fullAddress } from '../../domain/address.rules';
import type { Address } from '../../domain/address.types';
import { panelClass } from '../ProfilePanel';
import { AddressForm } from './AddressForm';

export function AddressPanel() {
  const { t } = useT();
  const q = useAddresses();
  const setDefault = useSetDefaultAddress();
  const del = useDeleteAddress();
  const [editing, setEditing] = useState<Address | 'new' | null>(null);
  const [removing, setRemoving] = useState<Address | null>(null);
  const fail = (e: unknown) => toast.error(errorMessage(e, t));
  const link =
    'border-0 bg-transparent p-0 text-sm font-semibold cursor-pointer hover:underline min-h-8';

  return (
    <section className={panelClass} aria-labelledby="addr-title">
      <PanelHead
        id="addr-title"
        icon="map-pin"
        title={t('account.address.book')}
        sub={t('account.address.book_sub')}
        action={
          <Button iconLeft="plus" onClick={() => setEditing('new')}>
            {t('account.address.add')}
          </Button>
        }
      />
      <div className="flex flex-col gap-3 mt-4">
        {q.isPending && <Skeleton lines={4} />}
        {q.data?.length === 0 && <EmptyState icon="map-pin" text={t('account.address.empty')} />}
        {q.data?.map((a) => (
          <div
            key={a.id}
            className="flex gap-3.5 border border-solid border-line-subtle rounded-md px-4 py-3.5 flex-wrap"
          >
            <Icon name="map-pin" size={18} className="text-subtle mt-0.5 flex-none" />
            <div className="flex-1 min-w-[200px]">
              <div className="flex items-center gap-[9px] mb-[3px] flex-wrap">
                <span className="font-bold text-sm text-strong">{a.recipient}</span>
                <span className="w-px h-3 bg-line-strong" aria-hidden="true" />
                <span className="font-mono text-[13px] text-muted">{formatPhone(a.phone)}</span>
                <span className="font-semibold text-[11px] text-muted bg-surface-sunken rounded-pill px-[9px] py-0.5">
                  {t(`account.address.label.${a.label}`)}
                </span>
                {a.isDefault && (
                  <span className="font-semibold text-[11px] text-brand-600 border border-solid border-brand-200 rounded-xs px-[7px] py-px">
                    {t('account.address.default')}
                  </span>
                )}
              </div>
              <p className="m-0 text-sm text-body leading-normal">{fullAddress(a)}</p>
            </div>
            <div className="flex flex-col items-end gap-2">
              <div className="flex gap-3.5">
                <button
                  type="button"
                  className={`${link} text-brand-600`}
                  onClick={() => setEditing(a)}
                >
                  {t('common.action.edit')}
                </button>
                {!a.isDefault && (
                  <button
                    type="button"
                    className={`${link} text-red-600`}
                    onClick={() => setRemoving(a)}
                  >
                    {t('common.action.delete')}
                  </button>
                )}
              </div>
              <Button
                size="sm"
                variant="outline"
                disabled={a.isDefault || setDefault.isPending}
                onClick={() => setDefault.mutate(a.id, { onError: fail })}
              >
                {t('account.address.set_default')}
              </Button>
            </div>
          </div>
        ))}
      </div>
      <Modal
        open={editing !== null}
        onClose={() => setEditing(null)}
        dismissible={false}
        width={560}
        title={t(editing === 'new' ? 'account.address.add_title' : 'account.address.edit_title')}
      >
        {editing !== null && (
          <AddressForm
            initial={editing === 'new' ? null : editing}
            onCancel={() => setEditing(null)}
            onSaved={() => setEditing(null)}
          />
        )}
      </Modal>
      <ConfirmDialog
        open={removing !== null}
        tone="danger"
        title={t('account.address.remove_title')}
        message={t('account.address.remove_text')}
        confirmLabel={t('common.action.delete')}
        loading={del.isPending}
        onCancel={() => setRemoving(null)}
        onConfirm={() =>
          removing &&
          del.mutate(removing.id, {
            onSuccess: () => setRemoving(null),
            onError: fail,
          })
        }
      />
    </section>
  );
}
