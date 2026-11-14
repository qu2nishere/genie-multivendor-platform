// Duyệt sản phẩm — danh sách (prototype admin/ProductModeration.jsx) — plan G12.18: tìm theo sản phẩm /
// nhà bán, tab Chờ duyệt (AI nghi ngờ lên đầu) / Đã duyệt / Không duyệt / Cấu hình nhân viên (?tab=),
// giao lại người duyệt, bật / tắt nhận việc + Phân phối lại; "Kiểm duyệt" mở /product-moderation/:id.
import { useEffect, useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { errorMessage } from '@/shared/api/errors';
import { useT } from '@/shared/hooks/useT';
import { cn } from '@/shared/lib/cn';
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
import type { ModerationRow, ModerationTab, Moderator } from '../domain/moderation.types';
import { LABEL } from './moderationLabel';

type View = ModerationTab | 'CONFIG';
const VIEWS: { id: View; icon: IconName }[] = [
  { id: 'CONFIG', icon: 'user' },
  { id: 'PUBLISHED', icon: 'circle-check' },
  { id: 'REJECTED', icon: 'ban' },
  { id: 'PENDING', icon: 'clock' },
];

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
  const { t } = useT();
  const [params, setParams] = useSearchParams();
  const raw = params.get('tab') as View | null;
  const view: View = raw && VIEWS.some((v) => v.id === raw) ? raw : 'PENDING';
  const [q, setQ] = useState(params.get('q') ?? '');
  const [page, setPage] = useState(1);
  const tab: ModerationTab = view === 'CONFIG' ? 'PENDING' : view;
  const list = useModerationQueue(tab, q, page);
  const staff = useModerators().data ?? [];
  const counts = list.data?.counts;
  const go = (v: View) => {
    setPage(1);
    setParams({ tab: v }, { replace: true });
  };

  return (
    <div className="max-w-[1180px]">
      <div className="flex items-center gap-3 mb-4 flex-wrap">
        <div className="relative flex-1 min-w-[260px] max-w-[440px] max-sm:min-w-full">
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
        <div
          role="group"
          aria-label={t('admin-moderation.product.tabs_label')}
          className="flex gap-3 ml-auto flex-wrap max-sm:ml-0"
        >
          {VIEWS.map((v) => {
            const on = view === v.id;
            const n = v.id === 'CONFIG' ? 0 : (counts?.[v.id] ?? 0);
            return (
              <button
                key={v.id}
                type="button"
                aria-pressed={on}
                onClick={() => go(v.id)}
                className={cn(
                  'relative inline-flex items-center gap-[7px] h-10 px-3.5 rounded-md cursor-pointer border-[1.5px] border-solid border-blue-500 text-[13px] font-semibold focus-visible:outline-none focus-visible:shadow-focus',
                  on ? 'bg-blue-500 text-white' : 'bg-surface-card text-blue-600',
                )}
              >
                <Icon name={v.icon} size={16} />
                {t(`admin-moderation.product.views.${v.id}`)}
                {n > 0 && (
                  <span className="absolute -top-2 -right-2 min-w-5 h-5 px-1 rounded-[10px] bg-red-500 text-white text-[11px] font-bold leading-5 text-center">
                    {n}
                  </span>
                )}
              </button>
            );
          })}
        </div>
      </div>

      {view === 'CONFIG' ? (
        <StaffConfig />
      ) : list.isPending ? (
        <Skeleton lines={6} />
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
            title={t('admin-moderation.product.empty.title')}
            text={t('admin-moderation.product.empty.desc')}
          />
        </div>
      ) : (
        <div className="bg-surface-card rounded-lg shadow-sm overflow-hidden">
          <div className="dash-tablewrap">
            <table className="dash-table">
              <thead>
                <tr>
                  <th>{t('admin-moderation.product.col.product')}</th>
                  <th>{t('admin-moderation.product.col.vendor')}</th>
                  <th>{t('admin-moderation.product.col.status')}</th>
                  <th>{t('admin-moderation.product.col.assignee')}</th>
                  <th className="num">{t('admin-moderation.product.col.action')}</th>
                </tr>
              </thead>
              <tbody>
                {list.data.items.map((r) => {
                  const st = LABEL[moderationLabel(r)];
                  return (
                    <tr key={r.productId}>
                      <td>
                        <span className="flex items-center gap-2.5">
                          <img
                            src={r.imageUrl}
                            alt=""
                            width={44}
                            height={44}
                            className="w-11 h-11 rounded-sm object-cover flex-none"
                          />
                          <span>
                            <span className="block font-semibold text-strong">{r.name}</span>
                            <span className="text-xs text-muted">ID: #{r.productId}</span>
                          </span>
                        </span>
                      </td>
                      <td>{r.shop.name}</td>
                      <td>
                        <Badge variant={st.variant} icon={st.icon}>
                          {t(`admin-moderation.product.label.${moderationLabel(r)}`)}
                        </Badge>
                      </td>
                      <td className="w-[200px] min-w-[180px]">
                        <AssigneeCell r={r} staff={staff} />
                      </td>
                      <td className="num">
                        <Link
                          to={`/product-moderation/${r.productId}`}
                          className={buttonClass({ variant: 'outline', size: 'sm' })}
                        >
                          {t('admin-moderation.product.review')}
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
    </div>
  );
}
