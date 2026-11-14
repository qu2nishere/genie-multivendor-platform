// Xử lý rút tiền (prototype admin/Finance.jsx Withdrawals) — plan G12.26: 3 chỉ số; bảng Mã / Người bán /
// Tài khoản nhận / Số tiền / KYC / Yêu cầu; Từ chối (lý do 16.4, hoàn ví) / Duyệt chi (hỏi lại) → hộp
// chuyển khoản VietQR nội dung TRWD…; "Xác nhận tay" bằng mã giao dịch ngân hàng. Hệ thống thật tự hoàn
// tất khi SePay báo tiền ra; mock dùng nút "Đã chuyển khoản" thay "Mô phỏng: SePay báo…" (D5).
import { useState } from 'react';
import { errorMessage } from '@/shared/api/errors';
import { useT } from '@/shared/hooks/useT';
import { cn } from '@/shared/lib/cn';
import { formatDateTime, formatVND } from '@/shared/lib/format';
import {
  Avatar,
  Badge,
  Button,
  EmptyState,
  Icon,
  Input,
  Modal,
  QrPlaceholder,
  Select,
  Skeleton,
  StatusBadge,
  Textarea,
  toast,
} from '@/shared/ui';
import { StatTile } from '@/shared/ui/dash';
import {
  useApproveWithdrawal,
  useCompleteWithdrawal,
  useRejectWithdrawal,
  useWithdrawals,
  useWithdrawalTransfer,
} from '../application/useFinance';
import {
  WITHDRAW_REJECT_REASONS,
  withdrawalView,
  type WithdrawRejectReason,
} from '../domain/finance.rules';
import type { Withdrawal } from '../domain/finance.types';

const th = 'px-3 py-[11px] whitespace-nowrap font-semibold text-xs text-muted text-left bg-gray-25';
const td = 'px-3 py-3 whitespace-nowrap';

function TransferModal({ w, onClose }: { w: Withdrawal | null; onClose: () => void }) {
  const { t, locale } = useT();
  const q = useWithdrawalTransfer(w?.id ?? null);
  const complete = useCompleteWithdrawal();
  const [manual, setManual] = useState(false);
  const [ref, setRef] = useState('');
  const close = () => {
    setManual(false);
    setRef('');
    onClose();
  };
  const tr = q.data;
  const finish = (bankReference: string) =>
    w &&
    complete.mutate(
      { id: w.id, bankReference },
      {
        onSuccess: () => {
          toast.success(t('admin-finance.withdrawals.completed', { code: w.code }));
          close();
        },
        onError: (e) => toast.error(errorMessage(e, t)),
      },
    );
  return (
    <Modal
      open={!!w}
      onClose={close}
      width={540}
      title={w ? t('admin-finance.withdrawals.transfer_title', { code: w.code }) : ''}
      footer={
        <>
          <Button variant="ghost" onClick={close}>
            {t('common.action.close')}
          </Button>
          {manual ? (
            <Button
              iconLeft="check"
              disabled={!ref.trim()}
              loading={complete.isPending}
              onClick={() => finish(ref.trim())}
            >
              {t('admin-finance.withdrawals.manual_confirm')}
            </Button>
          ) : (
            <Button
              iconLeft="circle-check"
              loading={complete.isPending}
              disabled={!tr}
              onClick={() => finish(tr?.transferContent ?? '')}
            >
              {t('admin-finance.withdrawals.transferred')}
            </Button>
          )}
        </>
      }
    >
      {!tr ? (
        <Skeleton lines={4} />
      ) : (
        <div>
          <div className="flex gap-[18px] items-center max-sm:flex-col max-sm:items-start">
            <QrPlaceholder seed={tr.qrPayload} size={148} />
            <dl className="flex-1 min-w-0 flex flex-col gap-[7px] text-sm m-0">
              {(
                [
                  [t('admin-finance.withdrawals.bank'), `${tr.bankName} · ${tr.accountNumber}`],
                  [t('admin-finance.withdrawals.holder'), tr.accountHolder],
                  [t('admin-finance.withdrawals.amount'), formatVND(tr.amount, { locale })],
                  [t('admin-finance.withdrawals.content'), tr.transferContent],
                ] as const
              ).map(([k, v], i) => (
                <div key={k}>
                  <dt className="inline text-muted">{k}: </dt>
                  <dd className={cn('inline m-0 font-bold', i === 3 && 'font-mono')}>{v}</dd>
                </div>
              ))}
              <p className="m-0 text-xs text-muted">{t('admin-finance.withdrawals.qr_hint')}</p>
            </dl>
          </div>
          <div className="mt-3.5 pt-3 border-0 border-t border-solid border-line-subtle">
            {manual ? (
              <Input
                label={t('admin-finance.withdrawals.bank_ref')}
                placeholder="FT26170123456"
                value={ref}
                onChange={(e) => setRef(e.target.value)}
              />
            ) : (
              <button
                type="button"
                onClick={() => setManual(true)}
                className="border-0 bg-transparent p-0 text-xs font-semibold text-blue-600 cursor-pointer hover:underline"
              >
                {t('admin-finance.withdrawals.manual_link')}
              </button>
            )}
          </div>
        </div>
      )}
    </Modal>
  );
}

