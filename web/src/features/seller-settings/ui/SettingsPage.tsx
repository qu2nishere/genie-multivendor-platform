// Cài đặt Shop (prototype VendorPages.jsx Settings) — plan G11.43: Thông tin gian hàng (ảnh bìa ≤ 2 MB, logo
// ≤ 1 MB, tên — đổi 1 lần / 30 ngày, mô tả), Vận chuyển (5 hãng, ít nhất 1; freeship từ X₫), Địa chỉ lấy
// hàng (đồng bộ GHN), Tài khoản nhận tiền (xác minh tên khớp CCCD → OTP; xóa có OTP), Tuỳ chọn vận hành (tự
// trả lời chat + "Lưu tin nhắn", Pre-order, Chế độ nghỉ ≤ 30 ngày — còn đơn chưa bàn giao thì liệt kê), Bảo
// mật. "Lưu cài đặt" gửi khối thông tin + vận chuyển + Pre-order (PATCH /seller/settings).
import { useEffect, useMemo, useState } from 'react';
import {
  RegionPicker,
  StepUpOtp,
  useRequestStepUp,
  type StepUpChallenge,
} from '@/features/account';
import { errorMessage, isApiError } from '@/shared/api/errors';
import { UploadError, uploadFile } from '@/shared/api/upload';
import { useT } from '@/shared/hooks/useT';
import { formatDate } from '@/shared/lib/format';
import {
  Badge,
  Button,
  Icon,
  Input,
  Modal,
  Select,
  Skeleton,
  Switch,
  Textarea,
  toast,
} from '@/shared/ui';
import {
  useBanks,
  useDeletePayout,
  useEndVacation,
  usePatchSettings,
  useSavePayout,
  useSavePickup,
  useShopSettings,
  useStartVacation,
  useVerifyPayout,
} from '../application/useSettings';
import { autoReplyValid, toggleCarrier, vacationRange } from '../domain/settings.rules';
import {
  CARRIERS,
  type CarrierCode,
  type PickupAddress,
  type ShopSettings,
} from '../domain/settings.types';
import { SecuritySection, Section, ToggleRow } from './SettingsParts';

const NOW = () => Date.parse('2026-06-08T18:00:00+07:00');
const labCls = 'block font-semibold text-sm text-strong';

function PayoutModal({ onClose }: { onClose: () => void }) {
  const { t } = useT();
  const banks = useBanks();
  const verify = useVerifyPayout();
  const stepUp = useRequestStepUp();
  const save = useSavePayout();
  const [bank, setBank] = useState('VCB');
  const [holder, setHolder] = useState('');
  const [number, setNumber] = useState('');
  const [err, setErr] = useState<string | null>(null);
  const [challenge, setChallenge] = useState<StepUpChallenge | null>(null);
  const [otp, setOtp] = useState('');
  const input = { bankCode: bank, holder, accountNumber: number };

  const next = () => {
    if (!holder.trim() || number.length < 6) return setErr(t('seller-settings.payout.err_fill'));
    setErr(null);
    verify.mutate(input, {
      onSuccess: () =>
        stepUp.mutate('PAYOUT_ACCOUNT_UPDATE', {
          onSuccess: (c) => {
            setOtp('');
            setChallenge(c);
          },
          onError: (e) => setErr(errorMessage(e, t)),
        }),
      onError: (e) =>
        setErr(
          isApiError(e) && e.code === 'GNE-WAL-4222'
            ? t('seller-settings.payout.err_name', { name: String(e.details.kyc_name ?? '') })
            : errorMessage(e, t),
        ),
    });
  };
  const confirm = () =>
    challenge &&
    save.mutate(
      { input, proof: { challengeId: challenge.id, code: otp } },
      {
        onSuccess: () => {
          toast.success(t('seller-settings.payout.saved'));
          onClose();
        },
        onError: (e) => setErr(errorMessage(e, t)),
      },
    );

  return (
    <Modal
      open
      onClose={onClose}
      title={t('seller-settings.payout.title')}
      footer={
        <>
          <Button variant="ghost" onClick={onClose}>
            {t('common.action.cancel')}
          </Button>
          {challenge ? (
            <Button
              iconLeft="check"
              loading={save.isPending}
              disabled={otp.length < 6}
              onClick={confirm}
            >
              {t('seller-settings.payout.confirm')}
            </Button>
          ) : (
            <Button iconLeft="check" loading={verify.isPending || stepUp.isPending} onClick={next}>
              {t('seller-settings.payout.verify')}
            </Button>
          )}
        </>
      }
    >
      <div className="flex flex-col gap-3.5">
        <Select
          label={t('seller-settings.payout.bank')}
          disabled={!!challenge}
          value={bank}
          onChange={setBank}
          options={(banks.data ?? []).map((b) => ({ value: b.code, label: b.name }))}
        />
        <Input
          label={t('seller-settings.payout.holder')}
          hint={t('seller-settings.payout.holder_hint')}
          disabled={!!challenge}
          value={holder}
          onChange={(e) => setHolder(e.target.value)}
        />
        <Input
          label={t('seller-settings.payout.number')}
          inputMode="numeric"
          disabled={!!challenge}
          value={number}
          onChange={(e) => setNumber(e.target.value.replace(/\D/g, '').slice(0, 19))}
        />
        {err && (
          <p className="m-0 flex items-center gap-1.5 text-xs text-red-600" role="alert">
            <Icon name="circle-alert" size={13} />
            {err}
          </p>
        )}
        {challenge && (
          <div className="border-0 border-t border-solid border-line-subtle pt-3">
            <p className="m-0 mb-2.5 flex items-center gap-1.5 text-mint-600 text-sm">
              <Icon name="badge-check" size={16} />
              {t('seller-settings.payout.matched')}
            </p>
            <StepUpOtp
              challenge={challenge}
              value={otp}
              onChange={setOtp}
              onResent={setChallenge}
            />
          </div>
        )}
      </div>
    </Modal>
  );
}

