// Dải voucher lưu nhanh (prototype Home.jsx SaveVoucher) — plan G08.12, island client:idle.
// Phiếu kiểu vé giấy; khách bấm "Lưu" → nhắc đăng nhập.
import { useStore } from '@nanostores/react';
import { Providers } from '@/shared/providers/Providers';
import { requireLogin } from '@/features/auth';
import { errorMessage } from '@/shared/api/errors';
import { $session } from '@/shared/auth/session';
import { useT } from '@/shared/hooks/useT';
import { Ticket, TicketSaveButton, toast } from '@/shared/ui';
import type { VoucherKind } from '../domain/voucher.types';
import { useSaveVoucher, useVouchersByCodes } from '../application/useVouchers';

export interface SaveVoucherItem {
  code: string;
  kind: VoucherKind;
  amount: string;
  label: string;
}

function Strip({ items }: { items: SaveVoucherItem[] }) {
  const { t } = useT();
  const session = useStore($session);
  const q = useVouchersByCodes(
    items.map((i) => i.code),
    session.status === 'authenticated',
  );
  const save = useSaveVoucher(
    () => toast.success(t('promotions.voucher.saved_toast')),
    (e) => toast.error(errorMessage(e, t)),
  );
  const saved = (code: string) =>
    session.status === 'authenticated' && !!q.data?.find((v) => v.code === code)?.saved;

  return (
    <div className="tr-vouchers">
      {items.map((v) => (
        <Ticket
          key={v.code}
          kind={v.kind}
          amount={v.amount}
          label={v.label}
          action={
            <TicketSaveButton
              saved={saved(v.code)}
              label={v.label}
              onSave={() => {
                if (session.status !== 'authenticated') {
                  requireLogin();
                  return;
                }
                save.mutate(v.code);
              }}
            />
          }
        />
      ))}
    </div>
  );
}

export default function SaveVoucher({ items }: { items: SaveVoucherItem[] }) {
  return (
    <Providers>
      <Strip items={items} />
    </Providers>
  );
}