function RejectModal({ w, onClose }: { w: Withdrawal | null; onClose: () => void }) {
  const { t, locale } = useT();
  const reject = useRejectWithdrawal();
  const [reason, setReason] = useState<WithdrawRejectReason>('ACCOUNT_MISMATCH');
  const [note, setNote] = useState('');
  const needNote = reason === 'OTHER' && !note.trim();
  return (
    <Modal
      open={!!w}
      onClose={onClose}
      title={w ? t('admin-finance.withdrawals.reject_title', { code: w.code }) : ''}
      footer={
        <>
          <Button variant="ghost" onClick={onClose}>
            {t('common.action.cancel')}
          </Button>
          <Button
            variant="danger"
            disabled={needNote}
            loading={reject.isPending}
            onClick={() =>
              w &&
              reject.mutate(
                { id: w.id, reason, note: note.trim() || null },
                {
                  onSuccess: () => {
                    toast.success(t('admin-finance.withdrawals.rejected', { code: w.code }));
                    setNote('');
                    onClose();
                  },
                  onError: (e) => toast.error(errorMessage(e, t)),
                },
              )
            }
          >
            {t('admin-finance.withdrawals.reject_submit')}
          </Button>
        </>
      }
    >
      <div className="flex flex-col gap-3">
        <Select<WithdrawRejectReason>
          label={t('admin-finance.withdrawals.reject_reason')}
          value={reason}
          onChange={setReason}
          options={WITHDRAW_REJECT_REASONS.map((r) => ({
            value: r,
            label: t(`admin-finance.withdrawals.reasons.${r}`),
          }))}
        />
        {reason === 'OTHER' && (
          <Textarea
            aria-label={t('admin-finance.withdrawals.note')}
            placeholder={t('admin-finance.withdrawals.note')}
            rows={2}
            maxLength={500}
            value={note}
            onChange={(e) => setNote(e.target.value)}
          />
        )}
        {w && (
          <p className="m-0 text-sm text-muted leading-normal">
            {t('admin-finance.withdrawals.reject_desc', {
              amount: formatVND(w.amount, { locale }),
              shop: w.shop.name,
            })}
          </p>
        )}
      </div>
    </Modal>
  );
}

