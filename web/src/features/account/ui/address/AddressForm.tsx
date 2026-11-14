// Form thêm / sửa địa chỉ (prototype AddressModal.jsx AddressForm) — plan G10.48: react-hook-form + zod
// (R8), nút "Dùng vị trí hiện tại (GPS)" tự điền Tỉnh / Quận / Phường, bộ chọn hành chính 3 cấp,
// loại địa chỉ, đặt mặc định.
import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { errorMessage } from '@/shared/api/errors';
import { useT } from '@/shared/hooks/useT';
import { cn } from '@/shared/lib/cn';
import { zodResolver } from '@/shared/lib/zodResolver';
import { Button, Checkbox, Field, Icon, Input, toast } from '@/shared/ui';
import { useReverseGeocode, useSaveAddress } from '../../application/useAddresses';
import { addressSchema, type AddressFormValues } from '../../domain/address.schema';
import type { Address, Region, Ward } from '../../domain/address.types';
import { RegionPicker, type RegionValue } from './RegionPicker';

export interface AddressFormProps {
  initial: Address | null;
  onCancel: () => void;
  onSaved: (a: Address) => void;
}

export function AddressForm({ initial, onCancel, onSaved }: AddressFormProps) {
  const { t } = useT();
  const save = useSaveAddress();
  const geo = useReverseGeocode();
  const [gps, setGps] = useState<'idle' | 'locating' | 'done'>('idle');
  const [names, setNames] = useState<RegionValue>({
    province: initial?.province ?? null,
    district: initial?.district ?? null,
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
      districtId: initial?.district.id ?? 0,
      wardCode: initial?.ward.code ?? '',
      line: initial?.line ?? '',
      label: initial?.label ?? 'HOME',
      isDefault: initial?.isDefault ?? false,
    },
  });
  const label = watch('label');
  const isDefault = watch('isDefault');
  const err = (m?: string) => (m ? t(m) : undefined);

  const setRegion = (v: {
    province: Region | null;
    district: Region | null;
    ward: Ward | null;
  }) => {
    setNames(v);
    setValue('provinceId', v.province?.id ?? 0, { shouldValidate: !!errors.provinceId });
    setValue('districtId', v.district?.id ?? 0, { shouldValidate: !!errors.districtId });
    setValue('wardCode', v.ward?.code ?? '', { shouldValidate: !!errors.wardCode });
  };

  const locate = () => {
    setGps('locating');
    const fill = (lat: number, lng: number) =>
      geo.mutate(
        { lat, lng },
        {
          onSuccess: (g) => {
            setRegion({ province: g.province, district: g.district, ward: g.ward });
            if (!watch('line')) setValue('line', g.line);
            setGps('done');
          },
          onError: (e) => {
            setGps('idle');
            toast.error(errorMessage(e, t));
          },
        },
      );
    // Trình duyệt từ chối / không có định vị → dùng tọa độ trung tâm TP. HCM để vẫn điền được
    if (!navigator.geolocation) return fill(10.7769, 106.7009);
    navigator.geolocation.getCurrentPosition(
      (p) => fill(p.coords.latitude, p.coords.longitude),
      () => fill(10.7769, 106.7009),
      { timeout: 4000 },
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
        onError: (e) => toast.error(errorMessage(e, t)),
      },
    ),
  );
  const regionError = err(
    errors.provinceId?.message ?? errors.districtId?.message ?? errors.wardCode?.message,
  );

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
            {t(gps === 'done' ? 'account.address.gps.done_hint' : 'account.address.gps.hint')}
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

      <fieldset className="border-0 p-0 m-0">
        <legend className="p-0 font-semibold text-sm text-strong">
          {t('account.address.label_type')}
        </legend>
        <div className="flex gap-2 mt-2">
          {(['HOME', 'OFFICE'] as const).map((l) => (
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
