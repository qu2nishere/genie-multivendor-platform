// Khối Hình ảnh + video (G11.24: kéo thả / phím ← → sắp xếp, ảnh đầu là ảnh chính, nén WebP) và Giấy tờ chứng
// thực (G11.30: PDF/DOC/XLS ≤ 10 MB, tối đa 4, bắt buộc theo ngành) — prototype ImageUpload, LegalDocs.
import { useRef, useState, type KeyboardEvent } from 'react';
import { useWatch } from 'react-hook-form';
import { UploadError, uploadFile } from '@/shared/api/upload';
import { errorMessage } from '@/shared/api/errors';
import { useT } from '@/shared/hooks/useT';
import { cn } from '@/shared/lib/cn';
import { Badge, Button, Icon, Spinner } from '@/shared/ui';
import { MAX_DOCS, MAX_IMAGES } from '../../domain/product.rules';
import type { MediaRef } from '../../domain/product.types';
import { errCls, hintCls, SectionCard, type SectionProps } from './editorKit';

export function ImageUpload({ form, readOnly, issues }: SectionProps) {
  const { t } = useT();
  const images = useWatch({ control: form.control, name: 'images' });
  const video = useWatch({ control: form.control, name: 'video' });
  const fileRef = useRef<HTMLInputElement>(null);
  const videoRef = useRef<HTMLInputElement>(null);
  const dragFrom = useRef<number | null>(null);
  const [busy, setBusy] = useState(false);
  const [err, setErr] = useState<string | null>(null);
  const set = (next: MediaRef[]) => form.setValue('images', next, { shouldDirty: true });

  const move = (from: number, to: number) => {
    if (to < 0 || to >= images.length || from === to) return;
    const next = images.slice();
    const [x] = next.splice(from, 1);
    next.splice(to, 0, x);
    set(next);
  };

  const pick = async (files: File[]) => {
    if (!files.length) return;
    setBusy(true);
    setErr(null);
    try {
      const room = MAX_IMAGES - images.length;
      const up = await Promise.all(files.slice(0, room).map((f) => uploadFile(f, 'PRODUCT_IMAGE')));
      set([...images, ...up.map((u) => ({ objectKey: u.objectKey, url: u.previewUrl ?? '' }))]);
    } catch (e) {
      setErr(e instanceof UploadError ? t('seller-products.images.err') : errorMessage(e, t));
    } finally {
      setBusy(false);
    }
  };

  const pickVideo = async (f: File | undefined) => {
    if (!f) return;
    setBusy(true);
    setErr(null);
    try {
      const u = await uploadFile(f, 'PRODUCT_VIDEO');
      form.setValue('video', { objectKey: u.objectKey, name: u.name }, { shouldDirty: true });
    } catch (e) {
      setErr(e instanceof UploadError ? t('seller-products.images.video_err') : errorMessage(e, t));
    } finally {
      setBusy(false);
    }
  };

  const onKey = (e: KeyboardEvent, i: number) => {
    if (readOnly) return;
    if (e.key === 'ArrowLeft') move(i, i - 1);
    else if (e.key === 'ArrowRight') move(i, i + 1);
    else if (e.key === 'Delete') set(images.filter((_, k) => k !== i));
  };

  return (
    <SectionCard
      id="sec-images"
      icon="image"
      title={t('seller-products.images.title')}
      error={issues.includes('images')}
      right={
        <>
          {images.length === 0 && <Badge variant="danger">{t('seller-products.images.min')}</Badge>}
          <Badge variant="neutral" className="ml-auto">
            {t('seller-products.images.rule')}
          </Badge>
        </>
      }
    >
      <ul
        className="m-0 p-0 list-none flex items-start gap-3 flex-wrap"
        aria-label={t('seller-products.images.list')}
      >
        {images.map((img, i) => {
          const main = i === 0;
          const dim = main
            ? 'w-[200px] h-[200px] max-sm:w-[140px] max-sm:h-[140px]'
            : 'w-[120px] h-[120px] max-sm:w-[96px] max-sm:h-[96px]';
          return (
            <li
              key={img.objectKey}
              tabIndex={readOnly ? -1 : 0}
              draggable={!readOnly}
              aria-label={t(
                main ? 'seller-products.images.slot_main' : 'seller-products.images.slot',
                { n: i + 1 },
              )}
              onDragStart={() => (dragFrom.current = i)}
              onDragOver={(e) => e.preventDefault()}
              onDrop={() => {
                if (dragFrom.current !== null) move(dragFrom.current, i);
                dragFrom.current = null;
              }}
              onKeyDown={(e) => onKey(e, i)}
              className={cn(
                'relative flex-none rounded-md overflow-hidden focus-visible:outline-none focus-visible:shadow-focus',
                dim,
                !readOnly && 'cursor-grab',
                main
                  ? 'border-2 border-dashed border-primary'
                  : 'border-[1.5px] border-dashed border-line-strong',
              )}
            >
              {img.url ? (
                <img src={img.url} alt="" className="w-full h-full object-cover" />
              ) : (
                <div className="w-full h-full grid place-items-center bg-gray-100">
                  <Icon name="image" size={main ? 40 : 26} className="text-black/30" />
                </div>
              )}
              {main && (
                <span className="absolute top-1.5 left-1.5 text-[10px] font-bold text-white bg-primary rounded-full px-2 py-[3px] inline-flex items-center gap-[3px]">
                  <Icon name="star" size={11} />
                  {t('seller-products.images.main')}
                </span>
              )}
              {!readOnly && (
                <button
                  type="button"
                  onClick={() => set(images.filter((_, k) => k !== i))}
                  aria-label={t('seller-products.images.remove')}
                  className="absolute top-1.5 right-1.5 w-6 h-6 rounded-full border-0 bg-surface-overlay text-white cursor-pointer grid place-items-center"
                >
                  <Icon name="x" size={14} />
                </button>
              )}
            </li>
          );
        })}
        {!readOnly && images.length < MAX_IMAGES && (
          <li>
            <button
              type="button"
              onClick={() => fileRef.current?.click()}
              disabled={busy}
              className="w-[120px] h-[120px] max-sm:w-[96px] max-sm:h-[96px] flex-none rounded-md border-[1.5px] border-dashed border-line-strong bg-gray-50 hover:bg-gray-100 hover:border-primary cursor-pointer flex flex-col items-center justify-center gap-1.5 text-subtle"
            >
              {busy ? <Spinner size={22} /> : <Icon name="image-plus" size={28} />}
              <span className="text-xs">{t('seller-products.images.add')}</span>
            </button>
          </li>
        )}
      </ul>
      <input
        ref={fileRef}
        type="file"
        hidden
        multiple
        accept="image/jpeg,image/png,image/webp"
        onChange={(e) => {
          const files = Array.from(e.target.files ?? []);
          e.target.value = '';
          void pick(files);
        }}
      />
      <input
        ref={videoRef}
        type="file"
        hidden
        accept="video/mp4"
        onChange={(e) => {
          const f = e.target.files?.[0];
          e.target.value = '';
          void pickVideo(f);
        }}
      />
      <p className={hintCls}>{t('seller-products.images.hint')}</p>
      <div className="flex items-center gap-2.5 mt-2.5 flex-wrap">
        {!readOnly && (
          <Button
            size="sm"
            variant="outline"
            iconLeft="upload"
            disabled={busy}
            onClick={() => videoRef.current?.click()}
          >
            {video
              ? t('seller-products.images.video_change')
              : t('seller-products.images.video_add')}
          </Button>
        )}
        <span className="text-xs text-muted">
          {video ? video.name : t('seller-products.images.video_rule')}
        </span>
        {video && !readOnly && (
          <button
            type="button"
            className="border-0 bg-transparent p-0 text-xs text-red-600 cursor-pointer"
            onClick={() => form.setValue('video', null, { shouldDirty: true })}
          >
            {t('seller-products.images.video_remove')}
          </button>
        )}
      </div>
      {err && (
        <p className={errCls} role="alert">
          <Icon name="circle-alert" size={13} />
          {err}
        </p>
      )}
    </SectionCard>
  );
}

