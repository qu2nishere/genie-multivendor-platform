// Duyệt sản phẩm — danh sách (prototype admin/ProductModeration.jsx) — plan G12.18: tìm theo sản phẩm /
// nhà bán, tab Chờ duyệt (AI nghi ngờ lên đầu) / Đã duyệt / Không duyệt / Cấu hình nhân viên (?tab=),
// giao lại người duyệt, bật / tắt nhận việc + Phân phối lại; "Kiểm duyệt" mở /product-moderation/:id.
import { useEffect, useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { errorMessage } from '@/shared/api/errors';
import { useT } from '@/shared/hooks/useT';
import { cn } from '@/shared/lib/cn';
import { formatDateTime } from '@/shared/lib/format';
import {
  Badge,
  Button,
  buttonClass,
  ConfirmDialog,
  EmptyState,
  Icon,
  Select,
  Skeleton,
  Switch,
  TablePager,
  toast,
  type IconName,
} from '@/shared/ui';
import {
  useAssignModerator,
  useModerationQueue,
  useModerators,
  useRedistribute,
  useSaveModerators,
} from '../application/useModeration';
import { moderationLabel } from '../domain/moderation.rules';
import {
  AI_CHECKS,
  type AiCheck,
  type ModerationRow,
  type ModerationTab,
  type Moderator,
} from '../domain/moderation.types';
import AiScreeningCard from './AiScreeningCard';
import { AI_LABEL, LABEL } from './moderationLabel';

type View = ModerationTab | 'CONFIG';
const VIEWS: { id: View; icon: IconName }[] = [
  { id: 'PENDING', icon: 'clock' },
  { id: 'PUBLISHED', icon: 'circle-check' },
  { id: 'REJECTED', icon: 'ban' },
  { id: 'CONFIG', icon: 'settings' },
];
const TAB_VIEWS = VIEWS.filter((v) => v.id !== 'CONFIG');

/** Ảnh nhỏ của sản phẩm — ảnh lỗi / thiếu thì hiện khung xám có icon thay cho ảnh vỡ. */
function ProductThumb({ src }: { src: string | null | undefined }) {
  const [broken, setBroken] = useState(false);
  if (!src || broken)
    return (
      <span className="w-12 h-12 rounded-md bg-surface-sunken grid place-items-center flex-none text-subtle">
        <Icon name="image" size={18} />
      </span>
    );
  return (
    <img
      src={src}
      alt=""
      width={48}
      height={48}
      loading="lazy"
      onError={() => setBroken(true)}
      className="w-12 h-12 rounded-md object-cover flex-none bg-surface-sunken"
    />
  );
}

function StaffConfig() {
  const { t } = useT();
  const q = useModerators();
  const queue = useModerationQueue('PENDING', '', 1);
  const save = useSaveModerators();
  const redo = useRedistribute();
  const [staff, setStaff] = useState<Moderator[]>([]);
  const [asking, setAsking] = useState(false);
  useEffect(() => {
    if (q.data) setStaff(q.data);
  }, [q.data]);
  if (!q.data) return <Skeleton lines={4} />;
  const waiting = (id: number) =>
    (queue.data?.items ?? []).filter((r) => r.assignee?.id === id).length;
  return (
    <section
      className="bg-surface-card rounded-lg shadow-sm overflow-hidden"
      aria-labelledby="mod-staff"
    >
      <div className="flex items-center gap-2 px-5 py-3.5 border-0 border-b border-solid border-line-subtle flex-wrap">
        <h2 id="mod-staff" className="m-0 text-base font-bold text-strong">
          {t('admin-moderation.product.staff.title')}
        </h2>
        <Button
          size="sm"
          variant="outline"
          iconLeft="refresh-cw"
          className="ml-auto"
          onClick={() => setAsking(true)}
        >
          {t('admin-moderation.product.staff.redistribute')}
        </Button>
        <Button
          size="sm"
          iconLeft="check"
          loading={save.isPending}
          onClick={() =>
            save.mutate(staff, {
              onSuccess: () => toast.success(t('admin-moderation.product.staff.saved')),
              onError: (e) => toast.error(errorMessage(e, t)),
            })
          }
        >
          {t('admin-moderation.product.staff.save')}
        </Button>
      </div>
      <ul className="list-none m-0 p-0">
        {staff.map((s) => (
          <li
            key={s.staffId}
            className="flex items-center gap-3 px-5 py-3 border-0 border-t border-solid border-line-subtle first:border-t-0"
          >
            <div className="flex-1 min-w-0">
              <div className="text-sm font-semibold text-strong">{s.name}</div>
              <div className="text-xs text-muted">
                {t('admin-moderation.product.staff.waiting', { count: waiting(s.staffId) })}
              </div>
            </div>
            <Switch
              checked={s.accepting}
              aria-label={t('admin-moderation.product.staff.toggle', { name: s.name })}
              onChange={() =>
                setStaff((xs) =>
                  xs.map((x) => (x.staffId === s.staffId ? { ...x, accepting: !x.accepting } : x)),
                )
              }
            />
            <span
              className={cn(
                'w-[110px] text-[13px] font-medium',
                s.accepting ? 'text-blue-600' : 'text-muted',
              )}
            >
              {t(
                s.accepting
                  ? 'admin-moderation.product.staff.on'
                  : 'admin-moderation.product.staff.off',
              )}
            </span>
          </li>
        ))}
      </ul>
      <ConfirmDialog
        open={asking}
        onCancel={() => setAsking(false)}
        title={t('admin-moderation.product.staff.confirm_title')}
        message={
          <>
            {t('admin-moderation.product.staff.confirm_desc')}
            <span className="block mt-2 text-sm text-muted">
              {t('admin-moderation.product.staff.confirm_note')}
            </span>
          </>
        }
        confirmLabel={t('admin-moderation.product.staff.confirm')}
        loading={redo.isPending}
        onConfirm={() =>
          redo.mutate(undefined, {
            onSuccess: (n) => {
              toast.success(t('admin-moderation.product.staff.redistributed', { count: n }));
              setAsking(false);
            },
            onError: (e) => toast.error(errorMessage(e, t)),
          })
        }
      />
    </section>
  );
}

function AssigneeCell({ r, staff }: { r: ModerationRow; staff: Moderator[] }) {
  const { t } = useT();
  const assign = useAssignModerator();
  if (r.status === 'REJECTED' && r.rejectedByAi)
    return (
      <span className="inline-flex items-center gap-1.5 text-muted">
        <Icon name="cpu" size={15} />
        {t('admin-moderation.product.ai_system')}
      </span>
    );
  // Đã quyết (đang bán / không duyệt): chỉ xem người đã duyệt, không giao lại
  if (r.status === 'PUBLISHED' || r.status === 'REJECTED')
    return (
      <span className="inline-flex items-center gap-1.5 text-body">
        <Icon name="user-check" size={15} className="text-subtle" />
        {r.assignee?.name ?? (
          <span className="text-muted">{t('admin-moderation.product.assignee_none')}</span>
        )}
      </span>
    );
  return (
    <Select<number>
      aria-label={t('admin-moderation.product.assignee_for', { name: r.name })}
      value={r.assignee?.id ?? 0}
      onChange={(v) => {
        if (!v) return;
        assign.mutate(
          { id: r.productId, staffId: v },
          {
            onSuccess: () => toast.success(t('admin-moderation.product.assigned')),
            onError: (e) => toast.error(errorMessage(e, t)),
          },
        );
      }}
      options={[
        { value: 0, label: t('admin-moderation.product.unassigned'), disabled: true },
        ...staff.map((s) => ({ value: s.staffId, label: s.name })),
      ]}
    />
  );
}

export default function ProductModerationPage() {
  const { t, locale } = useT();
  const [params, setParams] = useSearchParams();
  const raw = params.get('tab') as View | null;
  const view: View = raw && VIEWS.some((v) => v.id === raw) ? raw : 'PENDING';
  const rawAi = params.get('ai');
  const ai: AiCheck | undefined =
    rawAi && AI_CHECKS.includes(rawAi as AiCheck) ? (rawAi as AiCheck) : undefined;
  const [q, setQ] = useState(params.get('q') ?? '');
  const [page, setPage] = useState(1);
  const tab: ModerationTab = view === 'CONFIG' ? 'PENDING' : view;
  const list = useModerationQueue(tab, q, page, view === 'CONFIG' ? undefined : ai);
  const staff = useModerators().data ?? [];
  const counts = list.data?.counts;
  const go = (v: View) => {
    setPage(1);
    const next: Record<string, string> = { tab: v };
    if (v !== 'CONFIG' && ai) next.ai = ai;
    if (q.trim()) next.q = q.trim();
    setParams(next, { replace: true });
  };
  const setAiFilter = (val: string) => {
    setPage(1);
    const next: Record<string, string> = { tab: view };
    if (val) next.ai = val;
    if (q.trim()) next.q = q.trim();
    setParams(next, { replace: true });
  };
  // Sản phẩm đã quyết mà chưa từng qua AI (dữ liệu cũ) — nhãn "Chưa chấm AI" chỉ gây nhiễu, chỉ hiện ở tab Chờ duyệt
  const showAiBadge = (r: ModerationRow) =>
    !(r.status === 'FLAGGED' && r.aiCheck === 'SUSPECTED') &&
    !(view !== 'PENDING' && r.aiCheck === 'NOT_SCREENED');

  return (
    <div className="w-full flex flex-col gap-4">
      {/* Hàng 1: tab trạng thái + nút Cấu hình */}
      <div className="flex items-end justify-between gap-3 flex-wrap border-0 border-b border-solid border-line-subtle">
        <div
          role="tablist"
          aria-label={t('admin-moderation.product.tabs_label')}
          className="flex gap-1 -mb-px overflow-x-auto"
        >
          {TAB_VIEWS.map((v) => {
            const on = view === v.id;
            const n = counts?.[v.id as ModerationTab] ?? 0;
            return (
              <button
                key={v.id}
                type="button"
                role="tab"
                aria-selected={on}
                onClick={() => go(v.id)}
                className={cn(
                  'inline-flex items-center gap-2 h-11 px-4 bg-transparent cursor-pointer whitespace-nowrap',
                  'border-0 border-b-2 border-solid text-sm font-semibold',
                  'focus-visible:outline-none focus-visible:shadow-focus rounded-t-md',
                  on
                    ? 'border-primary text-primary'
                    : 'border-transparent text-muted hover:text-strong',
                )}
              >
                <Icon name={v.icon} size={16} />
                {t(`admin-moderation.product.views.${v.id}`)}
                {n > 0 && (
                  <span
                    className={cn(
                      'min-w-5 h-5 px-1.5 rounded-full text-[11px] font-bold leading-5 text-center',
                      v.id === 'PENDING' ? 'bg-red-500 text-white' : 'bg-surface-sunken text-muted',
                    )}
                  >
                    {n}
                  </span>
                )}
              </button>
            );
          })}
        </div>
        <button
          type="button"
          aria-pressed={view === 'CONFIG'}
          onClick={() => go('CONFIG')}
          className={cn(
            buttonClass({ variant: view === 'CONFIG' ? 'primary' : 'outline', size: 'sm' }),
            'mb-2',
          )}
        >
          <Icon name="settings" size={15} />
          {t('admin-moderation.product.views.CONFIG')}
        </button>
      </div>

      {view === 'CONFIG' ? (
        <div className="flex flex-col gap-4">
          <AiScreeningCard />
          <StaffConfig />
        </div>
      ) : (
        <>
          {/* Hàng 2: tìm kiếm + lọc AI + tổng số */}
          <div className="flex items-center gap-3 flex-wrap">
            <div className="relative flex-1 min-w-[260px] max-w-[460px] max-sm:min-w-full">
              <Icon
                name="search"
                size={16}
                className="absolute left-3 top-3 text-subtle pointer-events-none"
              />
              <input
                value={q}
                onChange={(e) => {
                  setQ(e.target.value);
                  setPage(1);
                }}
                aria-label={t('admin-moderation.product.search')}
                placeholder={t('admin-moderation.product.search_placeholder')}
                className="w-full h-10 pl-9 pr-3 border-[1.5px] border-solid border-line rounded-md text-sm bg-surface-card text-body outline-none focus:border-primary"
              />
            </div>
            <Select<string>
              className="w-[200px] max-sm:w-full"
              aria-label={t('admin-moderation.product.ai_filter.label')}
              value={ai ?? ''}
              onChange={setAiFilter}
              options={[
                { value: '', label: t('admin-moderation.product.ai_filter.all') },
                ...AI_CHECKS.map((k) => ({
                  value: k,
                  label: t(`admin-moderation.product.ai_check.${k}`),
                })),
              ]}
            />
            {list.data && (
              <span className="ml-auto text-sm text-muted max-sm:ml-0">
                {t('admin-moderation.product.result_count', { count: list.data.total_items })}
              </span>
            )}
          </div>

          {list.isPending ? (
            <div className="bg-surface-card rounded-lg shadow-sm p-5">
              <Skeleton lines={6} />
            </div>
          ) : list.isError ? (
            <EmptyState
              icon="triangle-alert"
              title={t('common.state.error')}
              text={errorMessage(list.error, t)}
            />
          ) : list.data.items.length === 0 ? (
            <div className="bg-surface-card rounded-lg shadow-sm">
              <EmptyState
                icon="package"
                title={t(
                  ai
                    ? 'admin-moderation.product.ai_filter.empty'
                    : 'admin-moderation.product.empty.title',
                )}
                text={ai ? undefined : t('admin-moderation.product.empty.desc')}
              />
            </div>
          ) : (
            <div className="bg-surface-card rounded-lg shadow-sm overflow-hidden">
              <div className="dash-tablewrap">
                <table className="dash-table">
                  <thead>
                    <tr>
                      <th>{t('admin-moderation.product.col.product')}</th>
                      <th className="w-[190px]">{t('admin-moderation.product.col.status')}</th>
                      <th className="w-[210px]">{t('admin-moderation.product.col.assignee')}</th>
                      <th className="num w-[120px]">{t('admin-moderation.product.col.action')}</th>
                    </tr>
                  </thead>
                  <tbody>
                    {list.data.items.map((r) => {
                      const st = LABEL[moderationLabel(r)];
                      const aiBadge = AI_LABEL[r.aiCheck];
                      return (
                        <tr key={r.productId}>
                          <td className="!whitespace-normal">
                            <span className="flex items-center gap-3 min-w-[260px]">
                              <ProductThumb src={r.imageUrl} />
                              <span className="min-w-0">
                                <Link
                                  to={`/product-moderation/${r.productId}`}
                                  className="block font-semibold text-strong no-underline hover:text-primary line-clamp-2"
                                >
                                  {r.name}
                                </Link>
                                <span className="flex items-center gap-x-3 gap-y-0.5 flex-wrap mt-0.5 text-xs text-muted">
                                  <span className="inline-flex items-center gap-1">
                                    <Icon name="store" size={12} />
                                    {r.shop.name}
                                  </span>
                                  <span>#{r.productId}</span>
                                  {r.submittedAt && (
                                    <span>
                                      {t('admin-moderation.product.submitted_at', {
                                        time: formatDateTime(r.submittedAt, { locale }),
                                      })}
                                    </span>
                                  )}
                                </span>
                              </span>
                            </span>
                          </td>
                          <td>
                            <span className="flex flex-col items-start gap-1">
                              <Badge variant={st.variant} icon={st.icon}>
                                {t(`admin-moderation.product.label.${moderationLabel(r)}`)}
                              </Badge>
                              {showAiBadge(r) && (
                                <Badge variant={aiBadge.variant} icon={aiBadge.icon}>
                                  {t(`admin-moderation.product.ai_check.${r.aiCheck}`)}
                                </Badge>
                              )}
                            </span>
                          </td>
                          <td>
                            <AssigneeCell r={r} staff={staff} />
                          </td>
                          <td className="num">
                            <Link
                              to={`/product-moderation/${r.productId}`}
                              className={buttonClass({
                                variant: view === 'PENDING' ? 'primary' : 'outline',
                                size: 'sm',
                              })}
                            >
                              {t(
                                view === 'PENDING'
                                  ? 'admin-moderation.product.review'
                                  : 'admin-moderation.product.view',
                              )}
                            </Link>
                          </td>
                        </tr>
                      );
                    })}
                  </tbody>
                </table>
              </div>
              {list.data.total_pages > 1 && (
                <TablePager
                  page={list.data.page}
                  size={list.data.size}
                  totalItems={list.data.total_items}
                  totalPages={list.data.total_pages}
                  onChange={setPage}
                  unit={t('admin-moderation.product.unit')}
                />
              )}
            </div>
          )}
        </>
      )}
    </div>
  );
}
