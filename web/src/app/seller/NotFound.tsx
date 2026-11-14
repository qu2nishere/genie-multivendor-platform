// 404 nội bộ của SPA Seller Center (G11.01): đường dẫn /seller/* không có màn.
import { Link } from 'react-router-dom';
import { useT } from '@/shared/hooks/useT';
import { buttonClass, EmptyState } from '@/shared/ui';

export default function NotFound() {
  const { t } = useT();
  return (
    <EmptyState
      icon="search"
      title={t('seller.shell.not_found.title')}
      text={t('seller.shell.not_found.desc')}
      action={
        <Link to="/" className={buttonClass({})}>
          {t('seller.shell.not_found.back')}
        </Link>
      }
    />
  );
}
