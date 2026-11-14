// Khối Thông tin cơ bản (G11.25: tên, CategoryPicker 3 cột, BrandCombo tìm / thêm mới, tình trạng, xuất xứ),
// Thông số kỹ thuật (G11.26: gợi ý + bắt buộc theo ngành), Mô tả (G11.27: trình soạn có định dạng) —
// prototype Products.jsx BasicInfo, CategoryPicker, BrandCombo, SpecsTable, RichDescription.
import { useEffect, useId, useRef, useState } from 'react';
import { useWatch } from 'react-hook-form';
import { Link } from 'react-router-dom';
import { errorMessage } from '@/shared/api/errors';
import { UploadError, uploadFile } from '@/shared/api/upload';
import { useClickOutside } from '@/shared/hooks/useClickOutside';
import { useT } from '@/shared/hooks/useT';
import { cn } from '@/shared/lib/cn';
import { formatNumber } from '@/shared/lib/format';
import { Button, Icon, Input, Modal, Select, Skeleton, toast, type IconName } from '@/shared/ui';
import {
  useBrandSearch,
  useCategoryTree,
  useCreateBrand,
} from '../../application/useSellerProducts';
import { DESC_MAX, missingSpecs, suggestedSpecs, textLength } from '../../domain/product.rules';
import type { CategoryNode, SpecRow } from '../../domain/product.types';
import { errCls, hintCls, labCls, SectionCard, type SectionProps } from './editorKit';

function CategoryPicker({ form, readOnly, issues }: SectionProps) {
  const { t } = useT();
  const path = useWatch({ control: form.control, name: 'categoryPath' });
  const tree = useCategoryTree();
  const [open, setOpen] = useState(path.length === 0);
  const [l1, setL1] = useState<number | null>(path[0]?.id ?? null);
  const [l2, setL2] = useState<number | null>(path[1]?.id ?? null);
  const bad = issues.includes('category');

  if (!open || readOnly) {
    return (
      <div>
        <span className={labCls}>
          {t('seller-products.basic.category')} <span className="text-red-600">*</span>
        </span>
        <div className="flex items-center gap-2.5 px-3.5 py-[11px] border border-solid border-line rounded-md bg-surface-sunken">
          <Icon name="boxes" size={16} className="text-subtle" />
          <span className="flex-1 text-sm text-strong">
            {path.map((p) => p.name).join(' › ') || t('seller-products.basic.category_none')}
          </span>
          {!readOnly && (
            <Button size="sm" variant="ghost" onClick={() => setOpen(true)}>
              {t('seller-products.basic.change')}
            </Button>
          )}
        </div>
      </div>
    );
  }
  const nodes = tree.data ?? [];
  const c2 = nodes.find((n) => n.id === l1)?.children ?? null;
  const c3 = c2?.find((n) => n.id === l2)?.children ?? null;
  const Col = ({
    items,
    active,
    leaf,
    onPick,
    label,
  }: {
    items: CategoryNode[];
    active: number | null;
    leaf?: boolean;
    onPick: (n: CategoryNode) => void;
    label: string;
  }) => (
    <ul
      aria-label={label}
      className="m-0 p-0 list-none w-[200px] h-[280px] overflow-y-auto bg-surface-card border border-solid border-line-subtle rounded-md max-sm:w-full max-sm:h-auto max-sm:max-h-[220px]"
    >
      {items.map((n) => {
        const on = active === n.id;
        return (
          <li key={n.id}>
            <button
              type="button"
              aria-pressed={on}
              onClick={() => onPick(n)}
              className={cn(
                'flex items-center w-full border-0 px-3.5 py-2 cursor-pointer text-left text-[13px]',
                on
                  ? 'bg-primary-tint text-brand-700 font-semibold'
                  : 'bg-transparent text-body hover:bg-gray-50',
              )}
            >
              <span className="flex-1">{n.name}</span>
              {!leaf && <Icon name="chevron-right" size={14} className="text-subtle" />}
              {leaf && on && <Icon name="check" size={14} />}
            </button>
          </li>
        );
      })}
    </ul>
  );
  return (
    <div>
      <span className={labCls}>
        {t('seller-products.basic.category')} <span className="text-red-600">*</span>
      </span>
      {tree.isPending ? (
        <Skeleton lines={4} />
      ) : (
        <div
          className={cn(
            'flex gap-2.5 flex-wrap',
            bad && 'rounded-md outline outline-2 outline-red-500',
          )}
        >
          <Col
            label={t('seller-products.basic.level', { n: 1 })}
            items={nodes}
            active={l1}
            onPick={(n) => {
              setL1(n.id);
              setL2(null);
            }}
          />
          {c2 && (
            <Col
              label={t('seller-products.basic.level', { n: 2 })}
              items={c2}
              active={l2}
              onPick={(n) => setL2(n.id)}
            />
          )}
          {c3 && (
            <Col
              label={t('seller-products.basic.level', { n: 3 })}
              items={c3}
              leaf
              active={path[2]?.id ?? null}
              onPick={(n) => {
                const a = nodes.find((x) => x.id === l1);
                const b = c2?.find((x) => x.id === l2);
                if (!a || !b) return;
                form.setValue(
                  'categoryPath',
                  [a, b, n].map(({ id, name }) => ({ id, name })),
                  { shouldDirty: true },
                );
                setOpen(false);
              }}
            />
          )}
        </div>
      )}
      <p className={hintCls}>
        {t('seller-products.basic.category_hint')}{' '}
        <Link to="/catalog" className="text-brand-600 font-semibold">
          {t('seller-products.basic.propose')}
        </Link>
      </p>
    </div>
  );
}

