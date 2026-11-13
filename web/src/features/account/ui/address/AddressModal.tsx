// "Địa chỉ của tôi" (prototype AddressModal.jsx) — plan G10.20, G10.48: chọn địa chỉ giao hàng, đặt
// mặc định, sửa, thêm mới (chuyển sang AddressForm trong cùng hộp). Dùng ở trang Thanh toán.
import { useEffect, useState } from 'react';
import { errorMessage } from '@/shared/api/errors';
import { useT } from '@/shared/hooks/useT';
import { cn } from '@/shared/lib/cn';
import { Button, Icon, Modal, Radio, Skeleton, toast } from '@/shared/ui';
import { useAddresses, useSetDefaultAddress } from '../../application/useAddresses';
import { formatPhone, fullAddress } from '../../domain/address.rules';
import type { Address } from '../../domain/address.types';
import { AddressForm } from './AddressForm';

export interface AddressModalProps {
  open: boolean;
  onClose: () => void;
  selectedId: number | null;
  onSelect: (a: Address) => void;
}

export function AddressModal({ open, onClose, selectedId, onSelect }: AddressModalProps) {
  const { t } = useT();
  const q = useAddresses(open);
  const setDefault = useSetDefaultAddress();
  const [mode, setMode] = useState<'list' | 'form'>('list');
  const [editing, setEditing] = useState<Address | null>(null);
  const [pick, setPick] = useState<number | null>(selectedId);

  useEffect(() => {
    if (open) {
      setMode('list');
      setEditing(null);
      setPick(selectedId);
    }
  }, [open, selectedId]);

  const list = q.data ?? [];
  const confirm = () => {
    const a = list.find((x) => x.id === pick) ?? list.find((x) => x.isDefault);
    if (a) onSelect(a);
    onClose();
  };

  const title =
    mode === 'list'
      ? t('account.address.modal_title')
      : t(editing ? 'account.address.edit_title' : 'account.address.add_title');

  return (
    <Modal
      open={open}
      onClose={onClose}
      title={title}
      width={560}
      dismissible={mode === 'list'}
      footer={
        mode === 'list' ? (
          <>
            <Button variant="ghost" onClick={onClose}>
              {t('common.action.cancel')}
            </Button>
            <Button iconLeft="check" onClick={confirm} disabled={list.length === 0}>
              {t('common.action.confirm')}
            </Button>
          </>
        ) : undefined
      }
    >
      {mode === 'form' ? (
        <AddressForm
          initial={editing}
          onCancel={() => setMode('list')}
          onSaved={(a) => {
            setPick(a.id);
            setMode('list');
            setEditing(null);
          }}
        />
      ) : (
        <div className="flex flex-col gap-2.5" role="radiogroup" aria-label={title}>
          {q.isPending && [0, 1].map((i) => <Skeleton key={i} className="h-[92px]" rounded="md" />)}
          {list.map((a) => {
            const on = a.id === pick;
            return (
              <div
                key={a.id}
                className={cn(
                  'flex gap-3 items-start rounded-md px-[15px] py-[13px] border-[1.5px] border-solid',
                  on ? 'border-primary bg-primary-tint' : 'border-line bg-surface-card',
                )}
              >
                <Radio
                  name="address"
                  checked={on}
                  onChange={() => setPick(a.id)}
                  aria-label={`${a.recipient}, ${fullAddress(a)}`}
                />
                <div className="flex-1 min-w-0">
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
                  <div className="flex gap-4 mt-[7px]">
                    <button
                      type="button"
                      className="border-0 bg-transparent p-0 text-sm text-brand-600 font-semibold cursor-pointer hover:underline"
                      onClick={() => {
                        setEditing(a);
                        setMode('form');
                      }}
                    >
                      {t('common.action.edit')}
                    </button>
                    {!a.isDefault && (
                      <button
                        type="button"
                        className="border-0 bg-transparent p-0 text-sm text-brand-600 font-semibold cursor-pointer hover:underline"
                        disabled={setDefault.isPending}
                        onClick={() =>
                          setDefault.mutate(a.id, {
                            onError: (e) => toast.error(errorMessage(e, t)),
                          })
                        }
                      >
                        {t('account.address.set_default')}
                      </button>
                    )}
                  </div>
                </div>
              </div>
            );
          })}
          <button
            type="button"
            onClick={() => {
              setEditing(null);
              setMode('form');
            }}
            className="flex items-center justify-center gap-2 mt-1 h-[46px] rounded-md bg-surface-sunken text-brand-600 font-semibold text-sm cursor-pointer border-[1.5px] border-dashed border-line-strong"
          >
            <Icon name="plus" size={17} /> {t('account.address.add')}
          </button>
        </div>
      )}
    </Modal>
  );
}
