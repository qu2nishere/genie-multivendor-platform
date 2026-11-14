// Thêm / Sửa banner (prototype Banners.jsx: form, LinkPicker, ProductPicker) — plan G12.36: tiêu đề, ảnh
// desktop (bắt buộc) + mobile (tải qua BANNER_IMAGE), liên kết đích chọn từ dữ liệu sàn (danh mục / sản
// phẩm / cửa hàng / Flash Sale / Gợi ý hôm nay / trang chiến dịch — luôn trong miền sàn), vị trí (+ danh
// mục khi "Đầu trang danh mục"), thứ tự (tự gợi ý số kế tiếp của ô), thời gian, hiển thị.
import { useQuery } from '@tanstack/react-query';
import { useEffect, useRef, useState } from 'react';
import { useForm } from 'react-hook-form';
import { listCategories } from '@/features/catalog';
import { listProducts, productHref } from '@/features/product';
import { listShops } from '@/features/shop';
import { errorMessage, isApiError } from '@/shared/api/errors';
import { uploadFile, UploadError } from '@/shared/api/upload';
import { useT } from '@/shared/hooks/useT';
import { cn } from '@/shared/lib/cn';
import { formatVND } from '@/shared/lib/format';
import { maxMb } from '@/shared/lib/upload.rules';
import { zodResolver } from '@/shared/lib/zodResolver';
import { Button, Icon, Input, Modal, Select, Spinner, Switch, toast } from '@/shared/ui';
import { useSaveBanner } from '../application/useMarketing';
import {
  BANNER_POSITIONS,
  DISCOVER_LINK,
  FLASH_LINK,
  LINK_TYPES,
  nextOrder,
  parseLink,
  sitePath,
  type LinkType,
} from '../domain/marketing.rules';
import { bannerSchema, type BannerValues } from '../domain/marketing.schema';
import type { AdminBanner, BannerPosition } from '../domain/marketing.types';

const lab = 'block text-sm font-semibold text-strong';
const box =
  'flex-1 min-w-0 h-[42px] border-[1.5px] border-solid rounded-md px-2.5 text-sm bg-surface-card text-body outline-none focus:border-primary';

