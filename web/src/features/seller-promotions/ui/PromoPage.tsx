// Khuyến mãi của shop (prototype VendorPages.jsx Promo) — plan G11.40: banner đăng ký Flash Sale, 3 chỉ số,
// bảng mã giảm giá (Mã / Loại / Điều kiện / Đã dùng / Trạng thái / Kết thúc; Chỉnh sửa / Nhân bản / Tắt).
// Route /seller/flash-sales mở sẵn hộp đăng ký Flash Sale.
import { useState } from 'react';
import { errorMessage } from '@/shared/api/errors';
import { useT } from '@/shared/hooks/useT';
import { cn } from '@/shared/lib/cn';
import { formatDate, formatMoneyShort, formatNumber, formatVND } from '@/shared/lib/format';
import { Button, ConfirmDialog, Icon, Skeleton, toast } from '@/shared/ui';
import { StatTile } from '@/shared/ui/dash';
import { useDisableVoucher, useFlashCampaigns, useVouchers } from '../application/usePromotions';
import { usagePercent, V_TONE, voucherActions } from '../domain/promo.rules';
import type { ShopVoucher } from '../domain/promo.types';
import { FlashSaleModal } from './FlashSaleModal';
import { VoucherFormModal, type VoucherFormTarget } from './VoucherFormModal';

const th = 'px-3.5 py-2.5 whitespace-nowrap';
const td = 'px-3.5 py-3 whitespace-nowrap';

