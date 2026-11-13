// "Đánh giá sản phẩm" (prototype ReviewModal.jsx) — plan G10.42: lần lượt từng sản phẩm trong đơn,
// 1–5 sao, nhận xét, ≤ 5 ảnh (+50 Điểm), 1 video ≤ 30 MB, ẩn tên; "Gửi và đánh giá sản phẩm tiếp theo".
import { useEffect, useState } from 'react';
import { errorMessage } from '@/shared/api/errors';
import type { UploadedFile } from '@/shared/api/upload';
import { useT } from '@/shared/hooks/useT';
import { Button, MediaPicker, Modal, Radio, RatingInput, Textarea, toast } from '@/shared/ui';
import { useCreateReview } from '../application/useReviews';

export interface ReviewOrder {
  id: number;
  items: { id: number; title: string; variantLabel: string }[];
}

export interface ReviewModalProps {
  open: boolean;
  order: ReviewOrder;
  /** Dòng hàng đã đánh giá — bỏ qua. */
  reviewedIds?: number[];
  onClose: () => void;
}

const MAX_CONTENT = 1000;

export function ReviewModal({ open, order, reviewedIds = [], onClose }: ReviewModalProps) {
  const { t } = useT();
  const items = order.items.filter((it) => !reviewedIds.includes(it.id));
  const create = useCreateReview();
  const [idx, setIdx] = useState(0);
  const [stars, setStars] = useState(5);
  const [text, setText] = useState('');
  const [imgs, setImgs] = useState<UploadedFile[]>([]);
  const [video, setVideo] = useState<UploadedFile[]>([]);
  const [anon, setAnon] = useState(false);
  const [earned, setEarned] = useState(0);

  const clear = () => {
    setStars(5);
    setText('');
    setImgs([]);
    setVideo([]);
    setAnon(false);
  };
  useEffect(() => {
    if (open) {
      clear();
      setIdx(0);
      setEarned(0);
    }
  }, [open]);

  const item = items[idx];
  if (!item) return null;
  const last = idx === items.length - 1;

  const submit = () =>
    create.mutate(
      {
        orderId: order.id,
        orderItemId: item.id,
        rating: stars,
        content: text.trim(),
        imageKeys: imgs.map((f) => f.objectKey),
        videoKey: video[0]?.objectKey ?? null,
        anonymous: anon,
      },
      {
        onSuccess: (r) => {
          const total = earned + r.coinsAwarded;
          if (last) {
            toast.success(
              total > 0 ? t('reviews.form.done_points', { points: total }) : t('reviews.form.done'),
            );
            onClose();
            return;
          }
          setEarned(total);
          setIdx(idx + 1);
          clear();
        },
        onError: (e) => toast.error(errorMessage(e, t)),
      },
    );

  return (
    <Modal open={open} onClose={onClose} title={t('reviews.form.title')} width={540}>
      <div className="flex flex-col gap-3.5">
        <p className="m-0 text-base text-strong">
          <b>{item.title}</b>
          <span className="text-muted ml-2">
            ({t('cart.line.variant', { variant: item.variantLabel })})
            {items.length > 1 &&
              ` · ${t('reviews.form.step', { n: idx + 1, count: items.length })}`}
          </span>
        </p>
        <div className="flex justify-center">
          <RatingInput
            value={stars}
            onChange={setStars}
            size={38}
            aria-label={t('reviews.form.stars')}
          />
        </div>
        <Textarea
          label={t('reviews.form.content')}
          placeholder={t('reviews.form.placeholder')}
          value={text}
          maxLength={MAX_CONTENT}
          rows={4}
          onChange={(e) => setText(e.target.value)}
        />
        <MediaPicker
          label={t('reviews.form.images', { n: imgs.length })}
          hint={t('reviews.form.images_hint')}
          value={imgs}
          onChange={setImgs}
          accept="image/jpeg,image/png"
          max={5}
          purposeOf={() => 'REVIEW_IMAGE'}
        />
        <MediaPicker
          label={t('reviews.form.video')}
          hint={t('reviews.form.video_hint')}
          value={video}
          onChange={setVideo}
          accept="video/mp4"
          max={1}
          purposeOf={() => 'REVIEW_VIDEO'}
        />
        <div
          className="flex gap-[18px] flex-wrap"
          role="radiogroup"
          aria-label={t('reviews.form.name')}
        >
          <Radio name="review-name" checked={!anon} onChange={() => setAnon(false)}>
            <span className="text-sm text-body">{t('reviews.form.public')}</span>
          </Radio>
          <Radio name="review-name" checked={anon} onChange={() => setAnon(true)}>
            <span className="text-sm text-body">{t('reviews.form.anonymous')}</span>
          </Radio>
        </div>
        <Button size="lg" block loading={create.isPending} onClick={submit}>
          {t(last ? 'reviews.form.submit' : 'reviews.form.submit_next')}
        </Button>
      </div>
    </Modal>
  );
}
