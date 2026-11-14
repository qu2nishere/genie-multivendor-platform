// Màn chưa chuyển (thay dần ở G12 nhóm B–G).
import { useT } from '@/shared/hooks/useT';
import { EmptyState } from '@/shared/ui';

export default function Pending() {
  const { t } = useT();
  return <EmptyState icon="boxes" title={t('admin.shell.pending')} />;
}