function BrandCombo({ form, readOnly }: SectionProps) {
  const { t } = useT();
  const brand = useWatch({ control: form.control, name: 'brand' });
  const [q, setQ] = useState(brand?.name ?? '');
  const [open, setOpen] = useState(false);
  const box = useRef<HTMLDivElement>(null);
  const listId = useId();
  useClickOutside(box, () => setOpen(false), open);
  const search = useBrandSearch(q, open);
  const create = useCreateBrand();
  useEffect(() => setQ(brand?.name ?? ''), [brand?.name]);
  const matches = search.data ?? [];
  const exact = matches.some((b) => b.name.toLowerCase() === q.trim().toLowerCase());
  const pick = (b: { id: number; name: string }) => {
    form.setValue('brand', b, { shouldDirty: true });
    setQ(b.name);
    setOpen(false);
  };
  return (
    <div ref={box} className="relative">
      <label htmlFor={`${listId}-in`} className={labCls}>
        {t('seller-products.basic.brand')}
      </label>
      <div className="relative">
        <Icon
          name="search"
          size={15}
          className="absolute left-3 top-[15px] text-subtle pointer-events-none"
        />
        <input
          id={`${listId}-in`}
          value={q}
          disabled={readOnly}
          role="combobox"
          aria-expanded={open}
          aria-controls={listId}
          aria-autocomplete="list"
          onChange={(e) => {
            setQ(e.target.value);
            setOpen(true);
          }}
          onFocus={() => setOpen(true)}
          onKeyDown={(e) => e.key === 'Escape' && setOpen(false)}
          placeholder={t('seller-products.basic.brand_placeholder')}
          className="w-full h-11 border-[1.5px] border-solid border-line rounded-md pl-[34px] pr-3 text-base text-strong bg-surface-card outline-none focus:border-primary disabled:bg-surface-sunken"
        />
      </div>
      {open && !readOnly && (
        <ul
          id={listId}
          role="listbox"
          className="absolute top-[calc(100%+4px)] left-0 right-0 m-0 p-0 list-none bg-surface-card border border-solid border-line rounded-md shadow-lg z-dropdown max-h-60 overflow-y-auto"
        >
          {matches.map((b) => (
            <li key={b.id} role="option" aria-selected={brand?.id === b.id}>
              <button
                type="button"
                onClick={() => pick(b)}
                className="flex items-center w-full border-0 bg-transparent px-3.5 py-[9px] cursor-pointer text-left text-sm text-body hover:bg-gray-50"
              >
                <span className="flex-1">{b.name}</span>
                {brand?.id === b.id && <Icon name="check" size={14} className="text-brand-600" />}
              </button>
            </li>
          ))}
          {q.trim() && !exact && (
            <li role="option" aria-selected={false}>
              <button
                type="button"
                disabled={create.isPending}
                onClick={() =>
                  create.mutate(q.trim(), {
                    onSuccess: pick,
                    onError: (e) => toast.error(errorMessage(e, t)),
                  })
                }
                className="flex items-center gap-[7px] w-full border-0 border-t border-solid border-line-subtle bg-transparent px-3.5 py-[9px] cursor-pointer text-left text-sm font-semibold text-brand-600 hover:bg-brand-50"
              >
                <Icon name="plus" size={14} />
                {t('seller-products.basic.brand_new', { name: q.trim() })}
              </button>
            </li>
          )}
        </ul>
      )}
    </div>
  );
}