function ProductPicker({ value, onChange }: { value: string; onChange: (href: string) => void }) {
  const { t, locale } = useT();
  const [q, setQ] = useState('');
  const [shopId, setShopId] = useState(0);
  const [term, setTerm] = useState('');
  useEffect(() => {
    const id = setTimeout(() => setTerm(q.trim()), 250);
    return () => clearTimeout(id);
  }, [q]);
  const shops = useQuery({ queryKey: ['shop', 'all'], queryFn: listShops, staleTime: Infinity });
  const found = useQuery({
    queryKey: ['admin', 'banner-products', term, shopId],
    queryFn: () => listProducts({ q: term || undefined, shopId: shopId || undefined, size: 8 }),
    enabled: !!term || !!shopId,
  });
  const chosenId = value ? Number(value) : null;
  const chosen = useQuery({
    queryKey: ['admin', 'banner-product', chosenId],
    queryFn: () =>
      listProducts({ size: 60 }).then((p) => p.items.find((x) => x.id === chosenId) ?? null),
    enabled: chosenId != null,
  });
  if (chosenId != null) {
    const p = chosen.data;
    return (
      <div>
        <span className={lab}>{t('admin-marketing.banner.link.product')}</span>
        <div className="flex items-center gap-2.5 mt-1.5 py-1 pl-2 pr-1 border-[1.5px] border-solid border-line rounded-md">
          {p ? (
            <>
              <img
                src={p.image}
                alt=""
                width={32}
                height={32}
                className="w-8 h-8 object-cover rounded-sm flex-none"
              />
              <span className="flex-1 min-w-0">
                <span className="block text-sm text-strong truncate">{p.title}</span>
                <span className="block text-xs text-muted">
                  {p.shopName} · {formatVND(p.price, { locale })}
                </span>
              </span>
            </>
          ) : (
            <span className="flex-1 text-sm text-muted">#{chosenId}</span>
          )}
          <Button size="sm" variant="ghost" onClick={() => onChange('')}>
            {t('admin-marketing.banner.link.change')}
          </Button>
        </div>
      </div>
    );
  }
  return (
    <div>
      <div className="grid grid-cols-[minmax(0,1fr)_150px] gap-2 max-sm:grid-cols-1">
        <Input
          label={t('admin-marketing.banner.link.product')}
          value={q}
          onChange={(e) => setQ(e.target.value)}
          placeholder={t('admin-marketing.banner.link.product_placeholder')}
        />
        <Select<number>
          label={t('admin-marketing.banner.link.shop')}
          value={shopId}
          onChange={setShopId}
          options={[
            { value: 0, label: t('admin-marketing.banner.link.all_shops') },
            ...(shops.data ?? []).map((s) => ({ value: s.id, label: s.name })),
          ]}
        />
      </div>
      {(term || shopId > 0) && (
        <div
          className="mt-1.5 border border-solid border-line rounded-md overflow-hidden"
          role="listbox"
        >
          {found.isFetching && !found.data ? (
            <div className="p-3 flex justify-center">
              <Spinner size={18} />
            </div>
          ) : (found.data?.items ?? []).length === 0 ? (
            <div className="px-3 py-2.5 text-sm text-muted">
              {t('admin-marketing.banner.link.no_product')}
            </div>
          ) : (
            found.data?.items.map((p) => (
              <button
                key={p.id}
                type="button"
                role="option"
                aria-selected={false}
                onClick={() => onChange(productHref(p))}
                className="flex items-center gap-2.5 w-full px-2.5 py-1.5 border-0 bg-surface-card cursor-pointer text-left hover:bg-gray-50 focus-visible:outline-none focus-visible:bg-gray-100"
              >
                <img
                  src={p.image}
                  alt=""
                  width={32}
                  height={32}
                  className="w-8 h-8 object-cover rounded-sm flex-none"
                />
                <span className="flex-1 min-w-0">
                  <span className="block text-sm text-body truncate">{p.title}</span>
                  <span className="block text-xs text-muted">
                    {p.shopName} · {formatVND(p.price, { locale })}
                  </span>
                </span>
              </button>
            ))
          )}
        </div>
      )}
    </div>
  );
}

function LinkPicker({
  link,
  error,
  onChange,
}: {
  link: string;
  error?: string;
  onChange: (l: string) => void;
}) {
  const { t } = useT();
  const parsed = parseLink(link);
  const [type, setType] = useState<LinkType>(parsed.type);
  const value = type === parsed.type ? parsed.value : '';
  const cats = useQuery({
    queryKey: ['catalog', 'all'],
    queryFn: listCategories,
    staleTime: Infinity,
  });
  const shops = useQuery({ queryKey: ['shop', 'all'], queryFn: listShops, staleTime: Infinity });
  const pick = (next: LinkType) => {
    setType(next);
    onChange(next === 'flash' ? FLASH_LINK : next === 'discover' ? DISCOVER_LINK : '');
  };
  return (
    <div>
      <div className="grid grid-cols-[170px_minmax(0,1fr)] gap-3 items-start max-sm:grid-cols-1">
        <Select<LinkType>
          label={t('admin-marketing.banner.link.label')}
          value={type}
          onChange={pick}
          options={LINK_TYPES.map((x) => ({
            value: x,
            label: t(`admin-marketing.banner.link.types.${x}`),
          }))}
        />
        {type === 'category' && (
          <Select
            label={t('admin-marketing.banner.link.category')}
            value={value}
            onChange={(slug) => onChange(slug ? `/c/${slug}` : '')}
            options={[
              { value: '', label: t('admin-marketing.banner.link.pick_category') },
              ...(cats.data ?? []).map((c) => ({
                value: c.slug,
                label: c.parentId ? `— ${c.name}` : c.name,
              })),
            ]}
          />
        )}
        {type === 'product' && <ProductPicker value={value} onChange={onChange} />}
        {type === 'shop' && (
          <Select
            label={t('admin-marketing.banner.link.shop')}
            value={value}
            onChange={(id) => onChange(id ? `/shop/${id}` : '')}
            options={[
              { value: '', label: t('admin-marketing.banner.link.pick_shop') },
              ...(shops.data ?? []).map((s) => ({ value: String(s.id), label: s.name })),
            ]}
          />
        )}
        {(type === 'flash' || type === 'discover') && (
          <div className="self-end min-h-[42px] flex items-center text-sm text-muted">
            {t(`admin-marketing.banner.link.${type}_note`)}
          </div>
        )}
        {type === 'campaign' && (
          <Input
            label={t('admin-marketing.banner.link.path')}
            value={value}
            onChange={(e) => onChange(e.target.value)}
            placeholder="/campaign/sieu-sale-12-12"
          />
        )}
      </div>
      {error && (
        <p role="alert" className="m-0 mt-1.5 flex items-center gap-1 text-xs text-red-600">
          <Icon name="circle-alert" size={13} />
          {error}
        </p>
      )}
    </div>
  );
}

