// Chụp selfie bằng camera (prototype Onboarding.jsx CameraModal) — plan G11.38: `getUserMedia`, hướng dẫn 3
// bước kiểm tra người thật (nháy mắt, quay trái, quay phải — AI Worker chấm liveness ở phía server), chụp ra
// JPEG; trình duyệt chặn / không có camera → báo lỗi và gợi ý tải ảnh lên.
import { useEffect, useRef, useState } from 'react';
import { useT } from '@/shared/hooks/useT';
import { Button, Icon, Modal } from '@/shared/ui';
import { LIVENESS } from '../domain/kyc.rules';

export function CameraModal({
  onClose,
  onShot,
}: {
  onClose: () => void;
  onShot: (f: File) => void;
}) {
  const { t } = useT();
  const video = useRef<HTMLVideoElement>(null);
  const [err, setErr] = useState<'denied' | 'none' | null>(null);
  const [ready, setReady] = useState(false);
  const [step, setStep] = useState(0);

  useEffect(() => {
    let stream: MediaStream | null = null;
    let alive = true;
    if (!navigator.mediaDevices?.getUserMedia) {
      setErr('none');
      return undefined;
    }
    navigator.mediaDevices
      .getUserMedia({ video: { facingMode: 'user' }, audio: false })
      .then((s) => {
        if (!alive) return s.getTracks().forEach((x) => x.stop());
        stream = s;
        if (video.current) video.current.srcObject = s;
        setReady(true);
      })
      .catch((e: unknown) =>
        setErr(e instanceof DOMException && e.name === 'NotFoundError' ? 'none' : 'denied'),
      );
    return () => {
      alive = false;
      stream?.getTracks().forEach((x) => x.stop());
    };
  }, []);

  const capture = () => {
    const v = video.current;
    if (!v) return;
    const c = document.createElement('canvas');
    c.width = v.videoWidth;
    c.height = v.videoHeight;
    c.getContext('2d')?.drawImage(v, 0, 0);
    c.toBlob(
      (b) => b && onShot(new File([b], 'selfie.jpg', { type: 'image/jpeg' })),
      'image/jpeg',
      0.92,
    );
  };

  const done = step >= LIVENESS.length;
  return (
    <Modal
      open
      onClose={onClose}
      width={520}
      title={t('seller-onboarding.kyc.camera.title')}
      footer={
        <>
          <Button variant="ghost" onClick={onClose}>
            {t('common.action.cancel')}
          </Button>
          {done ? (
            <Button iconLeft="camera" disabled={!!err || !ready} onClick={capture}>
              {t('seller-onboarding.kyc.camera.shoot')}
            </Button>
          ) : (
            <Button
              iconLeft="check"
              disabled={!!err || !ready}
              onClick={() => setStep((s) => s + 1)}
            >
              {t('seller-onboarding.kyc.camera.did')}
            </Button>
          )}
        </>
      }
    >
      {err ? (
        <p className="m-0 flex gap-2 items-start text-red-600 text-sm" role="alert">
          <Icon name="circle-alert" size={16} className="flex-none mt-0.5" />
          {t(`seller-onboarding.kyc.camera.err_${err}`)}
        </p>
      ) : (
        <div>
          <video
            ref={video}
            autoPlay
            playsInline
            muted
            aria-label={t('seller-onboarding.kyc.camera.preview')}
            className="w-full rounded-md bg-ink-900 aspect-[4/3] object-cover -scale-x-100"
          />
          <p
            className="m-0 mt-2.5 font-semibold text-sm text-strong"
            role="status"
            aria-live="polite"
          >
            {done
              ? t('seller-onboarding.kyc.camera.ready')
              : t('seller-onboarding.kyc.camera.step', {
                  n: step + 1,
                  total: LIVENESS.length,
                  task: t(`seller-onboarding.kyc.camera.tasks.${LIVENESS[step]}`),
                })}
          </p>
        </div>
      )}
    </Modal>
  );
}
