// 404 nội bộ của SPA Admin Portal (G12.01): đường dẫn /admin/* không có màn.
import { Link } from 'react-router-dom';
import { useT } from '@/shared/hooks/useT';
import { buttonClass, EmptyState } from '@/shared/ui';

export default function NotFound({ home }: { home: string }) {
  const { t } = useT();
  return (
    <EmptyState
      icon="search"
      title={t('admin.shell.not_found.title')}
      text={t('admin.shell.not_found.desc')}
      action={
        <Link to={home} className={buttonClass({})}>
          {t('admin.shell.not_found.back')}
        </Link>
      }
    />
  );
}
