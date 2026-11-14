// sessionStorage genie.checkout.draft — dạng snake_case đúng bảng LUU_TRU_DU_LIEU 6.2.
import { store } from '@/shared/lib/storage';
import { emptyDraft, type CheckoutDraft } from '../domain/draft.types';

interface DraftDto {
  selected_item_ids: number[];
  notes: Record<string, string>;
  voucher_codes?: string[];
  use_coins: boolean;
}

export function readDraft(): CheckoutDraft {
  const d = store.get<DraftDto>('checkoutDraft');
  if (!d || !Array.isArray(d.selected_item_ids)) return emptyDraft();
  return {
    itemIds: d.selected_item_ids.filter((x) => Number.isInteger(x)),
    notes: Object.fromEntries(
      Object.entries(d.notes ?? {}).map(([k, v]) => [Number(k), String(v)]),
    ),
    voucherCodes: Array.isArray(d.voucher_codes) ? d.voucher_codes.map(String) : null,
    usePoints: d.use_coins === true,
  };
}

export function writeDraft(d: CheckoutDraft): void {
  const dto: DraftDto = {
    selected_item_ids: d.itemIds,
    notes: Object.fromEntries(Object.entries(d.notes).filter(([, v]) => v.trim() !== '')),
    ...(d.voucherCodes ? { voucher_codes: d.voucherCodes } : {}),
    use_coins: d.usePoints,
  };
  store.set('checkoutDraft', dto);
}

export function clearDraft(): void {
  store.remove('checkoutDraft');
}