export default function PromoPage({ openFlash = false }: { openFlash?: boolean }) {
  const { t, locale } = useT();
  const q = useVouchers();
  const camps = useFlashCampaigns();
  const off = useDisableVoucher();
  const [form, setForm] = useState<VoucherFormTarget | null>(null);
  const [confirmOff, setConfirmOff] = useState<ShopVoucher | null>(null);
  const [flash, setFlash] = useState(openFlash);
  const camp = camps.data?.[0];
  const vnd = (n: number) => formatVND(n, { locale });
  const short = (n: number) => formatMoneyShort(n, { locale });

  const typeCell = (v: ShopVoucher) => (
    <>
      {t(`seller-promotions.kind.${v.discountType}`)}{' '}
      {v.discountType !== 'FREESHIP' && (
        <b>
          {v.discountType === 'PERCENTAGE' ? `${v.discountValue}%` : short(v.discountValue ?? 0)}
        </b>
      )}
      {v.maxDiscount ? (
        <span className="text-subtle">
          {' '}
          · {t('seller-promotions.table.cap', { value: short(v.maxDiscount) })}
        </span>
      ) : v.discountType === 'FREESHIP' ? (
        <span className="text-subtle"> · {t('seller-promotions.table.ship')}</span>
      ) : null}
    </>
  );

  return (
    <div className="max-w-[1180px]">
      <div className="flex items-center gap-3 mb-4 flex-wrap">
        <div>
          <h2 className="m-0 flex items-center gap-[9px] text-2xl font-bold text-strong">
            <Icon name="megaphone" size={22} className="text-brand-500" />
            {t('seller-promotions.title')}
          </h2>
          <p className="m-0 mt-[5px] ml-[31px] text-sm text-muted">{t('seller-promotions.sub')}</p>
        </div>
        <Button iconLeft="plus" className="ml-auto" onClick={() => setForm({ id: null })}>
          {t('seller-promotions.create')}
        </Button>
      </div>

      {camp && (
        <div className="flex items-center gap-4 bg-accent-tint border border-solid border-gold-100 text-strong rounded-lg px-[22px] py-[18px] mb-[18px] flex-wrap">
          <div className="w-12 h-12 rounded-md bg-accent text-accent-on grid place-items-center flex-none">
            <Icon name="zap" size={26} />
          </div>
          <div className="flex-1 min-w-[200px]">
            <div className="font-bold text-lg">
              {t('seller-promotions.flash.banner', { name: camp.name })}
            </div>
            <div className="text-sm text-muted">
              {t('seller-promotions.flash.banner_sub', {
                date: formatDate(`${camp.registrationDeadline}T00:00:00+07:00`, { locale }),
                max: camp.maxItemsPerShop,
              })}
            </div>
          </div>
          <Button onClick={() => setFlash(true)}>
            {camp.registeredCount
              ? t('seller-promotions.flash.registered_n', { count: camp.registeredCount })
              : t('seller-promotions.flash.register')}
          </Button>
        </div>
      )}

      {!q.data ? (
        <Skeleton lines={8} />
      ) : (
        <>
          <div className="dash-tiles mb-[18px]" style={{ ['--n' as string]: 3 }}>
            <StatTile
              label={t('seller-promotions.stats.active')}
              value={formatNumber(q.data.summary.activeCount, { locale })}
            />
            <StatTile
              label={t('seller-promotions.stats.usage')}
              value={formatNumber(q.data.summary.usageThisMonth, { locale })}
            />
            <StatTile
              label={t('seller-promotions.stats.revenue')}
              value={vnd(q.data.summary.revenueThisMonth)}
            />
          </div>
          <section
            className="bg-surface-card rounded-lg shadow-sm overflow-hidden"
            aria-labelledby="vch-title"
          >
            <h2
              id="vch-title"
              className="m-0 px-5 py-[15px] border-0 border-b border-solid border-line-subtle font-bold text-base text-strong"
            >
              {t('seller-promotions.table.title')}
            </h2>
            <div className="overflow-x-auto">
              <table className="w-full min-w-[760px] border-collapse tabular-nums">
                <thead>
                  <tr className="text-xs font-semibold text-muted text-left bg-gray-25">
                    <th scope="col" className={th}>
                      {t('seller-promotions.table.code')}
                    </th>
                    <th scope="col" className={th}>
                      {t('seller-promotions.table.type')}
                    </th>
                    <th scope="col" className={th}>
                      {t('seller-promotions.table.cond')}
                    </th>
                    <th scope="col" className={th}>
                      {t('seller-promotions.table.used')}
                    </th>
                    <th scope="col" className={th}>
                      {t('seller-promotions.table.status')}
                    </th>
                    <th scope="col" className={th}>
                      {t('seller-promotions.table.ends')}
                    </th>
                    <th scope="col" className={`${th} text-right`}>
                      {t('seller-promotions.table.actions')}
                    </th>
                  </tr>
                </thead>
                <tbody>
                  {q.data.items.map((v) => {
                    const acts = voucherActions(v.status);
                    return (
                      <tr
                        key={v.id}
                        className="border-0 border-t border-solid border-line-subtle text-[13px] text-body"
                      >
                        <td className={td}>
                          <span className="code font-semibold text-brand-600">{v.code}</span>
                        </td>
                        <td className={td}>{typeCell(v)}</td>
                        <td className={`${td} text-muted`}>
                          {v.minOrderValue
                            ? t('seller-promotions.table.min', { value: short(v.minOrderValue) })
                            : t('seller-promotions.table.any')}
                        </td>
                        <td className={td}>
                          <div className="flex items-center gap-2">
                            <div
                              className="w-[70px] h-1.5 rounded-sm bg-gray-200 overflow-hidden"
                              role="progressbar"
                              aria-valuenow={Math.round(usagePercent(v))}
                              aria-valuemin={0}
                              aria-valuemax={100}
                              aria-label={t('seller-promotions.table.used')}
                            >
                              <div
                                className="h-full bg-brand-500"
                                style={{ width: `${usagePercent(v)}%` }}
                              />
                            </div>
                            <span className="text-xs text-muted">
                              {v.usage.used}/{v.usage.limit}
                            </span>
                          </div>
                        </td>
                        <td className={td}>
                          <span
                            className={cn(
                              'inline-flex items-center font-semibold text-xs rounded-pill px-2.5 py-[3px]',
                              V_TONE[v.status],
                            )}
                          >
                            {t(`seller-promotions.status.${v.status}`)}
                          </span>
                        </td>
                        <td className={`${td} text-muted`}>{formatDate(v.validTo, { locale })}</td>
                        <td className={`${td} text-right`}>
                          <div className="flex gap-1 justify-end">
                            {acts.includes('EDIT') && (
                              <Button
                                size="sm"
                                variant="ghost"
                                onClick={() => setForm({ id: v.id })}
                              >
                                {t('seller-promotions.table.edit')}
                              </Button>
                            )}
                            <Button
                              size="sm"
                              variant="ghost"
                              onClick={() => setForm({ id: v.id, duplicate: true })}
                            >
                              {t('seller-promotions.table.duplicate')}
                            </Button>
                            {acts.includes('DISABLE') && (
                              <Button
                                size="sm"
                                variant="ghost"
                                className="!text-red-600"
                                onClick={() => setConfirmOff(v)}
                              >
                                {t('seller-promotions.table.disable')}
                              </Button>
                            )}
                          </div>
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          </section>
        </>
      )}

      {form && <VoucherFormModal target={form} onClose={() => setForm(null)} />}
      {flash && camp && <FlashSaleModal campaign={camp} onClose={() => setFlash(false)} />}
      <ConfirmDialog
        open={!!confirmOff}
        tone="danger"
        loading={off.isPending}
        title={t('seller-promotions.off.title')}
        message={t('seller-promotions.off.body', { code: confirmOff?.code ?? '' })}
        confirmLabel={t('seller-promotions.off.ok')}
        onCancel={() => setConfirmOff(null)}
        onConfirm={() =>
          confirmOff &&
          off.mutate(confirmOff.id, {
            onSuccess: () => {
              toast.success(t('seller-promotions.off.done', { code: confirmOff.code }));
              setConfirmOff(null);
            },
            onError: (e) => toast.error(errorMessage(e, t)),
          })
        }
      />
    </div>
  );
}

export function FlashSalesRoute() {
  return <PromoPage openFlash />;
}