function ImagePick({
  label,
  preview,
  busy,
  invalid,
  onFile,
  compact,
}: {
  label: string;
  preview: string | null;
  busy: boolean;
  invalid?: boolean;
  onFile: (f: File) => void;
  compact?: boolean;
}) {
  const { t } = useT();
  const ref = useRef<HTMLInputElement>(null);
  return (
    <>
      <input
        ref={ref}
        type="file"
        hidden
        accept="image/jpeg,image/png,image/webp"
        onChange={(e) => {
          const f = e.target.files?.[0];
          e.target.value = '';
          if (f) onFile(f);
        }}
      />
      {compact ? (
        <div className="flex items-center gap-2.5 mt-2">
          <Button
            size="sm"
            variant="outline"
            iconLeft="upload"
            loading={busy}
            onClick={() => ref.current?.click()}
          >
            {label}
          </Button>
          {preview && (
            <img
              src={preview}
              alt=""
              width={48}
              height={48}
              className="w-12 h-12 object-cover rounded-sm"
            />
          )}
        </div>
      ) : (
        <button
          type="button"
          onClick={() => ref.current?.click()}
          aria-label={label}
          className={cn(
            'mt-1.5 w-full h-[110px] border-[1.5px] border-dashed rounded-md bg-gray-25 cursor-pointer grid place-items-center overflow-hidden text-muted focus-visible:outline-none focus-visible:shadow-focus',
            invalid ? 'border-red-500' : 'border-line-strong',
          )}
        >
          {busy ? (
            <Spinner size={20} />
          ) : preview ? (
            <img
              src={preview}
              alt=""
              width={520}
              height={110}
              className="w-full h-full object-cover"
            />
          ) : (
            <span className="inline-flex items-center gap-2 text-sm">
              <Icon name="upload" size={18} />
              {t('admin-marketing.banner.pick_image')}
            </span>
          )}
        </button>
      )}
    </>
  );
}

