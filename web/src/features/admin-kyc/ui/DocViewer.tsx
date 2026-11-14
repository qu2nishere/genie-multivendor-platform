// Trình xem tài liệu KYC (prototype KYCReview.jsx DocViewer) — plan G12.11: phóng to / thu nhỏ, xoay,
// lật; Esc / nút Đóng để thoát, giữ focus trong lớp phủ. URL tài liệu là Presigned GET 5 phút (11.3)
// nên không cho tải xuống / mở tab mới; số CCCD chỉ hiện dạng đã che ở "Dữ liệu kê khai".
import { useEffect, useRef, useState } from 'react';
import { createPortal } from 'react-dom';
import { useFocusTrap } from '@/shared/hooks/useFocusTrap';
import { useScrollLock } from '@/shared/hooks/useScrollLock';
import { useT } from '@/shared/hooks/useT';
import { Icon, type IconName } from '@/shared/ui';
import type { KycDocument } from '../domain/kyc.types';

export function DocViewer({
  doc,
  label,
  onClose,
}: {
  doc: KycDocument;
  label: string;
  onClose: () => void;
}) {
  const { t } = useT();
  const box = useRef<HTMLDivElement>(null);
  const [zoom, setZoom] = useState(1);
  const [rot, setRot] = useState(0);
  const [flip, setFlip] = useState(false);
  useFocusTrap(box, true);
  useScrollLock(true);
  useEffect(() => {
    const onKey = (e: KeyboardEvent) => e.key === 'Escape' && onClose();
    window.addEventListener('keydown', onKey);
    return () => window.removeEventListener('keydown', onKey);
  }, [onClose]);

  const tool = (icon: IconName, title: string, fn: () => void) => (
    <button
      type="button"
      aria-label={title}
      title={title}
      onClick={fn}
      className="w-9 h-9 border-0 rounded-md bg-white/[.14] text-white cursor-pointer grid place-items-center hover:bg-white/25 focus-visible:outline-none focus-visible:shadow-focus"
    >
      <Icon name={icon} size={18} />
    </button>
  );

  return createPortal(
    <div
      ref={box}
      role="dialog"
      aria-modal="true"
      aria-label={t('admin-kyc.viewer.title', { name: label })}
      className="fixed inset-0 z-modal bg-[rgba(20,23,31,.9)] flex flex-col"
    >
      <div className="flex items-center gap-2 px-[18px] py-3 text-white">
        <Icon name="file-text" size={18} />
        <b className="text-sm font-semibold mr-auto truncate">
          {t('admin-kyc.viewer.title', { name: label })}
        </b>
        {tool('plus', t('admin-kyc.viewer.zoom_in'), () => setZoom((z) => Math.min(3, z + 0.25)))}
        {tool('minus', t('admin-kyc.viewer.zoom_out'), () =>
          setZoom((z) => Math.max(0.5, z - 0.25)),
        )}
        {tool('refresh-cw', t('admin-kyc.viewer.rotate'), () => setRot((r) => r + 90))}
        {tool('arrow-right', t('admin-kyc.viewer.flip'), () => setFlip((f) => !f))}
        {tool('x', t('common.action.close'), onClose)}
      </div>
      <div className="flex-1 overflow-auto grid place-items-center p-4">
        <img
          src={doc.url}
          alt={label}
          width={460}
          height={290}
          draggable={false}
          onContextMenu={(e) => e.preventDefault()}
          className="max-w-[min(460px,90vw)] h-auto rounded-[14px] shadow-xl select-none transition-transform duration-200"
          style={{ transform: `scale(${zoom}) rotate(${rot}deg) scaleX(${flip ? -1 : 1})` }}
        />
      </div>
      <div className="text-center text-xs text-white/60 pb-3" aria-live="polite">
        {Math.round(zoom * 100)}% · {((rot % 360) + 360) % 360}°
      </div>
    </div>,
    document.body,
  );
}
