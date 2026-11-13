// Chọn + tải ảnh / video (kéo thả hoặc bấm) — dùng ở yêu cầu trả hàng, đánh giá, bổ sung bằng chứng.
// Tải ngay khi chọn (shared/api/upload: nén ảnh, kiểm MIME / cỡ, URL ký sẵn), hiện ô xem trước + nút xóa.
import { useRef, useState, type ReactNode } from 'react';
import { UploadError, uploadFile, type UploadedFile } from '@/shared/api/upload';
import { useT } from '@/shared/hooks/useT';
import { cn } from '@/shared/lib/cn';
import { maxMb, type UploadPurpose } from '@/shared/lib/upload.rules';
import { Icon } from './icon/Icon';
import { Spinner } from './Spinner';
import { toast } from './Toast';

export interface MediaPickerProps {
  value: UploadedFile[];
  onChange: (files: UploadedFile[]) => void;
  /** Mục đích theo từng tệp (ảnh / video có giới hạn khác nhau). */
  purposeOf: (f: File) => UploadPurpose;
  accept: string;
  max: number;
  label: ReactNode;
  hint?: ReactNode;
  id?: string;
}

export function MediaPicker({
  value,
  onChange,
  purposeOf,
  accept,
  max,
  label,
  hint,
  id,
}: MediaPickerProps) {
  const { t } = useT();
  const input = useRef<HTMLInputElement>(null);
  const [busy, setBusy] = useState(0);
  const full = value.length + busy >= max;

  const add = async (list: FileList | null) => {
    if (!list) return;
    const picked = Array.from(list).slice(0, Math.max(0, max - value.length - busy));
    if (picked.length < (list.length ?? 0)) toast.error(t('ui.media.too_many', { max }));
    setBusy((b) => b + picked.length);
    const done: UploadedFile[] = [];
    for (const f of picked) {
      const purpose = purposeOf(f);
      try {
        done.push(await uploadFile(f, purpose));
      } catch (e) {
        toast.error(
          e instanceof UploadError
            ? t(`ui.media.${e.issue}`, { name: f.name, mb: maxMb(purpose) })
            : t('ui.media.failed', { name: f.name }),
        );
      } finally {
        setBusy((b) => b - 1);
      }
    }
    if (done.length) onChange([...value, ...done]);
  };

  const remove = (i: number) => {
    const f = value[i];
    if (f?.previewUrl) URL.revokeObjectURL(f.previewUrl);
    onChange(value.filter((_, j) => j !== i));
  };

  return (
    <div>
      <div className="font-semibold text-sm text-strong mb-1.5" id={id ? `${id}-label` : undefined}>
        {label}
      </div>
      <input
        ref={input}
        id={id}
        type="file"
        hidden
        multiple={max > 1}
        accept={accept}
        onChange={(e) => {
          void add(e.target.files);
          e.target.value = '';
        }}
      />
      <div
        onDragOver={(e) => e.preventDefault()}
        onDrop={(e) => {
          e.preventDefault();
          void add(e.dataTransfer.files);
        }}
        className="flex flex-wrap gap-3 p-3.5 min-h-[96px] items-center rounded-md bg-surface-sunken border border-dashed border-line-strong"
      >
        {value.map((f, i) => (
          <span
            key={f.objectKey}
            className="relative w-16 h-16 rounded-md overflow-hidden border border-solid border-line-subtle bg-surface-card grid place-items-center"
            title={f.name}
          >
            {f.previewUrl ? (
              <img src={f.previewUrl} alt="" className="w-full h-full object-cover" />
            ) : (
              <Icon
                name={f.mediaType === 'VIDEO' ? 'camera' : 'file-text'}
                size={22}
                className="text-muted"
              />
            )}
            <button
              type="button"
              onClick={() => remove(i)}
              aria-label={t('ui.media.remove', { name: f.name })}
              className="absolute top-0.5 right-0.5 w-6 h-6 rounded-full border-0 bg-surface-overlay text-inverse grid place-items-center cursor-pointer"
            >
              <Icon name="x" size={12} />
            </button>
          </span>
        ))}
        {Array.from({ length: busy }, (_, i) => (
          <span
            key={`b${i}`}
            className="w-16 h-16 rounded-md bg-surface-card grid place-items-center"
          >
            <Spinner size={20} />
          </span>
        ))}
        {!full && (
          <button
            type="button"
            onClick={() => input.current?.click()}
            className={cn(
              'border-0 bg-transparent cursor-pointer text-muted text-sm flex items-center gap-2 min-h-11',
              value.length === 0 && busy === 0
                ? 'mx-auto flex-col'
                : 'w-16 h-16 justify-center rounded-md border border-dashed border-line-strong',
            )}
          >
            <Icon name={value.length ? 'plus' : 'upload'} size={value.length ? 22 : 26} />
            {value.length === 0 && busy === 0 && <span>{t('ui.media.drop')}</span>}
          </button>
        )}
      </div>
      {hint && <p className="m-0 mt-1.5 text-xs text-muted">{hint}</p>}
    </div>
  );
}