export function BannerModal({
  open,
  editing,
  rows,
  onClose,
}: {
  open: boolean;
  editing: AdminBanner | null;
  rows: AdminBanner[];
  onClose: () => void;
}) {
  const { t } = useT();
  const save = useSaveBanner();
  const cats = useQuery({
    queryKey: ['catalog', 'all'],
    queryFn: listCategories,
    staleTime: Infinity,
  });
  const roots = (cats.data ?? []).filter((c) => c.parentId === null);
  const [img, setImg] = useState<{ key: string | null; preview: string | null }>({
    key: null,
    preview: null,
  });
  const [mob, setMob] = useState<{ key: string | null; preview: string | null }>({
    key: null,
    preview: null,
  });
  const [busy, setBusy] = useState<'desktop' | 'mobile' | null>(null);
  const blank = (): BannerValues => ({
    title: '',
    hasImage: false,
    targetUrl: '',
    position: 'HOME_TOP_CAROUSEL',
    categoryId: null,
    sortOrder: nextOrder(rows, { position: 'HOME_TOP_CAROUSEL', categoryId: null }),
    startsAt: '',
    endsAt: '',
    active: true,
  });
  const form = useForm<BannerValues>({
    resolver: zodResolver(bannerSchema),
    defaultValues: blank(),
  });
  useEffect(() => {
    if (!open) return;
    setImg({ key: null, preview: editing?.imageUrl ?? null });
    setMob({ key: null, preview: editing?.mobileImageUrl ?? null });
    form.reset(
      editing
        ? {
            title: editing.title,
            hasImage: true,
            targetUrl: editing.targetUrl,
            position: editing.position,
            categoryId: editing.category?.id ?? null,
            sortOrder: editing.sortOrder,
            startsAt: editing.startsAt.slice(0, 10),
            endsAt: editing.endsAt.slice(0, 10),
            active: editing.active,
          }
        : blank(),
    );
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [open, editing]);
  const v = form.watch();
  const e = form.formState.errors;
  const m = (x?: string) => (x ? t(x) : undefined);
  const position = v.position as BannerPosition;
  const slotCount = rows.filter(
    (b) =>
      b.id !== editing?.id &&
      b.position === position &&
      (position !== 'CATEGORY_TOP' || b.category?.id === v.categoryId),
  ).length;

  const upload = async (f: File, which: 'desktop' | 'mobile') => {
    setBusy(which);
    try {
      const r = await uploadFile(f, 'BANNER_IMAGE');
      if (which === 'desktop') {
        setImg({ key: r.objectKey, preview: r.previewUrl });
        form.setValue('hasImage', true, { shouldValidate: true, shouldDirty: true });
      } else setMob({ key: r.objectKey, preview: r.previewUrl });
    } catch (err) {
      toast.error(
        err instanceof UploadError
          ? t(`admin-marketing.banner.err.upload_${err.issue}`, { mb: maxMb('BANNER_IMAGE') })
          : errorMessage(err, t),
      );
    } finally {
      setBusy(null);
    }
  };

  const reslot = (p: BannerPosition, categoryId: number | null) => {
    form.setValue('position', p, { shouldDirty: true });
    form.setValue('categoryId', categoryId, {
      shouldDirty: true,
      shouldValidate: form.formState.isSubmitted,
    });
    if (!editing) form.setValue('sortOrder', nextOrder(rows, { position: p, categoryId }));
  };

  const submit = form.handleSubmit((x) => {
    const target = sitePath(x.targetUrl);
    if (!target) {
      form.setError('targetUrl', { message: 'admin-marketing.banner.err.link_domain' });
      return;
    }
    const cat = roots.find((c) => c.id === x.categoryId);
    save.mutate(
      {
        id: editing?.id ?? null,
        input: {
          title: x.title,
          imageObjectKey: img.key,
          mobileImageObjectKey: mob.key,
          imagePreview: img.key ? img.preview : null,
          mobilePreview: mob.key ? mob.preview : null,
          targetUrl: target,
          position: x.position as BannerPosition,
          categoryId: x.categoryId,
          sortOrder: x.sortOrder,
          startsAt: x.startsAt,
          endsAt: x.endsAt,
          active: x.active,
        },
        category: cat ? { id: cat.id, name: cat.name, slug: cat.slug } : null,
      },
      {
        onSuccess: () => {
          toast.success(t('admin-marketing.banner.saved'));
          onClose();
        },
        onError: (err) => {
          if (isApiError(err) && err.code === 'GNE-BNR-4221')
            form.setError('position', { message: 'errors.GNE-BNR-4221' });
          else if (isApiError(err) && err.code === 'GNE-BNR-4222')
            form.setError('targetUrl', { message: 'errors.GNE-BNR-4222' });
          else toast.error(errorMessage(err, t));
        },
      },
    );
  });

  return (
    <Modal
      open={open}
      onClose={onClose}
      width={560}
      dismissible={!form.formState.isDirty}
      title={
        editing ? t('admin-marketing.banner.edit_title') : t('admin-marketing.banner.add_title')
      }
      footer={
        <>
          <Button variant="ghost" onClick={onClose}>
            {t('common.action.cancel')}
          </Button>
          <Button
            iconLeft="check"
            loading={save.isPending}
            disabled={busy !== null}
            onClick={() => void submit()}
          >
            {t('admin-marketing.banner.save')}
          </Button>
        </>
      }
    >
      <form className="flex flex-col gap-3.5" onSubmit={(ev) => void submit(ev)} noValidate>
        <Input
          label={t('admin-marketing.banner.title')}
          placeholder={t('admin-marketing.banner.title_placeholder')}
          maxLength={120}
          error={m(e.title?.message)}
          {...form.register('title')}
        />
        <div>
          <span className={lab}>{t('admin-marketing.banner.image')}</span>
          <ImagePick
            label={t('admin-marketing.banner.image')}
            preview={img.preview}
            busy={busy === 'desktop'}
            invalid={!!e.hasImage}
            onFile={(f) => void upload(f, 'desktop')}
          />
          {e.hasImage && (
            <p role="alert" className="m-0 mt-1.5 text-xs text-red-600">
              {m(e.hasImage.message)}
            </p>
          )}
          <ImagePick
            compact
            label={
              mob.preview
                ? t('admin-marketing.banner.mobile_change')
                : t('admin-marketing.banner.mobile_add')
            }
            preview={mob.preview}
            busy={busy === 'mobile'}
            onFile={(f) => void upload(f, 'mobile')}
          />
        </div>
        <LinkPicker
          link={v.targetUrl}
          error={m(e.targetUrl?.message)}
          onChange={(l) =>
            form.setValue('targetUrl', l, {
              shouldDirty: true,
              shouldValidate: form.formState.isSubmitted,
            })
          }
        />
        <div className="grid grid-cols-2 gap-3 max-sm:grid-cols-1">
          <div>
            <Select<string>
              label={t('admin-marketing.banner.position')}
              value={v.position}
              onChange={(p) =>
                reslot(p as BannerPosition, p === 'CATEGORY_TOP' ? (roots[0]?.id ?? null) : null)
              }
              options={BANNER_POSITIONS.map((p) => ({
                value: p,
                label: t(`admin-marketing.banner.positions.${p}.label`),
              }))}
            />
            <p
              className={cn('m-0 mt-1 text-xs', e.position ? 'text-red-600' : 'text-muted')}
              role={e.position ? 'alert' : undefined}
            >
              {e.position
                ? m(e.position.message)
                : t(`admin-marketing.banner.positions.${position}.where`)}
            </p>
          </div>
          {position === 'CATEGORY_TOP' && (
            <Select<number>
              label={t('admin-marketing.banner.category')}
              value={v.categoryId ?? 0}
              onChange={(id) => reslot('CATEGORY_TOP', id || null)}
              error={m(e.categoryId?.message)}
              options={roots.map((c) => ({ value: c.id, label: c.name }))}
            />
          )}
          <div>
            <Input
              label={t('admin-marketing.banner.order')}
              inputMode="numeric"
              {...form.register('sortOrder', { valueAsNumber: true })}
            />
            <p className="m-0 mt-1 text-xs text-muted">
              {slotCount
                ? t('admin-marketing.banner.order_hint', { count: slotCount })
                : t('admin-marketing.banner.order_first')}
            </p>
          </div>
        </div>
        <fieldset className="border-0 p-0 m-0">
          <legend className={lab}>{t('admin-marketing.banner.period')}</legend>
          <div className="flex items-center gap-2.5 mt-1.5">
            <input
              type="date"
              aria-label={t('admin-marketing.banner.start')}
              className={cn(box, e.startsAt ? 'border-red-500' : 'border-line')}
              {...form.register('startsAt')}
            />
            <Icon name="arrow-right" size={16} className="text-subtle flex-none" />
            <input
              type="date"
              aria-label={t('admin-marketing.banner.end')}
              min={v.startsAt || undefined}
              className={cn(box, e.endsAt ? 'border-red-500' : 'border-line')}
              {...form.register('endsAt')}
            />
          </div>
          {(e.startsAt || e.endsAt) && (
            <p role="alert" className="m-0 mt-1.5 text-xs text-red-600">
              {m(e.endsAt?.message ?? e.startsAt?.message)}
            </p>
          )}
        </fieldset>
        <label className="flex items-center gap-2.5 text-sm text-body">
          <Switch
            checked={v.active}
            onChange={() => form.setValue('active', !v.active, { shouldDirty: true })}
          />
          {t('admin-marketing.banner.active')}
        </label>
      </form>
    </Modal>
  );
}
