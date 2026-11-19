// Form thêm / sửa địa chỉ (prototype AddressModal.jsx AddressForm) — plan G10.48: react-hook-form + zod
// (R8), nút "Dùng vị trí hiện tại (GPS)" tự điền Tỉnh / Phường, bộ chọn hành chính 2 cấp (plan AUTH D25),
// loại địa chỉ, đặt mặc định.
import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { errorMessage, isApiError } from '@/shared/api/errors';
import { useT } from '@/shared/hooks/useT';
import { cn } from '@/shared/lib/cn';
import { zodResolver } from '@/shared/lib/zodResolver';
import { Button, Checkbox, Field, Icon, Input, Textarea, toast } from '@/shared/ui';
import { useReverseGeocode, useSaveAddress } from '../../application/useAddresses';
import { addressSchema, type AddressFormValues } from '../../domain/address.schema';
import { ADDRESS_LABELS, type Address, type Region, type Ward } from '../../domain/address.types';
import { RegionPicker, type RegionValue } from './RegionPicker';

export interface AddressFormProps {
  initial: Address | null;
  onCancel: () => void;
  onSaved: (a: Address) => void;
  /** Địa chỉ đã có trong sổ (409 GNE-USR-4093) — nơi gọi đóng form và làm nổi địa chỉ cũ. */
  onDuplicate?: (existingId: number) => void;
}

