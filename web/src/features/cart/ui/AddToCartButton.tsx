// Nút "Thêm vào giỏ" / "Mua ngay" (prototype ProductDetail.jsx add / onGoCart) — plan G08.26.
// Dùng bên trong island khối mua (trang sản phẩm) — island đó giữ lựa chọn phân loại và truyền
// `getLine` (trả null khi còn thiếu phân loại; tự báo lỗi). Khách chưa đăng nhập ghi giỏ khách.
import { useStore } from '@nanostores/react';
import { useState } from 'react';
import { errorMessage } from '@/shared/api/errors';
import { $session } from '@/shared/auth/session';
import { useT } from '@/shared/hooks/useT';
import { Button, toast, type ButtonSize, type ButtonVariant, type IconName } from '@/shared/ui';
import { addToCart } from '../application/cart.store';
import type { CartLineInput } from '../domain/cart.types';

export interface AddToCartButtonProps {
  getLine: () => CartLineInput | null;
  /** add: ở lại trang, báo đã thêm; buy: thêm rồi sang giỏ với dòng này được chọn sẵn. */
  mode: 'add' | 'buy';
  label: string;
  disabled?: boolean;
  variant?: ButtonVariant;
  size?: ButtonSize;
  iconLeft?: IconName;
}

export default function AddToCartButton({
  getLine,
  mode,
  label,
  disabled,
  variant,
  size = 'lg',
  iconLeft,
}: AddToCartButtonProps) {
  const { t } = useT();
  const session = useStore($session);
  const [busy, setBusy] = useState(false);

  const onClick = async () => {
    const line = getLine();
    if (!line) return;
    setBusy(true);
    const r = await addToCart(line, session.status === 'authenticated');
    setBusy(false);
    if (!r.ok) {
      toast.error(r.reason === 'guest_full' ? t('cart.add.guest_full') : errorMessage(r.error, t));
      return;
    }
    if (mode === 'buy') window.location.assign(`/cart?buy=${encodeURIComponent(r.key)}`);
    else toast.success(t('cart.add.added'));
  };

  return (
    <Button
      type="button"
      variant={variant}
      size={size}
      iconLeft={iconLeft}
      disabled={disabled}
      loading={busy}
      onClick={() => void onClick()}
    >
      {label}
    </Button>
  );
}
