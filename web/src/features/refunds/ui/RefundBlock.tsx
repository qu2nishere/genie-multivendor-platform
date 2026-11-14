// Trạng thái yêu cầu trả hàng trong trang theo dõi đơn (prototype OrderTracking.jsx khối "Yêu cầu RFN-…")
// — plan G10.41: phương án + số tiền, lý do, hạn của bước hiện tại, lời nhắn của shop; hành động theo
// trạng thái: bổ sung bằng chứng, khiếu nại lên Sàn, chấp nhận shop từ chối. Không có nút "Mô phỏng" (D5).
import { useState } from 'react';
import { errorMessage } from '@/shared/api/errors';
import type { UploadedFile } from '@/shared/api/upload';
import { useT } from '@/shared/hooks/useT';
import { formatDateTime, formatVND } from '@/shared/lib/format';
import { Badge, Button, Icon, MediaPicker, toast } from '@/shared/ui';
import { useAcceptRejection, useAddEvidence, useEscalateRefund } from '../application/useRefunds';
import { REFUND_TONE, needsBank } from '../domain/refund.rules';
import type { RefundRequest } from '../domain/refund.types';

export interface RefundBlockProps {
  refund: RefundRequest;
  paymentMethod: string;
}

export function RefundBlock({ refund: r, paymentMethod }: RefundBlockProps) {
  const { t, locale } = useT();
  const [more, setMore] = useState<UploadedFile[]>([]);
  const add = useAddEvidence();
  const escalate = useEscalateRefund();
  const accept = useAcceptRejection();
  const fail = (e: unknown) => toast.error(errorMessage(e, t));
  const tone = REFUND_TONE[r.status] ?? 'neutral';

  return (
    <section
      id="return"
      className="bg-surface-card rounded-lg border border-solid border-line-subtle shadow-sm p-5 scroll-mt-28"
      aria-labelledby="refund-title"
    >
      <div className="flex items-center gap-2.5 mb-2.5 flex-wrap">
        <Icon name="undo-2" size={18} className="text-brand-500" />
        <h2 id="refund-title" className="m-0 text-xl font-semibold text-strong">
          {t('refunds.block.title', { code: r.code })}
        </h2>
        <Badge variant={tone} className="ml-auto">
          {t(`refunds.status.${r.status}.label`)}
        </Badge>
      </div>
      <div className="text-sm text-body leading-relaxed">
        <p className="m-0">
          {t('refunds.block.plan')} <b>{t(`refunds.type.${r.requestType}`)}</b> ·{' '}
          {formatVND(r.refundAmount, { locale })}
        </p>
        <p className="m-0">
          {t('refunds.block.reason')} {t(`refunds.reason.${r.reasonCode}`)}
        </p>
        {r.shopMessage && (
          <p className="m-0">
            {t('refunds.block.shop_said')} “{r.shopMessage}”
          </p>
        )}
        <p className="m-0 mt-1 text-muted">{t(`refunds.status.${r.status}.note`)}</p>
        {r.deadlineAt && !['REFUNDED', 'REJECTED', 'DISPUTED'].includes(r.status) && (
          <p className="m-0 text-muted">
            {t('refunds.block.deadline', { date: formatDateTime(r.deadlineAt, { locale }) })}
          </p>
        )}
        {needsBank(paymentMethod) && (
          <p className="m-0 text-muted">
            {t('refunds.block.bank', { method: t(`orders.pay.${paymentMethod}`) })}
            {r.refundBankMasked ? ` (${r.refundBankMasked})` : ''}
          </p>
        )}
        {r.resolvedAt && (
          <p className="m-0 text-muted">
            {t('refunds.block.resolved', { date: formatDateTime(r.resolvedAt, { locale }) })}
          </p>
        )}
      </div>

      {r.status === 'AWAITING_EVIDENCE' && (
        <div className="mt-3 flex flex-col gap-2.5">
          <MediaPicker
            label={t('refunds.block.more')}
            value={more}
            onChange={setMore}
            accept="image/jpeg,image/png,video/mp4"
            max={4}
            purposeOf={() => 'REFUND_EVIDENCE'}
          />
          <div className="flex gap-2 flex-wrap">
            <Button
              size="sm"
              disabled={!more.length}
              loading={add.isPending}
              onClick={() =>
                add.mutate(
                  {
                    r,
                    evidence: more.map((f) => ({ objectKey: f.objectKey, mediaType: f.mediaType })),
                  },
                  {
                    onSuccess: () => {
                      setMore([]);
                      toast.success(t('refunds.block.more_sent'));
                    },
                    onError: fail,
                  },
                )
              }
            >
              {t('refunds.block.send_more')}
            </Button>
            <Button
              size="sm"
              variant="ghost"
              iconLeft="triangle-alert"
              loading={escalate.isPending}
              onClick={() => escalate.mutate(r, { onError: fail })}
            >
              {t('refunds.block.escalate')}
            </Button>
          </div>
        </div>
      )}
      {r.status === 'SHOP_REJECTED' && (
        <div className="flex gap-2 mt-3 flex-wrap">
          <Button
            size="sm"
            variant="outline"
            iconLeft="check"
            loading={accept.isPending}
            onClick={() => accept.mutate(r, { onError: fail })}
          >
            {t('refunds.block.accept')}
          </Button>
          <Button
            size="sm"
            iconLeft="triangle-alert"
            loading={escalate.isPending}
            onClick={() => escalate.mutate(r, { onError: fail })}
          >
            {t('refunds.block.escalate')}
          </Button>
        </div>
      )}
    </section>
  );
}