export function AddressForm({ initial, onCancel, onSaved, onDuplicate }: AddressFormProps) {
  const { t } = useT();
  const save = useSaveAddress();
  const geo = useReverseGeocode();
  const [gps, setGps] = useState<'idle' | 'locating' | 'done'>('idle');
  /** Định vị chỉ khớp được tỉnh, chưa khớp phường → nhắc chọn tiếp. */
  const [partial, setPartial] = useState(false);
  const [names, setNames] = useState<RegionValue>({
    province: initial?.province ?? null,
    ward: initial?.ward ?? null,
  });
  const {
    register,
    handleSubmit,
    setValue,
    watch,
    formState: { errors },
  } = useForm<AddressFormValues>({
    resolver: zodResolver(addressSchema),
    defaultValues: {
      recipient: initial?.recipient ?? '',
      phone: initial?.phone ?? '',
      provinceId: initial?.province.id ?? 0,
      wardCode: initial?.ward.code ?? '',
      line: initial?.line ?? '',
      note: initial?.note ?? '',
      label: initial?.label ?? 'HOME',
      isDefault: initial?.isDefault ?? false,
    },
  });
  const label = watch('label');
  const isDefault = watch('isDefault');
  const err = (m?: string) => (m ? t(m) : undefined);

  const setRegion = (v: { province: Region | null; ward: Ward | null }) => {
    setNames(v);
    setValue('provinceId', v.province?.id ?? 0, { shouldValidate: !!errors.provinceId });
    setValue('wardCode', v.ward?.code ?? '', { shouldValidate: !!errors.wardCode });
  };

  const locate = () => {
    setGps('locating');
    const fill = (lat: number, lng: number) =>
      geo.mutate(
        { lat, lng },
        {
          onSuccess: (g) => {
            if (!g.province) {
              // không khớp được tỉnh nào (ngoài danh mục, giữa biển…) → giữ nguyên, để chọn thủ công
              if (g.line && !watch('line')) setValue('line', g.line);
              setGps('idle');
              toast.error(t('account.address.gps.not_found'));
              return;
            }
            setRegion({ province: g.province, ward: g.ward });
            if (g.line && !watch('line')) setValue('line', g.line);
            setPartial(!g.ward);
            setGps('done');
          },
          onError: (e) => {
            setGps('idle');
            toast.error(errorMessage(e, t));
          },
        },
      );
    const denied = () => {
      setGps('idle');
      toast.error(t('account.address.gps.denied'));
    };
    // Không đoán vị trí khi trình duyệt từ chối / không hỗ trợ — báo để người dùng chọn thủ công
    if (!navigator.geolocation) return denied();
    navigator.geolocation.getCurrentPosition(
      (p) => fill(p.coords.latitude, p.coords.longitude),
      denied,
      { enableHighAccuracy: true, timeout: 10000, maximumAge: 60000 },
    );
  };

  const submit = handleSubmit((v) =>
    save.mutate(
      { id: initial?.id ?? null, input: v },
      {
        onSuccess: (a) => {
          toast.success(t('account.address.saved'));
          onSaved(a);
        },
        onError: (e) => {
          toast.error(errorMessage(e, t));
          const existing =
            isApiError(e) && e.code === 'GNE-USR-4093' ? e.details.existing_address_id : null;
          if (typeof existing === 'number') onDuplicate?.(existing);
        },
      },
    ),
  );
  const regionError = err(errors.provinceId?.message ?? errors.wardCode?.message);

  return (
    <form className="flex flex-col gap-3.5" onSubmit={(e) => void submit(e)} noValidate>
      <div className="grid grid-cols-2 gap-3 max-sm:grid-cols-1">
        <Input
          label={t('account.address.recipient')}
          placeholder={t('common.placeholder.person_name')}
          autoComplete="name"
          error={err(errors.recipient?.message)}
          {...register('recipient')}
        />
        <Input
          label={t('account.address.phone')}
          placeholder="0901 234 567"
          inputMode="tel"
          autoComplete="tel"
          error={err(errors.phone?.message)}
          {...register('phone')}
        />
      </div>

      <button
        type="button"
        onClick={locate}
        disabled={gps === 'locating'}
        className={cn(
          'flex items-center gap-2.5 w-full rounded-md px-3.5 py-[11px] text-left cursor-pointer border-[1.5px] border-solid',
          'disabled:cursor-wait',
          gps === 'done' ? 'border-mint-500 bg-mint-50' : 'border-blue-300 bg-blue-50',
        )}
      >
        <span
          className={cn(
            'w-8 h-8 rounded-full bg-surface-card grid place-items-center flex-none',
            gps === 'done' ? 'text-mint-600' : 'text-blue-600',
          )}
        >
          <Icon
            name={gps === 'locating' ? 'refresh-cw' : gps === 'done' ? 'circle-check' : 'map-pin'}
            size={18}
            className={gps === 'locating' ? 'animate-spin' : undefined}
          />
        </span>
        <span className="flex-1">
          <span className="block font-semibold text-sm text-strong">
            {t(`account.address.gps.${gps}`)}
          </span>
          <span className="block text-xs text-muted">
            {t(
              gps === 'done'
                ? partial
                  ? 'account.address.gps.partial_hint'
                  : 'account.address.gps.done_hint'
                : 'account.address.gps.hint',
            )}
          </span>
        </span>
        {gps !== 'done' && <Icon name="chevron-right" size={18} className="text-subtle" />}
      </button>

      <Field label={t('account.address.region.label')} error={regionError}>
        {({ id, describedBy, invalid }) => (
          <RegionPicker
            id={id}
            describedBy={describedBy}
            invalid={invalid}
            value={names}
            onChange={setRegion}
          />
        )}
      </Field>

      <Input
        label={t('account.address.line')}
        placeholder={t('account.address.line_placeholder')}
        autoComplete="street-address"
        error={err(errors.line?.message)}
        {...register('line')}
      />

      <Textarea
        label={t('account.address.note')}
        placeholder={t('account.address.note_placeholder')}
        hint={t('account.address.note_hint')}
        rows={2}
        maxLength={200}
        error={err(errors.note?.message)}
        {...register('note')}
      />

      <fieldset className="border-0 p-0 m-0">
        <legend className="p-0 font-semibold text-sm text-strong">
          {t('account.address.label_type')}
        </legend>
        <div className="flex flex-wrap gap-2 mt-2">
          {ADDRESS_LABELS.map((l) => (
            <button
              key={l}
              type="button"
              aria-pressed={label === l}
              onClick={() => setValue('label', l)}
              className={cn(
                'h-[38px] px-4 rounded-md text-sm cursor-pointer border-[1.5px] border-solid',
                label === l
                  ? 'border-primary bg-primary-tint text-brand-700'
                  : 'border-line bg-surface-card text-body',
              )}
            >
              {t(`account.address.label.${l}`)}
            </button>
          ))}
        </div>
      </fieldset>

      <Checkbox
        checked={isDefault}
        disabled={initial?.isDefault}
        onChange={() => setValue('isDefault', !isDefault)}
      >
        <span className="text-sm text-body">{t('account.address.make_default')}</span>
      </Checkbox>

      <div className="flex justify-end gap-2.5 mt-1">
        <Button type="button" variant="ghost" iconLeft="arrow-left" onClick={onCancel}>
          {t('common.action.back')}
        </Button>
        <Button type="submit" iconLeft="check" loading={save.isPending}>
          {t('account.address.save')}
        </Button>
      </div>
    </form>
  );
}
