// Chỗ giữ cho màn Seller Center chưa chuyển (prototype index.html Placeholder) — bị thay ở G11 nhóm B–F.
import { useT } from '@/shared/hooks/useT';
import { Icon } from '@/shared/ui';

export default function Pending() {
  const { t } = useT();
  return (
    <div className="grid place-items-center min-h-[360px] text-center text-subtle gap-2.5">
      <div>
        <Icon name="layout-dashboard" size={40} className="mx-auto mb-2.5" />
        <p className="m-0 text-base">{t('seller.shell.pending')}</p>
      </div>
    </div>
  );
}
