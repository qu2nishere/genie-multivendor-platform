// "Điểm Genie" (prototype Account.jsx CoinsPanel) — plan G10.57, /account/points: số dư + Điểm sắp hết hạn,
// đổi quà (Điểm → voucher vào Kho Voucher), điểm danh 7 ngày (đếm ngược tới nửa đêm sau khi điểm danh),
// lịch sử Điểm. Từ ngữ "Điểm" thay "Xu" (R11).
import { useEffect, useState } from 'react';
import { errorMessage } from '@/shared/api/errors';
import { secondsUntilMidnightVN } from '@/shared/hooks/useCountdown';
import { useT } from '@/shared/hooks/useT';
import { cn } from '@/shared/lib/cn';
import { formatDate, formatHhMmSs, formatNumber, formatVND } from '@/shared/lib/format';
import { Providers } from '@/shared/providers/Providers';
import { Button, Icon, Modal, PanelHead, Skeleton, toast } from '@/shared/ui';
import {
  useCheckIn,
  useCoinRewards,
  useCoins,
  useCoinTxns,
  useRedeemReward,
} from '../application/useCoins';
import { canRedeem, checkInDays, rewardLabel, rewardOf } from '../domain/coins.rules';

const card = 'bg-surface-card rounded-lg shadow-sm px-[22px] py-5 max-sm:px-4';

function UntilMidnight() {
  const [s, setS] = useState(() => secondsUntilMidnightVN());
  useEffect(() => {
    const i = window.setInterval(() => setS(secondsUntilMidnightVN()), 1000);
    return () => window.clearInterval(i);
  }, []);
  return <span className="font-mono">{formatHhMmSs(s)}</span>;
}