export default function WithdrawalsPage() {
  const { t, locale } = useT();
  const q = useWithdrawals();
  const approve = useApproveWithdrawal();
  const [asking, setAsking] = useState<Withdrawal | null>(null);
  const [rejecting, setRejecting] = useState<Withdrawal | null>(null);
  const [paying, setPaying] = useState<Withdrawal | null>(null);
  if (q.isPending) return <Skeleton lines={8} />;
  if (q.isError)
    return (
      <EmptyState
        icon="triangle-alert"
        title={t('common.state.error')}
        text={errorMessage(q.error, t)}
      />
    );
  const { items, summary } = q.data;
  const kycLabel = (s: Withdrawal['shopKycStatus']) => t(`admin-finance.withdrawals.kyc.${s}`);
  return (
    <div className="max-w-[1180px]">
      <div className="grid grid-cols-3 gap-4 mb-4 max-md:grid-cols-1">
        <StatTile
          label={t('admin-finance.withdrawals.kpi.pending')}
          value={t('admin-finance.withdrawals.kpi.requests', { count: summary.pendingCount })}
        />
        <StatTile
          label={t('admin-finance.withdrawals.kpi.payable')}
          value={formatVND(summary.payableAmount, { locale })}
        />
        <StatTile
          label={t('admin-finance.withdrawals.kpi.blocked')}
          value={formatVND(summary.blockedAmount, { locale })}
        />
      </div>
      <div className="bg-surface-card rounded-lg shadow-sm overflow-x-auto">
        <table className="w-full min-w-[900px] border-collapse tabular-nums">
          <thead>
            <tr>
              <th className={th}>{t('admin-finance.withdrawals.col.code')}</th>
              <th className={th}>{t('admin-finance.withdrawals.col.shop')}</th>
              <th className={th}>{t('admin-finance.withdrawals.col.account')}</th>
              <th className={cn(th, 'text-right')}>{t('admin-finance.withdrawals.col.amount')}</th>
              <th className={th}>KYC</th>
              <th className={th}>{t('admin-finance.withdrawals.col.requested')}</th>
              <th className={cn(th, 'text-right')}>{t('admin-finance.withdrawals.col.action')}</th>
            </tr>
          </thead>
          <tbody>
            {items.map((w) => {
              const view = withdrawalView(w);
              return (
                <tr
                  key={w.id}
                  className="border-0 border-t border-solid border-line-subtle text-[13px] text-body"
                >
                  <td className={cn(td, 'font-mono text-xs')}>{w.code}</td>
                  <td className={td}>
                    <span className="flex items-center gap-2">
                      <Avatar name={w.shop.name} size={26} square />
                      {w.shop.name}
                    </span>
                  </td>
                  <td className={cn(td, 'font-mono text-muted')}>
                    <span className="inline-flex items-center gap-1">
                      {w.bankAccountMasked}
                      {w.accountVerified && (
                        <Icon
                          name="badge-check"
                          size={13}
                          className="text-mint-600"
                          aria-label={t('admin-finance.withdrawals.verified_account')}
                        />
                      )}
                    </span>
                  </td>
                  <td className={cn(td, 'text-right font-bold')}>
                    {formatVND(w.amount, { locale })}
                  </td>
                  <td className={td}>
                    <StatusBadge
                      status={w.shopKycStatus === 'VERIFIED' ? 'VERIFIED' : 'PENDING'}
                      label={kycLabel(w.shopKycStatus)}
                      dot={false}
                    />
                  </td>
                  <td className={cn(td, 'text-muted text-xs')}>
                    {formatDateTime(w.requestedAt, { locale })}
                  </td>
                  <td className={cn(td, 'text-right')}>
                    {view === 'completed' ? (
                      <Badge variant="success" icon="circle-check">
                        {t('admin-finance.withdrawals.state.completed')}
                      </Badge>
                    ) : view === 'rejected' ? (
                      <Badge variant="danger" icon="ban">
                        {t('admin-finance.withdrawals.state.rejected')}
                      </Badge>
                    ) : view === 'awaiting_transfer' ? (
                      <Button
                        size="sm"
                        variant="outline"
                        iconLeft="clock"
                        onClick={() => setPaying(w)}
                      >
                        {t('admin-finance.withdrawals.state.awaiting')}
                      </Button>
                    ) : view === 'blocked' ? (
                      <Badge variant="danger" icon="lock">
                        {t('admin-finance.withdrawals.state.blocked')}
                      </Badge>
                    ) : (
                      <div className="flex gap-1.5 justify-end">
                        <Button variant="ghost" size="sm" onClick={() => setRejecting(w)}>
                          {t('admin-finance.withdrawals.reject')}
                        </Button>
                        <Button size="sm" iconLeft="check" onClick={() => setAsking(w)}>
                          {t('admin-finance.withdrawals.approve')}
                        </Button>
                      </div>
                    )}
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>

      <Modal
        open={!!asking}
        onClose={() => setAsking(null)}
        title={asking ? t('admin-finance.withdrawals.approve_title', { code: asking.code }) : ''}
        footer={
          <>
            <Button variant="ghost" onClick={() => setAsking(null)}>
              {t('common.action.cancel')}
            </Button>
            <Button
              iconLeft="check"
              loading={approve.isPending}
              onClick={() =>
                asking &&
                approve.mutate(asking.id, {
                  onSuccess: () => {
                    setPaying(asking);
                    setAsking(null);
                  },
                  onError: (e) => toast.error(errorMessage(e, t)),
                })
              }
            >
              {t('admin-finance.withdrawals.approve_submit')}
            </Button>
          </>
        }
      >
        {asking && (
          <p className="m-0 text-base text-body leading-normal">
            {t('admin-finance.withdrawals.approve_desc', {
              amount: formatVND(asking.amount, { locale }),
              shop: asking.shop.name,
              account: asking.bankAccountMasked,
            })}
          </p>
        )}
      </Modal>
      <RejectModal w={rejecting} onClose={() => setRejecting(null)} />
      <TransferModal w={paying} onClose={() => setPaying(null)} />
    </div>
  );
}
