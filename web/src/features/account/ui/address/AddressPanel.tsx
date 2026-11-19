// "Sổ địa chỉ" (prototype Account.jsx AddressPanel) — plan G10.48: danh sách (mặc định đứng đầu), thêm /
// sửa (AddressForm trong hộp), xóa (không xóa được địa chỉ mặc định — GNE-USR-4092), đặt mặc định.
import { useEffect, useState } from 'react';
import { errorMessage } from '@/shared/api/errors';
import { useT } from '@/shared/hooks/useT';
import { cn } from '@/shared/lib/cn';
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
  /** Địa chỉ trùng vừa bị chặn (A12.07) — viền nổi vài giây để người dùng thấy địa chỉ đã có. */
  const [highlight, setHighlight] = useState<number | null>(null);
  useEffect(() => {
    if (highlight == null) return;
    document
      .getElementById(`addr-${highlight}`)
      ?.scrollIntoView({ behavior: 'smooth', block: 'center' });
    const timer = setTimeout(() => setHighlight(null), 4000);
    return () => clearTimeout(timer);
  }, [highlight]);
  const fail = (e: unknown) => toast.error(errorMessage(e, t));
  const link =
    'border-0 bg-transparent p-0 text-sm font-semibold cursor-pointer hover:underline min-h-8';

  return (
    <section className={panelClass} aria-labelledby="addr-title">
      <PanelHead
        id="addr-title"
        icon="map-pin"
        iconTile
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
            id={`addr-${a.id}`}
            className={cn(
              'flex items-center gap-4 border border-solid rounded-md px-4 py-4 flex-wrap transition-shadow',
              a.id === highlight ? 'border-primary shadow-focus' : 'border-line-subtle',
            )}
          >
            <span
              className="flex-none grid place-items-center w-9 h-9 rounded-full bg-surface-sunken text-subtle"
              aria-hidden="true"
            >
              <Icon name="map-pin" size={17} />
            </span>
            <div className="flex-1 min-w-[220px]">
              <div className="flex items-center gap-[9px] mb-1 flex-wrap">
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
              {a.note && (
                <p className="m-0 mt-1 flex items-start gap-1.5 text-[13px] text-muted italic">
                  <Icon name="message-square" size={14} className="mt-[3px] flex-none" />
                  <span className="break-words">{a.note}</span>
                </p>
              )}
            </div>
            {/* Thao tác trên một hàng, canh giữa theo chiều dọc; địa chỉ mặc định đã có nhãn "Mặc định"
                nên không hiện nút đặt mặc định (nút mờ trước đây làm thẻ lệch) và không xóa được. */}
            <div className="flex items-center justify-end gap-4 flex-none max-sm:w-full">
              {!a.isDefault && (
                <Button
                  size="sm"
                  variant="outline"
                  disabled={setDefault.isPending}
                  onClick={() => setDefault.mutate(a.id, { onError: fail })}
                >
                  {t('account.address.set_default')}
                </Button>
              )}
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
            onDuplicate={(id) => {
              setEditing(null);
              setHighlight(id);
            }}
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