export function LegalDocs({ form, readOnly, rules, issues }: SectionProps) {
  const { t } = useT();
  const docs = useWatch({ control: form.control, name: 'documents' });
  const [busy, setBusy] = useState(false);
  const [err, setErr] = useState<string | null>(null);
  const required = !!rules?.requiresDocuments;
  const bad = issues.includes('documents');

  const pick = async (files: File[]) => {
    if (!files.length) return;
    if (files.some((f) => f.type.startsWith('image/'))) {
      setErr(t('seller-products.docs.err_image'));
      return;
    }
    setBusy(true);
    setErr(null);
    try {
      const up = await Promise.all(
        files.slice(0, MAX_DOCS - docs.length).map((f) => uploadFile(f, 'PRODUCT_DOCUMENT')),
      );
      form.setValue(
        'documents',
        [...docs, ...up.map((u) => ({ objectKey: u.objectKey, name: u.name }))],
        {
          shouldDirty: true,
        },
      );
    } catch (e) {
      setErr(e instanceof UploadError ? t('seller-products.docs.err') : errorMessage(e, t));
    } finally {
      setBusy(false);
    }
  };

  return (
    <SectionCard
      id="sec-docs"
      icon="file-text"
      title={t('seller-products.docs.title')}
      error={bad}
      right={
        <>
          {docs.length === 0 &&
            (required ? (
              <Badge variant="danger">{t('seller-products.docs.required')}</Badge>
            ) : (
              <Badge variant="neutral">{t('seller-products.docs.optional')}</Badge>
            ))}
          <Badge variant="neutral" className="ml-auto">
            {t('seller-products.docs.rule')}
          </Badge>
        </>
      }
    >
      <ul className="m-0 p-0 list-none flex items-start gap-3 flex-wrap">
        {docs.map((d, i) => (
          <li
            key={d.objectKey}
            className="relative w-[120px] h-[120px] rounded-md border-[1.5px] border-solid border-line-strong bg-gray-50 flex flex-col items-center justify-center gap-2 p-2"
          >
            <Icon name="file-text" size={28} className="text-muted" />
            <span className="text-xs text-body text-center w-full truncate" title={d.name}>
              {d.name}
            </span>
            {!readOnly && (
              <button
                type="button"
                aria-label={t('seller-products.docs.remove', { name: d.name })}
                onClick={() =>
                  form.setValue(
                    'documents',
                    docs.filter((_, k) => k !== i),
                    { shouldDirty: true },
                  )
                }
                className="absolute top-1.5 right-1.5 w-[22px] h-[22px] rounded-full border-0 bg-surface-overlay text-white cursor-pointer grid place-items-center"
              >
                <Icon name="x" size={13} />
              </button>
            )}
          </li>
        ))}
        {!readOnly && docs.length < MAX_DOCS && (
          <li>
            <label className="w-[120px] h-[120px] rounded-md border-[1.5px] border-dashed border-line-strong bg-gray-50 hover:bg-gray-100 cursor-pointer flex flex-col items-center justify-center gap-1.5 text-subtle focus-within:shadow-focus">
              <input
                type="file"
                className="sr-only"
                multiple
                accept=".pdf,.doc,.docx,.xls,.xlsx"
                disabled={busy}
                onChange={(e) => {
                  const files = Array.from(e.target.files ?? []);
                  e.target.value = '';
                  void pick(files);
                }}
              />
              {busy ? <Spinner size={22} /> : <Icon name="upload" size={26} />}
              <span className="text-xs">{t('seller-products.docs.add')}</span>
            </label>
          </li>
        )}
      </ul>
      {(err || bad) && (
        <p className={errCls} role="alert">
          <Icon name="circle-alert" size={13} />
          {err ?? t('errors.GNE-CAT-4222')}
        </p>
      )}
      <p className={hintCls}>{t('seller-products.docs.hint')}</p>
    </SectionCard>
  );
}
