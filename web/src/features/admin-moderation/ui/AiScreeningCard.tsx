// Thẻ bật / tắt kiểm duyệt sản phẩm tự động bằng Google Gemini — UC-CAT-12 (plan C1 P06; c1_auto_product.md mục 5.4)
import { useStore } from '@nanostores/react';
import { useState } from 'react';
import { errorMessage } from '@/shared/api/errors';
import { $session } from '@/shared/auth/session';
import { useT } from '@/shared/hooks/useT';
import { formatDateTime } from '@/shared/lib/format';
import { ConfirmDialog, Icon, Skeleton, Switch, toast } from '@/shared/ui';
import { useAiSettings, useSaveAiSettings } from '../application/useModeration';

export default function AiScreeningCard() {
  const { t, locale } = useT();
  const session = useStore($session);
  const q = useAiSettings();
  const save = useSaveAiSettings();
  const [confirming, setConfirming] = useState<boolean | null>(null);

  const isAdmin = session.status === 'authenticated' && session.user.roles.includes('ADMIN');

  if (q.isPending) {
    return (
      <section className="bg-surface-card rounded-lg shadow-sm p-5">
        <Skeleton lines={3} />
      </section>
    );
  }
  if (q.isError || !q.data) {
    return null;
  }

  const s = q.data;
  const modeLabel = t(
    s.mode === 'http'
      ? 'admin-moderation.product.ai_settings.mode.http'
      : 'admin-moderation.product.ai_settings.mode.stub',
  );

  const handleConfirm = () => {
    if (confirming === null) return;
    const next = confirming;
    save.mutate(next, {
      onSuccess: () => {
        toast.success(
          t(
            next
              ? 'admin-moderation.product.ai_settings.saved_on'
              : 'admin-moderation.product.ai_settings.saved_off',
          ),
        );
        setConfirming(null);
      },
      onError: (e) => {
        toast.error(errorMessage(e, t));
        setConfirming(null);
      },
    });
  };

  return (
    <section
      className="bg-surface-card rounded-lg shadow-sm overflow-hidden"
      aria-labelledby="mod-ai-screening"
    >
      <div className="flex items-start gap-3 px-5 py-4">
        <div className="flex-1 min-w-0 space-y-1.5">
          <div className="flex items-center gap-2">
            <Icon name="shield-check" size={18} className="text-primary flex-none" />
            <h2 id="mod-ai-screening" className="m-0 text-base font-bold text-strong">
              {t('admin-moderation.product.ai_settings.title')}
            </h2>
          </div>
          <p className="m-0 text-sm text-body">
            {t('admin-moderation.product.ai_settings.desc', { threshold: s.rejectThreshold })}
          </p>
          <p className="m-0 text-xs text-muted">
            {t('admin-moderation.product.ai_settings.meta', {
              model: s.model,
              mode: modeLabel,
              threshold: s.rejectThreshold,
            })}
          </p>
          {s.updatedAt && s.updatedBy && (
            <p className="m-0 text-xs text-muted">
              {t('admin-moderation.product.ai_settings.updated', {
                time: formatDateTime(s.updatedAt, { locale }),
                name: s.updatedBy,
              })}
            </p>
          )}
          {!s.configured && (
            <p className="m-0 mt-1 flex items-center gap-1.5 text-xs font-medium text-amber-600">
              <Icon name="triangle-alert" size={14} className="flex-none" />
              <span>{t('admin-moderation.product.ai_settings.not_configured')}</span>
            </p>
          )}
          {!isAdmin && (
            <p className="m-0 mt-1 flex items-center gap-1.5 text-xs text-muted">
              <span aria-hidden="true">ⓘ</span>
              <span>{t('admin-moderation.product.ai_settings.admin_only')}</span>
            </p>
          )}
        </div>
        <div className="pt-0.5 flex-none">
          <Switch
            checked={s.enabled}
            disabled={!isAdmin || save.isPending}
            aria-label={t('admin-moderation.product.ai_settings.toggle')}
            onChange={() => setConfirming(!s.enabled)}
          />
        </div>
      </div>

      <ConfirmDialog
        open={confirming !== null}
        onCancel={() => setConfirming(null)}
        title={t('admin-moderation.product.ai_settings.title')}
        message={t(
          confirming
            ? 'admin-moderation.product.ai_settings.confirm_on'
            : 'admin-moderation.product.ai_settings.confirm_off',
        )}
        confirmLabel={t('common.action.confirm')}
        loading={save.isPending}
        onConfirm={handleConfirm}
      />
    </section>
  );
}
