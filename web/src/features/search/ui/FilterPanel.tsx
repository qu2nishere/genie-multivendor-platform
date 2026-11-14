// Bộ lọc bên trái (prototype Category.jsx / Search.jsx: aside.tr-filters, Group, fRow, priceBox) —
// plan G08.18. Là một <form method="get"> thật: tắt JS vẫn lọc được bằng nút "Áp dụng" (R16).
// Có JS: màn rộng đổi lựa chọn là gửi ngay; màn hẹp là ngăn kéo, chọn xong bấm "Xem N sản phẩm".
import { useStore } from '@nanostores/react';
import { useEffect, useRef, useState, type FormEvent, type ReactNode } from 'react';
import { useFocusTrap } from '@/shared/hooks/useFocusTrap';
import { MQ, useMediaQuery } from '@/shared/hooks/useMediaQuery';
import { useScrollLock } from '@/shared/hooks/useScrollLock';
import { useT } from '@/shared/hooks/useT';
import { formatNumber } from '@/shared/lib/format';
import { Badge, Button, Checkbox, Icon } from '@/shared/ui';
import { $filtersOpen, closeFilters } from '../application/filtersDrawer';
import {
  CATEGORY_BANDS,
  clearedFilters,
  filtersHref,
  RATINGS,
  SEARCH_BANDS,
  type FilterState,
} from '../domain/filters';
import { bandLabel } from './labels';

export interface FilterPanelProps {
  /** Đích của form (đường dẫn trang hiện tại). */
  path: string;
  state: FilterState;
  variant: 'category' | 'search';
  /** Tìm kiếm toàn sàn: danh mục gốc để lọc. */
  categories?: { slug: string; name: string }[];
  brands: string[];
  /** Thương hiệu đã xác minh (hiện dấu tích). */
  verifiedBrands?: string[];
  places?: string[];
  /** Thuộc tính lọc theo ngành (UC-CAT-05) khi đã chọn đúng một danh mục. */
  attribute?: { name: string; values: string[] } | null;
  /** Đang tìm trong một shop: ẩn danh mục / thương hiệu / nơi bán (prototype !inShop). */
  inShop?: boolean;
  resultCount: number;
}

const ROW = 'min-h-[34px] max-md:min-h-[44px] text-sm';

function Group({ title, children }: { title: string; children: ReactNode }) {
  return (
    <fieldset className="m-0 mt-3 border-0 border-t border-solid border-line-subtle p-0 pt-3">
      <legend className="float-left mb-2 w-full p-0 text-xs font-semibold uppercase tracking-[.04em] text-strong">
        {title}
      </legend>
      <div className="clear-both flex flex-col">{children}</div>
    </fieldset>
  );
}

function Radio({
  name,
  value,
  checked,
  onPick,
  children,
}: {
  name: string;
  value: string;
  checked: boolean;
  onPick?: () => void;
  children: ReactNode;
}) {
  return (
    <label className={`flex cursor-pointer items-center gap-[9px] text-body ${ROW}`}>
      <input
        type="radio"
        name={name}
        value={value}
        defaultChecked={checked}
        onChange={onPick}
        className="h-4 w-4 flex-none accent-[var(--color-primary)]"
      />
      {children}
    </label>
  );
}

function Stars({ n }: { n: number }) {
  const { t } = useT();
  return (
    <span className="inline-flex items-center gap-0.5">
      {Array.from({ length: 5 }, (_, i) => (
        <Icon
          key={i}
          name="star"
          size={13}
          style={{
            color: i < n ? 'var(--gold-500)' : 'var(--gray-300)',
            fill: i < n ? 'var(--gold-500)' : 'none',
          }}
        />
      ))}
      <span className="ml-1">{t('search.filter.and_up')}</span>
    </span>
  );
}

