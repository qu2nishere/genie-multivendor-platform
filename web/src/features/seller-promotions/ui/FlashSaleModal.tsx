// "Đăng ký Flash Sale" (prototype VendorPages.jsx Promo — flashOpen) — plan G11.40: chọn sản phẩm đủ điều kiện,
// giá Flash ≤ 85% giá 30 ngày, số suất 1..tồn; danh sách đã đăng ký chờ sàn duyệt; đăng ký lại = ghi đè.
import { useState } from 'react';
import { errorMessage } from '@/shared/api/errors';
import { useT } from '@/shared/hooks/useT';
import { formatVND } from '@/shared/lib/format';
import { Badge, Button, Input, Modal, Select, Skeleton, toast } from '@/shared/ui';
import { useEligible, useFlashItems, useRegisterFlash } from '../application/usePromotions';
import { flashIssues } from '../domain/promo.rules';
import type { FlashCampaign } from '../domain/promo.types';

const int = (s: string): number | null => {
  const d = s.replace(/\D/g, '');
  return d ? Number(d) : null;
};

export function FlashSaleModal({
  campaign,
  onClose,
}: {
  campaign: FlashCampaign;
  onClose: () => void;
}) {
  const { t, locale } = useT();
  const eligible = useEligible(campaign.id);
  const items = useFlashItems(campaign.id);
  const reg = useRegisterFlash();
  const [pid, setPid] = useState<number | null>(null);
  const [price, setPrice] = useState<number | null>(null);
  const [stock, setStock] = useState<number | null>(null);
  const [shown, setShown] = useState(false);
  const list = eligible.data ?? [];
  const p = list.find((x) => x.productId === pid) ?? list[0];
  const vnd = (n: number) => formatVND(n, { locale });
  const issues = p ? flashIssues(p, price, stock) : [];

  const add = () => {
    setShown(true);
    if (!p || issues.length) return;
    reg.mutate(
      {
        campaignId: campaign.id,
        productId: p.productId,
        flashPrice: price ?? 0,
        flashStock: stock ?? 0,
      },
      {
        onSuccess: () => {
          toast.success(t('seller-promotions.flash.added', { name: p.name }));
          setPrice(null);
          setStock(null);
          setShown(false);
        },
        onError: (e) => toast.error(errorMessage(e, t)),
      },
    );
  };

  return (
    <Modal
      open
      onClose={onClose}
      width={520}
      title={t('seller-promotions.flash.title', { name: campaign.name })}
      footer={
        <>
          <Button variant="ghost" onClick={onClose}>
            {t('common.action.close')}
          </Button>
          <Button iconLeft="plus" loading={reg.isPending} disabled={!p} onClick={add}>
            {t('seller-promotions.flash.add')}
          </Button>
        </>
      }
    >
      {!eligible.data ? (
        <Skeleton lines={4} />
      ) : (
        <div className="flex flex-col gap-3">
          <Select
            label={t('seller-promotions.flash.product')}
            value={p?.productId ?? 0}
            onChange={(id) => {
              setPid(id);
              setShown(false);
            }}
            options={list.map((x) => ({
              value: x.productId,
              label: `${x.name} (${vnd(x.referencePrice)})`,
              text: x.name,
            }))}
          />
          {p && (
            <div className="grid grid-cols-2 gap-3 max-sm:grid-cols-1">
              <Input
                label={t('seller-promotions.flash.price')}
                inputMode="numeric"
                value={price ?? ''}
                onChange={(e) => setPrice(int(e.target.value))}
                hint={t('seller-promotions.flash.price_hint', { max: vnd(p.maxFlashPrice) })}
                error={
                  shown && issues.includes('price')
                    ? t('seller-promotions.flash.err_price', { max: vnd(p.maxFlashPrice) })
                    : undefined
                }
              />
              <Input
                label={t('seller-promotions.flash.stock', { stock: p.stock })}
                inputMode="numeric"
                value={stock ?? ''}
                onChange={(e) => setStock(int(e.target.value))}
                error={
                  shown && issues.includes('stock')
                    ? t('seller-promotions.flash.err_stock', { max: p.stock })
                    : undefined
                }
              />
            </div>
          )}
          {(items.data ?? []).length > 0 && (
            <div className="border-0 border-t border-solid border-line-subtle pt-2.5">
              <div className="font-semibold text-sm text-strong mb-1.5">
                {t('seller-promotions.flash.registered')}
              </div>
              <ul className="m-0 p-0 list-none">
                {(items.data ?? []).map((r) => (
                  <li
                    key={r.productId}
                    className="flex justify-between gap-2 text-sm text-body py-[3px]"
                  >
                    <span className="min-w-0 truncate">{r.name}</span>
                    <span className="flex items-center gap-2 flex-none">
                      {t('seller-promotions.flash.row', {
                        price: vnd(r.flashPrice),
                        stock: r.flashStock,
                      })}
                      <Badge variant="warning">
                        {t(`seller-promotions.flash.status.${r.status}`)}
                      </Badge>
                    </span>
                  </li>
                ))}
              </ul>
            </div>
          )}
        </div>
      )}
    </Modal>
  );
}