export function BasicInfo(props: SectionProps) {
  const { t } = useT();
  const { form, readOnly, issues } = props;
  const condition = useWatch({ control: form.control, name: 'condition' });
  const nameErr = form.formState.errors.name?.message;
  return (
    <SectionCard
      id="sec-basic"
      icon="clipboard-check"
      title={t('seller-products.basic.title')}
      error={issues.includes('name') || issues.includes('category')}
    >
      <div className="flex flex-col gap-4">
        <div>
          <Input
            label={t('seller-products.basic.name')}
            required
            disabled={readOnly}
            maxLength={120}
            error={
              nameErr
                ? t(nameErr)
                : issues.includes('name')
                  ? t('seller-products.err.name')
                  : undefined
            }
            {...form.register('name')}
          />
          <p className={hintCls}>{t('seller-products.basic.name_hint')}</p>
        </div>
        <CategoryPicker {...props} />
        <div className="grid grid-cols-2 gap-3.5 max-sm:grid-cols-1">
          <BrandCombo {...props} />
          <div />
        </div>
        <div className="grid grid-cols-2 gap-3.5 max-sm:grid-cols-1">
          <Select
            label={t('seller-products.basic.condition')}
            required
            disabled={readOnly}
            value={condition}
            onChange={(v) => form.setValue('condition', v, { shouldDirty: true })}
            options={(['NEW', 'USED', 'REFURBISHED'] as const).map((c) => ({
              value: c,
              label: t(`seller-products.basic.conditions.${c}`),
            }))}
          />
          <Input
            label={t('seller-products.basic.origin')}
            placeholder={t('seller-products.basic.origin_placeholder')}
            disabled={readOnly}
            maxLength={60}
            {...form.register('origin')}
          />
        </div>
      </div>
    </SectionCard>
  );
}

export function SpecsTable({ form, readOnly, rules, issues }: SectionProps) {
  const { t } = useT();
  const specs = useWatch({ control: form.control, name: 'specs' });
  const set = (next: SpecRow[]) => form.setValue('specs', next, { shouldDirty: true });
  // Luôn có một dòng trống cuối để nhập nhanh
  const rows = readOnly ? specs : [...specs, { name: '', value: '' }];
  const setRow = (i: number, patch: Partial<SpecRow>) => {
    const next = rows.map((r, k) => (k === i ? { ...r, ...patch } : r));
    set(next.filter((r) => r.name || r.value));
  };
  const required = (rules?.attributes ?? []).filter((a) => a.required).map((a) => a.name);
  const missing = missingSpecs(specs, rules);
  const sugg = suggestedSpecs(specs, rules);
  const show = issues.includes('specs');
  const cell =
    'min-w-0 border-0 bg-transparent px-3 py-2.5 text-sm text-strong outline-none focus:bg-blue-50 disabled:text-body';
  return (
    <SectionCard
      id="sec-specs"
      icon="sliders-horizontal"
      title={t('seller-products.specs.title')}
      error={show}
    >
      {!readOnly && sugg.length > 0 && (
        <div className="flex items-center flex-wrap gap-2 mb-3">
          <span className="text-xs text-muted">{t('seller-products.specs.suggest')}</span>
          {sugg.map((s) => (
            <button
              key={s}
              type="button"
              onClick={() => set([...specs, { name: s, value: '' }])}
              className="inline-flex items-center gap-1 border border-dashed border-line-strong bg-surface-card rounded-full px-2.5 py-[3px] text-xs font-semibold text-brand-600 cursor-pointer"
            >
              <Icon name="plus" size={12} />
              {s}
            </button>
          ))}
        </div>
      )}
      <div className="border border-solid border-line-subtle rounded-md overflow-hidden">
        <div className="flex bg-gray-25 text-xs font-semibold text-muted">
          <div className="w-[220px] px-3 py-2.5 max-sm:w-[40%]">
            {t('seller-products.specs.name')}
          </div>
          <div className="flex-1 px-3 py-2.5">{t('seller-products.specs.value')}</div>
          <div className="w-11" />
        </div>
        {rows.map((r, i) => {
          const empty = !r.name && !r.value;
          return (
            <div
              key={i}
              className="flex items-center border-0 border-t border-solid border-line-subtle hover:bg-gray-50"
            >
              <input
                value={r.name}
                disabled={readOnly}
                aria-label={t('seller-products.specs.name_n', { n: i + 1 })}
                onChange={(e) => setRow(i, { name: e.target.value })}
                placeholder={t('seller-products.specs.name_placeholder')}
                className={cn(
                  cell,
                  'flex-none w-[220px] max-sm:w-[40%] border-0 border-r border-solid border-line-subtle',
                )}
              />
              <input
                value={r.value}
                disabled={readOnly}
                aria-label={t('seller-products.specs.value_n', { n: i + 1 })}
                onChange={(e) => setRow(i, { value: e.target.value })}
                placeholder={t('seller-products.specs.value_placeholder')}
                className={cn(cell, 'flex-1')}
              />
              <div className="w-11 grid place-items-center">
                {!empty && !readOnly && (
                  <button
                    type="button"
                    aria-label={t('seller-products.specs.remove', { name: r.name || i + 1 })}
                    onClick={() => set(specs.filter((_, k) => k !== i))}
                    className="border-0 bg-transparent cursor-pointer text-subtle hover:text-red-600 grid place-items-center p-1.5"
                  >
                    <Icon name="x" size={15} />
                  </button>
                )}
              </div>
            </div>
          );
        })}
      </div>
      {required.length > 0 && (
        <p
          className={cn(
            'text-xs mt-2.5 mb-0',
            show && missing.length ? 'text-red-600' : 'text-muted',
          )}
        >
          {t('seller-products.specs.required', { list: required.join(', ') })}
          {show &&
            missing.length > 0 &&
            ` ${t('seller-products.specs.missing', { list: missing.join(', ') })}`}
        </p>
      )}
      <p className={cn(hintCls, 'flex gap-[7px] mt-2.5')}>
        <Icon name="info" size={14} className="flex-none mt-px" />
        {t('seller-products.specs.hint')}
      </p>
    </SectionCard>
  );
}