export default function FilterPanel({
  path,
  state: s,
  variant,
  categories = [],
  brands,
  verifiedBrands = [],
  places = [],
  attribute,
  inShop = false,
  resultCount,
}: FilterPanelProps) {
  const { t, locale } = useT();
  const open = useStore($filtersOpen);
  const narrow = useMediaQuery(MQ.narrow);
  const form = useRef<HTMLFormElement>(null);
  const aside = useRef<HTMLElement>(null);
  const [dirty, setDirty] = useState(false);
  const [js, setJs] = useState(false);
  const bands = variant === 'category' ? CATEGORY_BANDS : SEARCH_BANDS;
  const drawer = narrow && open;

  useEffect(() => setJs(true), []);
  useFocusTrap(aside, drawer);
  useScrollLock(drawer);
  useEffect(() => {
    if (!drawer) return undefined;
    const onKey = (e: KeyboardEvent) => e.key === 'Escape' && closeFilters();
    window.addEventListener('keydown', onKey);
    return () => window.removeEventListener('keydown', onKey);
  }, [drawer]);

  /** Màn rộng: gửi ngay. Màn hẹp: chờ nút "Xem N sản phẩm". */
  const changed = () => {
    if (narrow) setDirty(true);
    else form.current?.requestSubmit();
  };

  // Chọn khoảng sẵn thì xoá ô tự nhập; gõ ô tự nhập thì bỏ chọn khoảng sẵn (prototype)
  const pickBand = () => {
    const f = form.current;
    if (f) {
      (f.elements.namedItem('min') as HTMLInputElement).value = '';
      (f.elements.namedItem('max') as HTMLInputElement).value = '';
    }
    changed();
  };
  const typePrice = () => {
    form.current?.querySelectorAll<HTMLInputElement>('input[name="band"]').forEach((r) => {
      r.checked = false;
    });
    setDirty(true);
  };
  const submitPrice = () => {
    if (!narrow && dirty) form.current?.requestSubmit();
  };

  /** Bỏ tham số rỗng để URL gọn (min=&max=…). */
  const onSubmit = (e: FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    const data = new FormData(e.currentTarget);
    const params = new URLSearchParams();
    data.forEach((v, k) => {
      const raw = k === 'min' || k === 'max' ? String(v).replace(/\D/g, '') : String(v);
      if (raw) params.append(k, raw);
    });
    closeFilters();
    window.location.assign(params.toString() ? `${path}?${params}` : path);
  };

  const hidden: [string, string][] = [];
  if (s.q) hidden.push(['q', s.q]);
  if (s.scope === 'shop' && s.shop) hidden.push(['scope', 'shop']);
  if (s.shop) hidden.push(['shop', String(s.shop)]);
  if (s.sub) hidden.push(['sub', s.sub]);
  if (s.sort !== 'rel') hidden.push(['sort', s.sort]);
  if (s.sort === 'price' && s.dir === 'desc') hidden.push(['dir', 'desc']);
  if (inShop) s.cats.forEach((c) => hidden.push(['cat', c]));

  const priceBox =
    'h-[34px] w-full min-w-0 rounded-sm border border-solid border-line bg-surface-sunken px-2 text-center text-sm text-body ' +
    'placeholder:text-subtle focus-visible:outline-none focus-visible:shadow-focus max-md:h-11';

  return (
    <aside
      ref={aside}
      id="filters"
      className="tr-filters sticky top-[calc(var(--top-h,86px)+12px)] rounded-lg border border-solid border-line-subtle bg-surface-card px-4 pb-5 pt-4 shadow-sm"
      data-open={drawer ? 1 : 0}
      role={drawer ? 'dialog' : undefined}
      aria-modal={drawer ? true : undefined}
      aria-labelledby="filters-title"
    >
      <button
        type="button"
        className="tr-filters__close"
        aria-label={t('search.filter.close')}
        onClick={closeFilters}
      >
        <Icon name="x" size={22} />
      </button>
      <h2
        id="filters-title"
        className="m-0 flex items-center gap-[7px] text-sm font-bold text-strong"
      >
        <Icon name="sliders-horizontal" size={17} className="text-primary" />
        {t('search.filter.title')}
      </h2>

      <form ref={form} method="get" action={path} onSubmit={onSubmit}>
        {hidden.map(([k, v]) => (
          <input key={`${k}=${v}`} type="hidden" name={k} value={v} />
        ))}

        {variant === 'category' && (
          <Group title={t('search.filter.service')}>
            <Checkbox
              name="authentic"
              value="1"
              defaultChecked={s.authentic}
              onChange={changed}
              className={ROW}
            >
              {t('search.filter.authentic_shop')}
            </Checkbox>
            <Checkbox
              name="freeship"
              value="1"
              defaultChecked={s.freeship}
              onChange={changed}
              className={ROW}
            >
              {t('search.filter.freeship')}
            </Checkbox>
          </Group>
        )}

        {variant === 'search' && !inShop && categories.length > 0 && (
          <Group title={t('search.filter.category')}>
            {categories.map((c) => (
              <Checkbox
                key={c.slug}
                name="cat"
                value={c.slug}
                defaultChecked={s.cats.includes(c.slug)}
                onChange={changed}
                className={ROW}
              >
                {c.name}
              </Checkbox>
            ))}
          </Group>
        )}

        {!inShop && brands.length > 0 && (
          <Group title={t('search.filter.brand')}>
            {brands.map((b) => {
              const label = (
                <span className="inline-flex items-center gap-1">
                  {b}
                  {verifiedBrands.includes(b) && (
                    <Icon
                      name="badge-check"
                      size={14}
                      className="text-primary"
                      aria-label={t('search.filter.brand_verified')}
                    />
                  )}
                </span>
              );
              return variant === 'category' ? (
                <Radio key={b} name="brand" value={b} checked={s.brands[0] === b} onPick={changed}>
                  {label}
                </Radio>
              ) : (
                <Checkbox
                  key={b}
                  name="brand"
                  value={b}
                  defaultChecked={s.brands.includes(b)}
                  onChange={changed}
                  className={ROW}
                >
                  {label}
                </Checkbox>
              );
            })}
          </Group>
        )}

        {attribute && (
          <Group title={attribute.name}>
            {attribute.values.map((a) => (
              <Checkbox
                key={a}
                name="attr"
                value={a}
                defaultChecked={s.attrs.includes(a)}
                onChange={changed}
                className={ROW}
              >
                {a}
              </Checkbox>
            ))}
          </Group>
        )}

        {variant === 'search' && !inShop && places.length > 0 && (
          <Group title={t('search.filter.place')}>
            {places.map((p) => (
              <Checkbox
                key={p}
                name="place"
                value={p}
                defaultChecked={s.places.includes(p)}
                onChange={changed}
                className={ROW}
              >
                {p}
              </Checkbox>
            ))}
          </Group>
        )}

        <Group title={t('search.filter.price')}>
          <div className="mb-2 flex items-center gap-1.5">
            <input
              name="min"
              inputMode="numeric"
              defaultValue={s.min ?? ''}
              placeholder={t('search.filter.price_from')}
              aria-label={t('search.filter.price_min')}
              className={priceBox}
              onInput={(e) => {
                e.currentTarget.value = e.currentTarget.value.replace(/\D/g, '');
                typePrice();
              }}
              onBlur={submitPrice}
            />
            <span className="text-subtle" aria-hidden="true">
              —
            </span>
            <input
              name="max"
              inputMode="numeric"
              defaultValue={s.max ?? ''}
              placeholder={t('search.filter.price_to')}
              aria-label={t('search.filter.price_max')}
              className={priceBox}
              onInput={(e) => {
                e.currentTarget.value = e.currentTarget.value.replace(/\D/g, '');
                typePrice();
              }}
              onBlur={submitPrice}
            />
          </div>
          {bands.map((b) => (
            <Radio
              key={b.id}
              name="band"
              value={b.id}
              checked={s.band === b.id && s.min == null && s.max == null}
              onPick={pickBand}
            >
              {bandLabel(b, t, locale)}
            </Radio>
          ))}
        </Group>

        <Group title={t('search.filter.rating')}>
          {RATINGS.map((r) => (
            <Radio
              key={r}
              name="rating"
              value={String(r)}
              checked={s.rating === r}
              onPick={changed}
            >
              <Stars n={r} />
            </Radio>
          ))}
        </Group>

        {variant === 'search' && (
          <Group title={t('search.filter.service_more')}>
            <Checkbox
              name="authentic"
              value="1"
              defaultChecked={s.authentic}
              onChange={changed}
              className={ROW}
            >
              <span className="inline-flex items-center gap-1.5">
                <Badge variant="primary">{t('search.filter.authentic')}</Badge>
                {t('search.filter.authentic')}
              </span>
            </Checkbox>
            <Checkbox
              name="freeship"
              value="1"
              defaultChecked={s.freeship}
              onChange={changed}
              className={ROW}
            >
              <span className="inline-flex items-center gap-1.5">
                <Icon name="truck" size={14} style={{ color: 'var(--mint-600)' }} />
                {t('search.filter.freeship_long')}
              </span>
            </Checkbox>
            {!inShop && (
              <Checkbox
                name="voucher"
                value="1"
                defaultChecked={s.voucher}
                onChange={changed}
                className={ROW}
              >
                <span className="inline-flex items-center gap-1.5">
                  <Icon name="ticket" size={14} className="text-primary" />
                  {t('search.filter.voucher')}
                </span>
              </Checkbox>
            )}
          </Group>
        )}

        {/* Không JS hoặc vừa gõ khoảng giá: nút áp dụng; màn rộng có JS thì lựa chọn tự gửi */}
        {(!js || (dirty && !narrow)) && (
          <Button type="submit" block variant="secondary" className="mt-3.5">
            {t('search.filter.apply')}
          </Button>
        )}
        <Button
          href={filtersHref(path, clearedFilters(s))}
          block
          variant="outline"
          iconLeft="refresh-cw"
          className="mt-3.5"
        >
          {t('search.filter.clear_all')}
        </Button>
        <div className="tr-filters__done">
          <Button type="submit" block size="lg">
            {dirty
              ? t('search.filter.apply')
              : t('search.filter.show_n', { n: formatNumber(resultCount, { locale }) })}
          </Button>
        </div>
      </form>
    </aside>
  );
}
