// Duyệt sản phẩm — chi tiết (prototype ProductModeration.jsx Detail) — plan G12.18: dòng Gemini (quyết định,
// điểm rủi ro, vi phạm), lý do AI / lý do từ chối, ảnh (phóng to), hồ sơ pháp lý (Xem trực tiếp), thông
// tin cơ bản, mô tả, vận chuyển & kích thước, bảng SKU; thanh quyết định dính đáy: lý do + ghi chú,
// Từ chối / Duyệt lên sàn / Chờ duyệt lại (hỏi lại trước khi duyệt / từ chối). AI chỉ gắn cờ hoặc từ
// chối; người vẫn phải duyệt trước khi sản phẩm lên sàn.
import { useMemo, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { errorMessage } from '@/shared/api/errors';
import { useT } from '@/shared/hooks/useT';
import { cn } from '@/shared/lib/cn';
import { formatDateTime, formatNumber, formatVND } from '@/shared/lib/format';
import {
  Badge,
  Button,
  buttonClass,
  EmptyState,
  Icon,
  Modal,
  Select,
  Skeleton,
  toast,
} from '@/shared/ui';
import { useModerationDecision, useModerationDetail } from '../application/useModeration';
import {
  moderationActions,
  moderationLabel,
  PRODUCT_REJECT_REASONS,
  riskTone,
  type ProductRejectReason,
} from '../domain/moderation.rules';
import type { ModerationDetail } from '../domain/moderation.types';
import { LABEL } from './moderationLabel';

const card = 'bg-surface-card rounded-lg shadow-sm px-[18px] py-4';
const h3 = 'flex items-center gap-[7px] m-0 mb-3 text-sm font-bold text-strong';
const RISK_TEXT = {
  danger: 'text-red-600',
  warning: 'text-amber-600',
  success: 'text-mint-600',
} as const;

/** Mô tả do người bán soạn: chỉ hiện chữ (bỏ thẻ) để không chạy HTML lạ trong trang quản trị. */
function htmlToText(html: string): string {
  if (typeof DOMParser === 'undefined') return html.replace(/<[^>]+>/g, ' ');
  const doc = new DOMParser().parseFromString(
    html.replace(/<\/(p|div|li|h\d)>|<br\s*\/?>/gi, '\n'),
    'text/html',
  );
  return (doc.body.textContent ?? '').replace(/\n{3,}/g, '\n\n').trim();
}

function InfoGrid({ rows }: { rows: [string, string][] }) {
  return (
    <dl className="grid grid-cols-2 gap-3 m-0 max-sm:grid-cols-1">
      {rows.map(([k, v]) => (
        <div key={k}>
          <dt className="text-xs text-muted">{k}</dt>
          <dd className="m-0 mt-0.5 text-sm font-medium text-strong">{v}</dd>
        </div>
      ))}
    </dl>
  );
}

function Body({ p }: { p: ModerationDetail }) {
  const { t, locale } = useT();
  const navigate = useNavigate();
  const decide = useModerationDecision();
  const [reason, setReason] = useState<ProductRejectReason>('MISSING_SAFETY_CERT');
  const [note, setNote] = useState('');
  const [confirm, setConfirm] = useState<'approve' | 'reject' | null>(null);
  const [doc, setDoc] = useState<ModerationDetail['documents'][number] | null>(null);
  const [zoom, setZoom] = useState<string | null>(null);
  const label = moderationLabel(p);
  const st = LABEL[label];
  const acts = moderationActions(p.status);
  const reasonLabel = t(`admin-moderation.product.reasons.${reason}`);
  const rejectText = reasonLabel + (note.trim() ? `: ${note.trim()}` : '');
  const desc = useMemo(() => htmlToText(p.descriptionHtml), [p.descriptionHtml]);
  const aiRejected = p.status === 'REJECTED' && p.rejectedByAi;
  const reasonText =
    p.status === 'REJECTED' ? (p.decisionReason ?? p.ai?.reasoning) : p.ai?.reasoning;
  const d = p.dimensionsCm;

  const run = (kind: 'approve' | 'reject' | 'requeue') =>
    decide.mutate(
      kind === 'reject'
        ? { kind, id: p.productId, reason, note: note.trim() || null, label: reasonLabel }
        : { kind, id: p.productId },
      {
        onSuccess: () => {
          toast.success(t(`admin-moderation.product.done.${kind}`));
          setConfirm(null);
          navigate('/product-moderation');
        },
        onError: (e) => toast.error(errorMessage(e, t)),
      },
    );

  return (
    <div className="max-w-[1180px]">
      <Link
        to="/product-moderation"
        className="inline-flex items-center gap-1.5 text-sm text-muted no-underline hover:underline mb-3.5"
      >
        <Icon name="arrow-left" size={16} />
        {t('admin-moderation.product.back')}
      </Link>
      <div className="flex items-center gap-3 mb-4 flex-wrap">
        <div>
          <h2 className="m-0 text-xl font-bold text-strong">
            {t('admin-moderation.product.detail_title', { id: p.productId })}
          </h2>
          <div className="text-xs text-muted mt-1">
            {t('admin-moderation.product.meta', {
              at: formatDateTime(p.submittedAt, { locale }),
              shop: p.shop.name,
            })}
          </div>
        </div>
        <span className="ml-auto">
          <Badge variant={st.variant} icon={st.icon}>
            {t(`admin-moderation.product.label.${label}`)}
          </Badge>
        </span>
      </div>

      {p.ai && (
        <div className="flex items-center gap-2.5 px-4 py-3 mb-3 rounded-md bg-surface-card shadow-sm text-sm text-body flex-wrap">
          <Icon name="cpu" size={18} className="text-blue-600" />
          <b>Gemini:</b> <span className="font-mono">{p.ai.decision}</span>
          <span>
            · {t('admin-moderation.product.risk')}{' '}
            <b className={RISK_TEXT[riskTone(p.ai.riskScore)]}>{p.ai.riskScore}/100</b>
          </span>
          {p.ai.violations.length > 0 ? (
            p.ai.violations.map((v) => (
              <Badge key={v} variant="danger">
                {v}
              </Badge>
            ))
          ) : (
            <span className="text-muted">· {t('admin-moderation.product.no_violation')}</span>
          )}
        </div>
      )}
      {reasonText && (
        <div
          className={cn(
            'flex gap-3.5 p-4 mb-4 rounded-md border border-solid',
            p.status === 'REJECTED' ? 'bg-red-50 border-red-200' : 'bg-amber-50 border-gold-400',
          )}
        >
          <Icon
            name={aiRejected ? 'cpu' : 'triangle-alert'}
            size={22}
            className={cn('flex-none', p.status === 'REJECTED' ? 'text-red-600' : 'text-amber-600')}
          />
          <div>
            <div className="text-sm font-bold text-strong">
              {t(
                aiRejected
                  ? 'admin-moderation.product.ai_rejected'
                  : p.status === 'REJECTED'
                    ? 'admin-moderation.product.rejected_reason'
                    : 'admin-moderation.product.ai_flagged',
              )}
            </div>
            <p className="m-0 mt-1 text-sm text-body">{reasonText}</p>
          </div>
        </div>
      )}

      <div className="grid grid-cols-[2fr_3fr] gap-4 items-start max-md:grid-cols-1">
        <div className="flex flex-col gap-4">
          <section className={card}>
            <h3 className={h3}>
              <Icon name="image" size={16} />
              {t('admin-moderation.product.images')}
            </h3>
            {p.images.map((src) => (
              <button
                key={src}
                type="button"
                onClick={() => setZoom(src)}
                className="block w-full border-0 p-0 bg-transparent cursor-zoom-in rounded-md focus-visible:outline-none focus-visible:shadow-focus"
                aria-label={t('admin-moderation.product.zoom')}
              >
                <img
                  src={src}
                  alt={p.name}
                  width={400}
                  height={400}
                  className="w-full aspect-square object-cover rounded-md"
                />
              </button>
            ))}
          </section>
          <section className={card}>
            <h3 className={h3}>
              <Icon name="file-text" size={16} />
              {t('admin-moderation.product.docs')}
            </h3>
            {p.documents.length === 0 && (
              <div className="text-sm text-muted">{t('admin-moderation.product.no_docs')}</div>
            )}
            <ul className="list-none m-0 p-0">
              {p.documents.map((x) => (
                <li
                  key={x.id}
                  className="flex items-center gap-2.5 py-2.5 border-0 border-t border-solid border-line-subtle"
                >
                  <span className="w-9 h-9 rounded-sm bg-blue-50 text-blue-600 grid place-items-center flex-none">
                    <Icon name="file-text" size={18} />
                  </span>
                  <span className="flex-1 min-w-0 text-sm text-body truncate" title={x.fileName}>
                    {x.fileName}
                  </span>
                  <Button size="sm" variant="ghost" iconLeft="eye" onClick={() => setDoc(x)}>
                    {t('admin-moderation.product.view_doc')}
                  </Button>
                </li>
              ))}
            </ul>
          </section>
        </div>

        <div className="flex flex-col gap-4 min-w-0">
          <section className={card}>
            <h3 className="m-0 mb-3.5 text-lg font-semibold text-strong">{p.name}</h3>
            <InfoGrid
              rows={[
                [t('admin-moderation.product.info.category'), p.categoryPath],
                [t('admin-moderation.product.info.brand'), p.brand],
                [
                  t('admin-moderation.product.info.condition'),
                  t(`admin-moderation.product.condition.${p.condition}`),
                ],
                [t('admin-moderation.product.info.origin'), p.origin],
              ]}
            />
            <h4 className={cn(h3, 'mt-[18px]')}>
              <Icon name="file-text" size={16} />
              {t('admin-moderation.product.desc')}
            </h4>
            <p className="m-0 text-sm text-body leading-relaxed whitespace-pre-line">{desc}</p>
          </section>
          <section className={card}>
            <h3 className={h3}>
              <Icon name="package" size={16} />
              {t('admin-moderation.product.shipping')}
            </h3>
            <InfoGrid
              rows={[
                [
                  t('admin-moderation.product.info.weight'),
                  `${formatNumber(p.weightGrams, { locale })} g`,
                ],
                [
                  t('admin-moderation.product.info.size'),
                  `${d.length} × ${d.width} × ${d.height} cm`,
                ],
                [t('admin-moderation.product.info.ships_from'), p.shipsFrom],
                [
                  t('admin-moderation.product.info.payer'),
                  t(`admin-moderation.product.payer.${p.shippingPayer}`),
                ],
              ]}
            />
          </section>
          <section className={card}>
            <h3 className={h3}>
              <Icon name="layers" size={16} />
              {t('admin-moderation.product.skus')}
            </h3>
            <div className="overflow-x-auto">
              <table className="w-full border-collapse text-[13px] tabular-nums">
                <thead>
                  <tr className="text-muted text-left">
                    <th className="px-2 py-[9px] whitespace-nowrap font-semibold">
                      {t('admin-moderation.product.sku.code')}
                    </th>
                    <th className="px-2 py-[9px] whitespace-nowrap font-semibold">
                      {t('admin-moderation.product.sku.name')}
                    </th>
                    <th className="px-2 py-[9px] whitespace-nowrap font-semibold text-right">
                      {t('admin-moderation.product.sku.price')}
                    </th>
                    <th className="px-2 py-[9px] whitespace-nowrap font-semibold text-right">
                      {t('admin-moderation.product.sku.stock')}
                    </th>
                  </tr>
                </thead>
                <tbody>
                  {p.variants.map((s) => (
                    <tr
                      key={s.sku}
                      className="border-0 border-t border-solid border-line-subtle text-body"
                    >
                      <td className="px-2 py-[9px] font-mono">{s.sku}</td>
                      <td className="px-2 py-[9px]">{s.name}</td>
                      <td className="px-2 py-[9px] text-right">{formatVND(s.price, { locale })}</td>
                      <td className="px-2 py-[9px] text-right">
                        {formatNumber(s.stock, { locale })}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </section>
        </div>
      </div>

      <div className="sticky bottom-0 flex items-end gap-2.5 mt-4 px-[18px] py-3.5 bg-surface-card rounded-lg shadow-lg flex-wrap z-sticky">
        <div className="flex-1 min-w-[280px]">
          <span className="block text-sm font-semibold text-strong mb-1.5" id="reject-reason-label">
            {t('admin-moderation.product.reject_reason')}
          </span>
          <div className="flex gap-2 max-sm:flex-col">
            <Select<ProductRejectReason>
              className="w-[240px] flex-none max-sm:w-full"
              aria-label={t('admin-moderation.product.reject_reason')}
              value={reason}
              onChange={setReason}
              options={PRODUCT_REJECT_REASONS.map((r) => ({
                value: r,
                label: t(`admin-moderation.product.reasons.${r}`),
              }))}
            />
            <input
              value={note}
              maxLength={500}
              onChange={(e) => setNote(e.target.value)}
              aria-label={t('admin-moderation.product.note')}
              placeholder={t('admin-moderation.product.note_placeholder')}
              className="flex-1 min-w-0 h-11 max-sm:flex-none max-sm:w-full px-3 border-[1.5px] border-solid border-line rounded-md text-sm bg-surface-card text-body outline-none focus:border-primary"
            />
          </div>
        </div>
        {acts.includes('reject') && (
          <Button variant="danger" iconLeft="ban" onClick={() => setConfirm('reject')}>
            {t('admin-moderation.product.reject')}
          </Button>
        )}
        {acts.includes('approve') && (
          <Button iconLeft="circle-check" onClick={() => setConfirm('approve')}>
            {t('admin-moderation.product.approve')}
          </Button>
        )}
        {acts.includes('requeue') && (
          <Button
            variant="outline"
            iconLeft="refresh-cw"
            loading={decide.isPending && confirm === null}
            onClick={() => run('requeue')}
          >
            {t('admin-moderation.product.requeue')}
          </Button>
        )}
      </div>

      <Modal
        open={!!confirm}
        onClose={() => setConfirm(null)}
        width={420}
        title={t(`admin-moderation.product.confirm.${confirm ?? 'approve'}.title`)}
        footer={
          <>
            <Button variant="ghost" onClick={() => setConfirm(null)}>
              {t('common.action.cancel')}
            </Button>
            <Button
              variant={confirm === 'reject' ? 'danger' : 'primary'}
              loading={decide.isPending}
              onClick={() => confirm && run(confirm)}
            >
              {t(`admin-moderation.product.${confirm ?? 'approve'}`)}
            </Button>
          </>
        }
      >
        <p className="m-0 text-base text-body leading-normal">
          {t(`admin-moderation.product.confirm.${confirm ?? 'approve'}.desc`)}
        </p>
        {confirm === 'reject' && (
          <p className="m-0 mt-2 text-sm text-muted">
            {t('admin-moderation.product.confirm.reason', { reason: rejectText })}
          </p>
        )}
      </Modal>
      <Modal
        open={!!doc}
        onClose={() => setDoc(null)}
        width={640}
        title={doc ? t('admin-moderation.product.doc_title', { name: doc.fileName }) : ''}
        footer={<Button onClick={() => setDoc(null)}>{t('common.action.close')}</Button>}
      >
        {doc && (
          <img
            src={doc.url}
            alt={doc.fileName}
            width={620}
            height={820}
            className="block w-full h-auto max-h-[70vh] object-contain bg-surface-sunken rounded-md"
          />
        )}
      </Modal>
      <Modal
        open={!!zoom}
        onClose={() => setZoom(null)}
        width={760}
        aria-label={t('admin-moderation.product.zoom')}
      >
        {zoom && (
          <img
            src={zoom}
            alt={p.name}
            width={720}
            height={720}
            className="block w-full h-auto max-h-[78vh] object-contain rounded-md"
          />
        )}
      </Modal>
    </div>
  );
}

export default function ProductModerationDetail() {
  const { t } = useT();
  const { id } = useParams();
  const num = Number(id);
  const q = useModerationDetail(Number.isInteger(num) && num > 0 ? num : null);
  if (!Number.isInteger(num) || num <= 0 || q.isError)
    return (
      <EmptyState
        icon="package"
        title={t('admin-moderation.product.not_found')}
        text={q.isError ? errorMessage(q.error, t) : undefined}
        action={
          <Link to="/product-moderation" className={buttonClass({})}>
            {t('admin-moderation.product.back')}
          </Link>
        }
      />
    );
  if (!q.data) return <Skeleton lines={12} />;
  return <Body p={q.data} />;
}