type Tool = {
  cmd: string;
  arg?: string;
  label: string;
  text?: string;
  icon?: IconName;
  cls?: string;
};

export function RichDescription({ form, readOnly, issues }: SectionProps) {
  const { t, locale } = useT();
  const ref = useRef<HTMLDivElement>(null);
  const imgRef = useRef<HTMLInputElement>(null);
  const initial = useRef(form.getValues('descriptionHtml'));
  const [count, setCount] = useState(textLength(initial.current));
  const [linkOpen, setLinkOpen] = useState(false);
  const [url, setUrl] = useState('https://');
  const saved = useRef<Range | null>(null);

  useEffect(() => {
    if (ref.current) ref.current.innerHTML = initial.current;
  }, []);

  const sync = () => {
    const html = ref.current?.innerHTML ?? '';
    setCount(textLength(html));
    form.setValue('descriptionHtml', html, { shouldDirty: true });
  };
  const run = (cmd: string, arg?: string) => {
    ref.current?.focus();
    document.execCommand(cmd, false, arg);
    sync();
  };
  const keepSelection = () => {
    const s = window.getSelection();
    saved.current = s && s.rangeCount ? s.getRangeAt(0) : null;
  };
  const restore = () => {
    const s = window.getSelection();
    if (saved.current && s) {
      s.removeAllRanges();
      s.addRange(saved.current);
    }
  };
  const tools: (Tool | '|')[] = [
    { cmd: 'bold', label: t('seller-products.desc.bold'), text: 'B', cls: 'font-bold' },
    { cmd: 'italic', label: t('seller-products.desc.italic'), text: 'I', cls: 'italic' },
    { cmd: 'underline', label: t('seller-products.desc.underline'), text: 'U', cls: 'underline' },
    '|',
    {
      cmd: 'formatBlock',
      arg: 'H2',
      label: t('seller-products.desc.h1'),
      text: 'H1',
      cls: 'font-semibold',
    },
    {
      cmd: 'formatBlock',
      arg: 'H3',
      label: t('seller-products.desc.h2'),
      text: 'H2',
      cls: 'font-semibold',
    },
    '|',
    { cmd: 'insertUnorderedList', label: t('seller-products.desc.ul'), icon: 'list' },
    {
      cmd: 'insertOrderedList',
      label: t('seller-products.desc.ol'),
      text: '1.',
      cls: 'font-semibold',
    },
    '|',
    { cmd: 'link', label: t('seller-products.desc.link'), icon: 'paperclip' },
    { cmd: 'image', label: t('seller-products.desc.image'), icon: 'image' },
  ];
  const over = count > DESC_MAX;
  const cColor = over ? 'text-red-600' : count > 8000 ? 'text-amber-600' : 'text-subtle';
  return (
    <SectionCard
      id="sec-desc"
      icon="file-text"
      title={t('seller-products.desc.title')}
      error={issues.includes('description')}
    >
      <input
        ref={imgRef}
        type="file"
        hidden
        accept="image/jpeg,image/png,image/webp"
        onChange={async (e) => {
          const f = e.target.files?.[0];
          e.target.value = '';
          if (!f) return;
          try {
            const u = await uploadFile(f, 'DESCRIPTION_IMAGE');
            restore();
            run('insertImage', u.previewUrl ?? '');
          } catch (er) {
            toast.error(
              er instanceof UploadError ? t('seller-products.images.err') : errorMessage(er, t),
            );
          }
        }}
      />
      <div className="border border-solid border-line rounded-md overflow-hidden focus-within:border-primary">
        {!readOnly && (
          <div
            role="toolbar"
            aria-label={t('seller-products.desc.toolbar')}
            className="flex items-center gap-0.5 flex-wrap bg-gray-25 border-0 border-b border-solid border-line-subtle px-2 py-[5px]"
          >
            {tools.map((tool, i) =>
              tool === '|' ? (
                <span key={i} className="w-px h-5 bg-line mx-1" aria-hidden="true" />
              ) : (
                <button
                  key={i}
                  type="button"
                  aria-label={tool.label}
                  title={tool.label}
                  onMouseDown={(e) => e.preventDefault()}
                  onClick={() => {
                    if (tool.cmd === 'link') {
                      keepSelection();
                      setUrl('https://');
                      setLinkOpen(true);
                    } else if (tool.cmd === 'image') {
                      keepSelection();
                      imgRef.current?.click();
                    } else run(tool.cmd, tool.arg);
                  }}
                  className={cn(
                    'min-w-8 h-8 border-0 bg-transparent rounded-md cursor-pointer text-[13px] text-body grid place-items-center px-1.5 hover:bg-gray-100',
                    tool.cls,
                  )}
                >
                  {tool.text ?? (tool.icon && <Icon name={tool.icon} size={16} />)}
                </button>
              ),
            )}
          </div>
        )}
        <div
          ref={ref}
          role="textbox"
          aria-multiline="true"
          aria-label={t('seller-products.desc.title')}
          contentEditable={!readOnly}
          suppressContentEditableWarning
          onInput={sync}
          className="min-h-[320px] px-5 py-[18px] text-base leading-[1.7] text-body outline-none [&_ul]:pl-5 [&_ol]:pl-5 [&_img]:max-w-full [&_a]:text-link"
        />
      </div>
      <div className="flex items-center gap-2 mt-2 flex-wrap">
        <span className={cn('text-xs font-semibold tabular-nums', cColor)}>
          {t('seller-products.desc.count', {
            count: formatNumber(count, { locale }),
            max: formatNumber(DESC_MAX, { locale }),
          })}
        </span>
        <span className={cn(hintCls, 'mt-0 ml-auto flex gap-1.5 items-center')}>
          <Icon name="info" size={13} />
          {t('seller-products.desc.hint')}
        </span>
      </div>
      {over && (
        <p className={errCls} role="alert">
          <Icon name="circle-alert" size={13} />
          {t('seller-products.err.description')}
        </p>
      )}
      <Modal
        open={linkOpen}
        onClose={() => setLinkOpen(false)}
        width={420}
        title={t('seller-products.desc.link')}
        footer={
          <>
            <Button variant="ghost" onClick={() => setLinkOpen(false)}>
              {t('common.action.cancel')}
            </Button>
            <Button
              disabled={!/^https?:\/\/[^\s]+\.[^\s]+/.test(url)}
              onClick={() => {
                setLinkOpen(false);
                restore();
                run('createLink', url);
              }}
            >
              {t('seller-products.desc.link_insert')}
            </Button>
          </>
        }
      >
        <Input
          label="URL"
          type="url"
          value={url}
          onChange={(e) => setUrl(e.target.value)}
          autoFocus
        />
      </Modal>
    </SectionCard>
  );
}