function Panel() {
  const { t, locale } = useT();
  const q = useCoins();
  const txns = useCoinTxns();
  const rewards = useCoinRewards();
  const check = useCheckIn();
  const redeem = useRedeemReward();
  const [open, setOpen] = useState(false);
  const n = (x: number) => formatNumber(x, { locale });
  const c = q.data;

  return (
    <section className="flex flex-col gap-[18px]" aria-label={t('account.menu.points')}>
      <div
        className={cn(
          card,
          'bg-accent-tint border border-solid border-gold-100 flex items-center gap-[18px] flex-wrap',
        )}
      >
        <span className="w-16 h-16 rounded-full bg-accent text-accent-on grid place-items-center flex-none">
          <Icon name="star" size={32} className="[&_path]:fill-current" />
        </span>
        <div className="flex-1 min-w-[200px]">
          <p className="m-0 text-sm text-muted">{t('coins.balance')}</p>
          {c ? (
            <div className="font-extrabold text-[34px] leading-tight tabular-nums text-strong">
              {n(c.balance)}{' '}
              <span className="text-lg font-semibold">{t('common.points_short')}</span>
            </div>
          ) : (
            <Skeleton className="h-10 w-40" />
          )}
          <p className="m-0 mt-0.5 text-xs text-muted">
            {t('coins.rule')}
            {c?.expiringSoon &&
              ` · ${t('coins.expiring', { amount: n(c.expiringSoon.amount), days: c.expiringSoon.withinDays })}`}
          </p>
        </div>
        <Button variant="secondary" iconLeft="gift" onClick={() => setOpen(true)}>
          {t('coins.redeem')}
        </Button>
      </div>

      <div className={card}>
        <PanelHead
          icon="calendar"
          title={t('coins.checkin.title')}
          action={
            c && (
              <span className="flex items-center gap-3 flex-wrap justify-end">
                {c.checkIn.checkedInToday && (
                  <span className="text-xs text-muted">
                    {t('coins.checkin.again')} <UntilMidnight />
                  </span>
                )}
                <Button
                  variant={c.checkIn.checkedInToday ? 'outline' : 'primary'}
                  iconLeft={c.checkIn.checkedInToday ? 'check' : 'calendar'}
                  disabled={c.checkIn.checkedInToday}
                  loading={check.isPending}
                  onClick={() =>
                    check.mutate(undefined, {
                      onSuccess: (r) =>
                        toast.success(t('coins.checkin.done', { points: n(r.reward) })),
                      onError: (e) => toast.error(errorMessage(e, t)),
                    })
                  }
                >
                  {t(c.checkIn.checkedInToday ? 'coins.checkin.checked' : 'coins.checkin.cta')}
                </Button>
              </span>
            )
          }
        />
        {c ? (
          <ol className="list-none m-0 p-0 mt-3.5 flex justify-between gap-1">
            {checkInDays(c.checkIn).map((st, i) => (
              <li key={i} className="flex flex-col items-center gap-2">
                <span
                  className={cn(
                    'w-10 h-10 rounded-full grid place-items-center font-bold text-xs border-2 border-solid',
                    st === 'done'
                      ? 'bg-gold-500 text-inverse border-transparent'
                      : st === 'today'
                        ? 'bg-gold-50 text-gold-700 border-gold-500'
                        : 'bg-gray-100 text-subtle border-transparent',
                  )}
                >
                  {st === 'done' ? (
                    <Icon name="check" size={20} />
                  ) : (
                    rewardLabel(rewardOf(c.checkIn, i + 1))
                  )}
                </span>
                <span
                  className={cn(
                    'text-xs',
                    st === 'today' ? 'text-gold-700 font-semibold' : 'text-muted',
                  )}
                >
                  {t('coins.checkin.day', { n: i + 1 })}
                </span>
              </li>
            ))}
          </ol>
        ) : (
          <Skeleton className="h-16 mt-3.5" />
        )}
      </div>

      <section className={cn(card, '!p-0 overflow-hidden')} aria-labelledby="coin-log">
        <div className="px-[22px] pt-4 pb-3">
          <PanelHead id="coin-log" icon="clock" title={t('coins.history')} />
        </div>
        {txns.isPending ? (
          <Skeleton lines={4} className="mx-[22px] mb-4" />
        ) : (
          <ul className="list-none m-0 p-0">
            {(txns.data ?? []).map((x) => (
              <li
                key={`${x.type}-${x.id}`}
                className="flex items-center gap-3 px-[22px] py-[13px] border-0 border-t border-solid border-line-subtle"
              >
                <span
                  className={cn(
                    'w-[38px] h-[38px] rounded-full flex-none grid place-items-center',
                    x.amount > 0 ? 'bg-mint-50 text-mint-600' : 'bg-surface-sunken text-muted',
                  )}
                >
                  <Icon name={x.amount > 0 ? 'plus' : 'minus'} size={18} />
                </span>
                <div className="flex-1 min-w-0">
                  <div className="text-sm text-body">{x.description}</div>
                  <div className="text-xs text-subtle">{formatDate(x.createdAt, { locale })}</div>
                </div>
                <span
                  className={cn(
                    'font-extrabold text-base tabular-nums',
                    x.amount > 0 ? 'text-mint-600' : 'text-muted',
                  )}
                >
                  {x.amount > 0 ? '+' : ''}
                  {n(x.amount)} {t('common.points_short')}
                </span>
              </li>
            ))}
          </ul>
        )}
      </section>

      <Modal
        open={open}
        onClose={() => setOpen(false)}
        width={440}
        title={t('coins.redeem_title')}
        footer={
          <Button variant="ghost" onClick={() => setOpen(false)}>
            {t('common.action.close')}
          </Button>
        }
      >
        <p className="m-0 mb-2.5 text-sm text-muted">
          {t('coins.balance_short')}{' '}
          <b className="text-strong">
            {n(c?.balance ?? 0)} {t('common.points_short')}
          </b>
        </p>
        <ul className="list-none m-0 p-0">
          {(rewards.data ?? []).map((r) => {
            const ok = canRedeem(c?.balance ?? 0, r.cost);
            return (
              <li
                key={r.id}
                className="flex items-center gap-3 py-2.5 border-0 border-t border-solid border-line-subtle"
              >
                <Icon name="ticket" size={20} className="text-primary" />
                <div className="flex-1">
                  <div className="font-semibold text-sm text-strong">
                    {t('coins.voucher', { amount: formatVND(r.voucherValue, { locale }) })}
                  </div>
                  <div className="text-xs text-muted">
                    {t('coins.cost', { points: n(r.cost), min: formatVND(r.minOrder, { locale }) })}
                  </div>
                </div>
                <Button
                  size="sm"
                  disabled={!ok}
                  loading={redeem.isPending && redeem.variables?.id === r.id}
                  onClick={() =>
                    redeem.mutate(r, {
                      onSuccess: (res) => {
                        setOpen(false);
                        toast.success(t('coins.redeemed', { code: res.code }));
                      },
                      onError: (e) => toast.error(errorMessage(e, t)),
                    })
                  }
                >
                  {ok ? t('coins.swap') : t('coins.not_enough')}
                </Button>
              </li>
            );
          })}
        </ul>
      </Modal>
    </section>
  );
}

export default function CoinsPanel() {
  return (
    <Providers>
      <Panel />
    </Providers>
  );
}