function DeletePayoutModal({ onClose }: { onClose: () => void }) {
  const { t } = useT();
  const stepUp = useRequestStepUp();
  const del = useDeletePayout();
  const [challenge, setChallenge] = useState<StepUpChallenge | null>(null);
  const [otp, setOtp] = useState('');
  const [err, setErr] = useState<string | null>(null);
  useEffect(() => {
    stepUp.mutate('PAYOUT_ACCOUNT_DELETE', {
      onSuccess: setChallenge,
      onError: (e) => setErr(errorMessage(e, t)),
    });
    // gửi mã một lần khi mở
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);
  return (
    <Modal
      open
      onClose={onClose}
      title={t('seller-settings.payout.delete_title')}
      footer={
        <>
          <Button variant="ghost" onClick={onClose}>
            {t('common.action.cancel')}
          </Button>
          <Button
            variant="danger"
            loading={del.isPending}
            disabled={!challenge || otp.length < 6}
            onClick={() =>
              challenge &&
              del.mutate(
                { challengeId: challenge.id, code: otp },
                {
                  onSuccess: () => {
                    toast.success(t('seller-settings.payout.deleted'));
                    onClose();
                  },
                  onError: (e) => setErr(errorMessage(e, t)),
                },
              )
            }
          >
            {t('seller-settings.payout.delete')}
          </Button>
        </>
      }
    >
      <p className="m-0 mb-3 text-base text-body">{t('seller-settings.payout.delete_body')}</p>
      {challenge ? (
        <StepUpOtp
          challenge={challenge}
          value={otp}
          onChange={setOtp}
          onResent={setChallenge}
          error={err}
        />
      ) : (
        <Skeleton lines={2} />
      )}
    </Modal>
  );
}

function VacationModal({ s, onClose }: { s: ShopSettings; onClose: () => void }) {
  const { t } = useT();
  const start = useStartVacation();
  const range = vacationRange(NOW());
  const [until, setUntil] = useState('');
  const [msg, setMsg] = useState(t('seller-settings.vacation.default_msg'));
  const [pending, setPending] = useState<string[]>([]);
  const valid = !!until && until >= range.min && until <= range.max;
  return (
    <Modal
      open
      onClose={onClose}
      width={480}
      title={t('seller-settings.vacation.title')}
      footer={
        pending.length ? (
          <Button variant="ghost" onClick={onClose}>
            {t('common.action.close')}
          </Button>
        ) : (
          <>
            <Button variant="ghost" onClick={onClose}>
              {t('common.action.cancel')}
            </Button>
            <Button
              disabled={!valid}
              loading={start.isPending}
              onClick={() =>
                start.mutate(
                  { endsAt: until, message: msg },
                  {
                    onSuccess: () => {
                      toast.success(t('seller-settings.vacation.on'));
                      onClose();
                    },
                    onError: (e) => {
                      if (isApiError(e) && e.code === 'GNE-VND-4092')
                        setPending(
                          String(e.details.pending_order_codes ?? '')
                            .split(', ')
                            .filter(Boolean),
                        );
                      else toast.error(errorMessage(e, t));
                    },
                  },
                )
              }
            >
              {t('seller-settings.vacation.turn_on')}
            </Button>
          </>
        )
      }
    >
      {pending.length ? (
        <div className="text-base text-body leading-normal">
          {t('seller-settings.vacation.pending')}
          <ul className="mt-2 mb-0 pl-5">
            {pending.map((o) => (
              <li key={o} className="code">
                <a href={`/seller/orders?q=${encodeURIComponent(o)}`} className="text-brand-600">
                  {o}
                </a>
              </li>
            ))}
          </ul>
        </div>
      ) : (
        <div className="flex flex-col gap-3">
          <div>
            <label htmlFor="vac-until" className={labCls}>
              {t('seller-settings.vacation.until')}
            </label>
            <input
              id="vac-until"
              type="date"
              min={range.min}
              max={range.max}
              value={until}
              onChange={(e) => setUntil(e.target.value)}
              className="mt-1.5 w-full h-11 border-[1.5px] border-solid border-line rounded-md px-3 text-base text-strong bg-surface-card outline-none focus:border-primary"
            />
          </div>
          <Input
            label={t('seller-settings.vacation.msg')}
            value={msg}
            maxLength={200}
            onChange={(e) => setMsg(e.target.value)}
          />
          {s.autoReply.enabled && (
            <p className="m-0 text-xs text-muted">{t('seller-settings.vacation.hint')}</p>
          )}
        </div>
      )}
    </Modal>
  );
}

function Form({ s }: { s: ShopSettings }) {
  const { t, locale } = useT();
  const patch = usePatchSettings();
  const pickupSave = useSavePickup();
  const endVac = useEndVacation();
  const initial = useMemo(
    () => ({
      shopName: s.profile.shopName,
      description: s.profile.description,
      carriers: s.shipping.carriers,
      freeShippingEnabled: s.shipping.freeShippingEnabled,
      freeShippingThreshold: s.shipping.freeShippingThreshold,
      preOrderEnabled: s.preOrderEnabled,
    }),
    [s],
  );
  const [f, setF] = useState(initial);
  const [cover, setCover] = useState<{ url: string | null; key?: string | null }>({
    url: s.profile.coverUrl,
  });
  const [logo, setLogo] = useState<{ url: string | null; key?: string | null }>({
    url: s.profile.logoUrl,
  });
  const [pickup, setPickup] = useState<PickupAddress>(s.pickup);
  const [auto, setAuto] = useState(s.autoReply);
  const [modal, setModal] = useState<null | 'payout' | 'delPayout' | 'vacation'>(null);
  const [busy, setBusy] = useState<'cover' | 'logo' | null>(null);
  useEffect(() => setF(initial), [initial]);

  const pick = async (kind: 'cover' | 'logo', file: File | undefined) => {
    if (!file) return;
    setBusy(kind);
    try {
      const u = await uploadFile(file, kind === 'cover' ? 'SHOP_COVER' : 'SHOP_LOGO');
      (kind === 'cover' ? setCover : setLogo)({ url: u.previewUrl, key: u.objectKey });
    } catch (e) {
      toast.error(
        e instanceof UploadError ? t(`seller-settings.profile.err_${kind}`) : errorMessage(e, t),
      );
    } finally {
      setBusy(null);
    }
  };

  const saveAll = () =>
    patch.mutate(
      {
        profile: {
          shopName: f.shopName,
          description: f.description,
          ...(logo.key !== undefined ? { logoObjectKey: logo.key } : {}),
          ...(cover.key !== undefined ? { coverObjectKey: cover.key } : {}),
        },
        shipping: {
          carriers: f.carriers,
          freeShippingEnabled: f.freeShippingEnabled,
          freeShippingThreshold: f.freeShippingThreshold,
        },
        preOrderEnabled: f.preOrderEnabled,
      },
      {
        onSuccess: () => toast.success(t('seller-settings.saved')),
        onError: (e) => toast.error(errorMessage(e, t)),
      },
    );

  const carrierToggle = (c: CarrierCode) => {
    const next = toggleCarrier(f.carriers, c);
    if (!next) return toast.error(t('errors.GNE-VND-4221'));
    setF({ ...f, carriers: next });
  };

  const initial0 = s.profile.shopName.charAt(0).toUpperCase();

  return (
    <div className="max-w-[1180px]">
      <h2 className="m-0 flex items-center gap-[9px] text-2xl font-bold text-strong">
        <Icon name="settings" size={22} className="text-brand-500" />
        {t('seller-settings.title')}
      </h2>
      <p className="m-0 mt-[5px] ml-[31px] text-sm text-muted">{t('seller-settings.sub')}</p>
      <div className="flex flex-col gap-4 mt-4">
        <Section id="profile" icon="store" title={t('seller-settings.profile.title')}>
          <div className="mb-[18px]">
            <span className={labCls}>{t('seller-settings.profile.cover')}</span>
            <div className="relative mt-2 h-[150px] rounded-lg overflow-hidden bg-[linear-gradient(120deg,#1E232E,#3a4250)] border border-solid border-line-subtle">
              {cover.url && (
                <img
                  src={cover.url}
                  alt=""
                  className="absolute inset-0 w-full h-full object-cover opacity-85"
                />
              )}
              <div className="absolute inset-0 bg-[linear-gradient(90deg,rgba(20,23,31,.55),rgba(20,23,31,.1))]" />
              <div className="absolute left-4 bottom-3.5 flex items-center gap-3">
                <div className="w-14 h-14 rounded-md bg-ink-800 text-white grid place-items-center font-extrabold text-[26px] border-[3px] border-solid border-white shadow-md overflow-hidden">
                  {logo.url ? (
                    <img src={logo.url} alt="" className="w-full h-full object-cover" />
                  ) : (
                    initial0
                  )}
                </div>
                <span className="text-white font-bold text-base [text-shadow:0_1px_3px_rgba(0,0,0,.4)]">
                  {f.shopName}
                </span>
              </div>
              <label className="absolute right-3 top-3 inline-flex items-center gap-1.5 h-8 px-3 rounded-md bg-surface-card text-body font-semibold text-sm cursor-pointer focus-within:shadow-focus">
                <input
                  type="file"
                  className="sr-only"
                  accept="image/jpeg,image/png"
                  onChange={(e) => {
                    void pick('cover', e.target.files?.[0]);
                    e.target.value = '';
                  }}
                />
                <Icon name="upload" size={15} />
                {busy === 'cover'
                  ? t('common.state.loading')
                  : t('seller-settings.profile.cover_upload')}
              </label>
            </div>
            <p className="m-0 mt-1.5 text-xs text-subtle">
              {t('seller-settings.profile.cover_hint')}
            </p>
          </div>
          <div className="flex gap-[18px] max-sm:flex-col">
            <div className="flex flex-col items-center gap-2.5">
              <div className="w-[84px] h-[84px] rounded-lg bg-ink-800 text-white grid place-items-center font-extrabold text-4xl overflow-hidden">
                {logo.url ? (
                  <img
                    src={logo.url}
                    alt={t('seller-settings.profile.logo_alt')}
                    className="w-full h-full object-cover"
                  />
                ) : (
                  initial0
                )}
              </div>
              <label className="inline-flex items-center gap-1.5 h-8 px-3 rounded-md border-[1.5px] border-solid border-line text-body font-semibold text-sm cursor-pointer focus-within:shadow-focus">
                <input
                  type="file"
                  className="sr-only"
                  accept="image/jpeg,image/png"
                  onChange={(e) => {
                    void pick('logo', e.target.files?.[0]);
                    e.target.value = '';
                  }}
                />
                <Icon name="upload" size={15} />
                {busy === 'logo' ? t('common.state.loading') : t('seller-settings.profile.logo')}
              </label>
            </div>
            <div className="flex-1 flex flex-col gap-3.5">
              <Input
                label={t('seller-settings.profile.name')}
                value={f.shopName}
                maxLength={60}
                onChange={(e) => setF({ ...f, shopName: e.target.value })}
                hint={
                  s.profile.nextRenameAllowedAt
                    ? t('seller-settings.profile.rename_at', {
                        date: formatDate(s.profile.nextRenameAllowedAt, { locale }),
                      })
                    : undefined
                }
              />
              <Textarea
                label={t('seller-settings.profile.desc')}
                rows={2}
                maxLength={500}
                value={f.description}
                onChange={(e) => setF({ ...f, description: e.target.value })}
              />
            </div>
          </div>
        </Section>

        <Section id="shipping" icon="truck" title={t('seller-settings.shipping.title')}>
          <div className="flex flex-col gap-1">
            {CARRIERS.map((c) => (
              <ToggleRow
                key={c}
                label={t(`seller-settings.shipping.carriers.${c}.name`)}
                desc={t(`seller-settings.shipping.carriers.${c}.desc`)}
                on={f.carriers[c]}
                onToggle={() => carrierToggle(c)}
              />
            ))}
            <ToggleRow
              label={t('seller-settings.shipping.free')}
              desc={t('seller-settings.shipping.free_desc')}
              on={f.freeShippingEnabled}
              onToggle={() => setF({ ...f, freeShippingEnabled: !f.freeShippingEnabled })}
            />
            {f.freeShippingEnabled && (
              <div className="w-60 py-2.5">
                <Input
                  label={t('seller-settings.shipping.free_from')}
                  inputMode="numeric"
                  value={f.freeShippingThreshold ?? ''}
                  onChange={(e) => {
                    const d = e.target.value.replace(/\D/g, '');
                    setF({ ...f, freeShippingThreshold: d ? Number(d) : null });
                  }}
                />
              </div>
            )}
          </div>
        </Section>

        <Section id="pickup" icon="map-pin" title={t('seller-settings.pickup.title')}>
          <div className="grid grid-cols-2 gap-3.5 max-md:grid-cols-1">
            <Input
              label={t('seller-settings.pickup.phone')}
              inputMode="tel"
              value={pickup.phone}
              onChange={(e) => setPickup({ ...pickup, phone: e.target.value })}
            />
            <div>
              <span className={labCls}>{t('seller-settings.pickup.region')}</span>
              <div className="mt-1.5">
                <RegionPicker
                  value={{
                    province: pickup.province,
                    district: pickup.district,
                    ward: pickup.ward,
                  }}
                  onChange={(r) => setPickup({ ...pickup, ...r })}
                />
              </div>
            </div>
          </div>
          <div className="mt-3.5">
            <Input
              label={t('seller-settings.pickup.line')}
              value={pickup.line}
              onChange={(e) => setPickup({ ...pickup, line: e.target.value })}
            />
          </div>
          <div className="flex items-center gap-3 mt-3.5 flex-wrap">
            <Button
              size="sm"
              iconLeft="check"
              loading={pickupSave.isPending}
              disabled={
                !pickup.ward ||
                !pickup.line.trim() ||
                !/^0\d{9,10}$/.test(pickup.phone.replace(/\s/g, ''))
              }
              onClick={() =>
                pickupSave.mutate(pickup, {
                  onSuccess: (a) => {
                    setPickup(a);
                    toast.success(t('seller-settings.pickup.saved'));
                  },
                  onError: (e) => toast.error(errorMessage(e, t)),
                })
              }
            >
              {t('seller-settings.pickup.save')}
            </Button>
            <span className="text-xs text-muted">
              {t('seller-settings.pickup.ghn')}{' '}
              <b className="code">{pickup.ghnShopId ? `GHN-${pickup.ghnShopId}` : '—'}</b>
            </span>
          </div>
        </Section>

        <Section id="payout" icon="banknote" title={t('seller-settings.payout.section')}>
          {s.payoutAccount ? (
            <div className="flex items-center gap-3 border border-solid border-line-subtle rounded-md px-3.5 py-3 flex-wrap">
              <div className="w-11 h-11 rounded-md bg-blue-50 text-blue-600 grid place-items-center flex-none">
                <Icon name="credit-card" size={22} />
              </div>
              <div className="flex-1 min-w-[180px]">
                <div className="font-semibold text-sm text-strong">
                  {s.payoutAccount.bank} ·{' '}
                  {t('seller-settings.payout.holder_short', { name: s.payoutAccount.holder })}
                </div>
                <div className="code text-[13px] text-muted">{s.payoutAccount.numberMasked}</div>
              </div>
              {s.payoutAccount.verified && (
                <Badge variant="mint" icon="badge-check">
                  {t('seller-wallet.withdraw.verified')}
                </Badge>
              )}
              <Button size="sm" variant="ghost" onClick={() => setModal('payout')}>
                {t('seller-settings.payout.change')}
              </Button>
              <Button
                size="sm"
                variant="ghost"
                className="!text-red-600"
                onClick={() => setModal('delPayout')}
              >
                {t('seller-settings.payout.delete')}
              </Button>
            </div>
          ) : (
            <Button variant="outline" iconLeft="plus" onClick={() => setModal('payout')}>
              {t('seller-settings.payout.add')}
            </Button>
          )}
        </Section>

        <Section id="prefs" icon="sliders-horizontal" title={t('seller-settings.prefs.title')}>
          <div className="flex flex-col gap-1">
            <ToggleRow
              label={t('seller-settings.prefs.auto')}
              desc={t('seller-settings.prefs.auto_desc')}
              on={auto.enabled}
              onToggle={() => setAuto({ ...auto, enabled: !auto.enabled })}
            />
            {auto.enabled && (
              <div className="flex flex-col gap-3 py-3 border-0 border-b border-solid border-line-subtle">
                <Input
                  label={t('seller-settings.prefs.greeting')}
                  value={auto.greeting}
                  maxLength={300}
                  placeholder={t('seller-settings.prefs.greeting_placeholder')}
                  onChange={(e) => setAuto({ ...auto, greeting: e.target.value })}
                />
                <Input
                  label={t('seller-settings.prefs.away')}
                  value={auto.away}
                  maxLength={300}
                  placeholder={t('seller-settings.prefs.away_placeholder')}
                  onChange={(e) => setAuto({ ...auto, away: e.target.value })}
                />
              </div>
            )}
            <div className="py-2.5 border-0 border-b border-solid border-line-subtle">
              <Button
                size="sm"
                variant="secondary"
                iconLeft="check"
                loading={patch.isPending}
                onClick={() => {
                  if (!autoReplyValid(auto)) return toast.error(t('errors.GNE-VND-4222'));
                  patch.mutate(
                    { autoReply: auto },
                    {
                      onSuccess: () => toast.success(t('seller-settings.prefs.auto_saved')),
                      onError: (e) => toast.error(errorMessage(e, t)),
                    },
                  );
                }}
              >
                {t('seller-settings.prefs.save_msg')}
              </Button>
            </div>
            <ToggleRow
              label={t('seller-settings.prefs.pre_order')}
              desc={t('seller-settings.prefs.pre_order_desc')}
              on={f.preOrderEnabled}
              onToggle={() => setF({ ...f, preOrderEnabled: !f.preOrderEnabled })}
            />
            <div className="flex items-center gap-3 py-[11px] border-0 border-b border-solid border-line-subtle">
              <div className="flex-1 min-w-0">
                <div className="font-semibold text-sm text-red-600">
                  {t('seller-settings.vacation.label')}
                </div>
                <div className="text-xs text-muted">
                  {s.vacation
                    ? t('seller-settings.vacation.active', {
                        date: formatDate(s.vacation.endsAt, { locale }),
                      })
                    : t('seller-settings.vacation.desc')}
                </div>
              </div>
              <Switch
                checked={!!s.vacation}
                aria-label={t('seller-settings.vacation.label')}
                onChange={() =>
                  s.vacation
                    ? endVac.mutate(undefined, {
                        onSuccess: () => toast.success(t('seller-settings.vacation.off')),
                        onError: (e) => toast.error(errorMessage(e, t)),
                      })
                    : setModal('vacation')
                }
              />
            </div>
          </div>
        </Section>

        <SecuritySection />

        <div className="flex justify-end gap-2.5">
          <Button
            variant="ghost"
            onClick={() => {
              setF(initial);
              setCover({ url: s.profile.coverUrl });
              setLogo({ url: s.profile.logoUrl });
              toast.info(t('seller-settings.reset'));
            }}
          >
            {t('common.action.cancel')}
          </Button>
          <Button iconLeft="check" loading={patch.isPending} onClick={saveAll}>
            {t('seller-settings.save')}
          </Button>
        </div>
      </div>
      {modal === 'payout' && <PayoutModal onClose={() => setModal(null)} />}
      {modal === 'delPayout' && <DeletePayoutModal onClose={() => setModal(null)} />}
      {modal === 'vacation' && <VacationModal s={s} onClose={() => setModal(null)} />}
    </div>
  );
}

export default function SettingsPage() {
  const q = useShopSettings();
  if (!q.data) return <Skeleton lines={12} />;
  return <Form s={q.data} />;
}
