// Nhân viên & phân quyền (11.18), người dùng (11.13), nhật ký (11.10), sửa tài khoản (11.1 PATCH
// /admin/me). Mock báo lỗi như server: email đã dùng (GNE-ADM-4091), sai đuôi (GNE-ADM-4221), xóa chính
// mình (GNE-ADM-4092), cấp MANAGE_RBAC cho Staff (GNE-SEC-4031).
import { api, getPage, request } from '@/shared/api/http';
import { mockDelay, mockFail, paginateCursor, USE_MOCK } from '@/shared/api/mock';
import {
  isPermission,
  segregationWarning,
  type PermissionCode,
  type StaffTitle,
} from '@/shared/auth/permissions';
import type { CursorPage } from '@/shared/api/types';
import {
  inUserTab,
  matchesStaff,
  matchesUser,
  STAFF_EMAIL,
  suspendUntil,
  USER_TABS,
  type Duration,
  type SuspendReason,
} from '../domain/system.rules';
import type {
  AuditCategory,
  AuditEntry,
  InviteInput,
  PlatformUser,
  StaffMember,
  UserCounts,
  UserTab,
} from '../domain/system.types';
import type { AuditLogDto, PlatformUserDto, StaffDto } from './system.dto';
import { adminMeStore, AUDIT_DTOS, AUDIT_NOW, staffStore, usersStore } from './system.mock';

const toStaff = (d: StaffDto): StaffMember => ({
  id: d.id,
  fullName: d.full_name,
  email: d.email,
  role: d.role,
  staffTitle: d.staff_title,
  permissions: d.permissions.filter(isPermission),
  status: d.status,
  isSelf: d.is_self,
});

const toUser = (d: PlatformUserDto): PlatformUser => ({
  id: d.id,
  code: d.code,
  fullName: d.full_name,
  shopName: d.shop_name,
  email: d.email,
  phone: d.phone_number,
  role: d.role,
  orderCount: d.order_count,
  warningCount: d.warning_count,
  status: d.status,
  suspension: d.suspension,
});

const toAudit = (d: AuditLogDto): AuditEntry => ({
  id: d.id,
  createdAt: d.created_at,
  actorEmail: d.actor_email,
  actorRole: d.actor_role,
  action: d.action,
  category: d.category,
  target: d.target,
  ip: d.ip_address,
  result: d.result,
});

// ---------- Nhân viên ----------
export async function listStaff(q: string): Promise<StaffMember[]> {
  if (USE_MOCK) {
    await mockDelay(160);
    return staffStore
      .get()
      .map(toStaff)
      .filter((s) => matchesStaff(s, q));
  }
  return (
    await getPage<StaffDto, StaffMember>('/admin/staff', toStaff, {
      query: { q: q.trim() || undefined, size: 60 },
    })
  ).items;
}

function patchStaff(id: number, fn: (s: StaffDto) => StaffDto): void {
  const rows = staffStore.get();
  if (!rows.some((s) => s.id === id)) mockFail('GNE-SYS-4040', { status: 404 });
  staffStore.set(rows.map((s) => (s.id === id ? fn(s) : s)));
}

export async function inviteStaff(v: InviteInput): Promise<void> {
  const body = {
    full_name: v.fullName.trim(),
    email: v.email.trim().toLowerCase(),
    staff_title: v.staffTitle,
    permissions: v.permissions,
  };
  if (USE_MOCK) {
    await mockDelay(300);
    if (!STAFF_EMAIL.test(body.email)) mockFail('GNE-ADM-4221');
    const rows = staffStore.get();
    if (rows.some((s) => s.email === body.email)) mockFail('GNE-ADM-4091');
    if (body.permissions.includes('MANAGE_RBAC')) mockFail('GNE-SEC-4031');
    staffStore.set([
      ...rows,
      {
        id: Math.max(0, ...rows.map((s) => s.id)) + 1,
        ...body,
        role: 'STAFF',
        status: 'PENDING_ACTIVATION',
        is_self: false,
      },
    ]);
    return;
  }
  await api.post('/admin/staff/invitations', body);
}

export async function resendInvitation(id: number): Promise<void> {
  if (USE_MOCK) {
    await mockDelay(200);
    return;
  }
  await api.post(`/admin/staff/${id}/invitations/resend`);
}

/** Lưu quyền; trả cảnh báo tách biệt nhiệm vụ (SEGREGATION_OF_DUTIES) nếu có. */
export async function updateStaffPermissions(
  id: number,
  title: Exclude<StaffTitle, 'ADMIN'>,
  permissions: PermissionCode[],
): Promise<string[]> {
  if (USE_MOCK) {
    await mockDelay(250);
    if (permissions.includes('MANAGE_RBAC')) mockFail('GNE-SEC-4031');
    patchStaff(id, (s) => ({ ...s, staff_title: title, permissions }));
    return segregationWarning(permissions) ? ['SEGREGATION_OF_DUTIES'] : [];
  }
  const res = await request<StaffDto>(`/admin/staff/${id}/permissions`, {
    method: 'PUT',
    body: { staff_title: title, permissions },
  });
  return res.warnings ?? [];
}

export async function lockStaff(id: number, reason: string): Promise<void> {
  if (USE_MOCK) {
    await mockDelay(220);
    patchStaff(id, (s) => ({ ...s, status: 'LOCKED' }));
    return;
  }
  await api.post(`/admin/staff/${id}/lock`, { reason });
}

export async function unlockStaff(id: number): Promise<void> {
  if (USE_MOCK) {
    await mockDelay(220);
    patchStaff(id, (s) => ({ ...s, status: 'ACTIVE' }));
    return;
  }
  await api.post(`/admin/staff/${id}/unlock`);
}

export async function deleteStaff(id: number): Promise<void> {
  if (USE_MOCK) {
    await mockDelay(220);
    const s = staffStore.get().find((x) => x.id === id);
    if (s?.is_self) mockFail('GNE-ADM-4092');
    staffStore.set(staffStore.get().filter((x) => x.id !== id));
    return;
  }
  await api.delete(`/admin/staff/${id}`);
}

// ---------- Người dùng ----------
export interface UserList {
  items: PlatformUser[];
  counts: UserCounts;
}

export async function listUsers(tab: UserTab, q: string): Promise<UserList> {
  if (USE_MOCK) {
    await mockDelay(180);
    const all = usersStore.get().map(toUser);
    const found = all.filter((u) => matchesUser(u, q));
    const counts = Object.fromEntries(
      USER_TABS.map((t) => [t, all.filter((u) => inUserTab(u, t)).length]),
    ) as UserCounts;
    return { items: found.filter((u) => inUserTab(u, tab)), counts };
  }
  const res = await getPage<PlatformUserDto, PlatformUser>('/admin/users', toUser, {
    query: { tab, q: q.trim() || undefined, size: 60 },
  });
  const c = (res.summary.counts ?? {}) as Partial<UserCounts>;
  return {
    items: res.items,
    counts: {
      ALL: c.ALL ?? 0,
      CUSTOMER: c.CUSTOMER ?? 0,
      VENDOR: c.VENDOR ?? 0,
      LOCKED: c.LOCKED ?? 0,
    },
  };
}

export async function suspendUser(
  id: number,
  reason: SuspendReason,
  note: string | null,
  duration: Duration,
  /** Nhãn lý do đã dịch — mock lưu làm lý do hiển thị. */
  label: string,
): Promise<void> {
  if (USE_MOCK) {
    await mockDelay(250);
    usersStore.set(
      usersStore.get().map((u) =>
        u.id === id
          ? {
              ...u,
              status: duration === 'FOREVER' ? 'LOCKED' : 'SUSPENDED',
              suspension: {
                reason: reason === 'OTHER' && note ? note : label,
                until: suspendUntil(duration, new Date()),
              },
            }
          : u,
      ),
    );
    return;
  }
  await api.post(`/admin/users/${id}/suspend`, {
    reason_code: reason,
    note,
    duration: duration === 'FOREVER' ? null : duration,
  });
}

export async function reactivateUser(id: number, reason: string): Promise<void> {
  if (USE_MOCK) {
    await mockDelay(220);
    usersStore.set(
      usersStore.get().map((u) => (u.id === id ? { ...u, status: 'ACTIVE', suspension: null } : u)),
    );
    return;
  }
  await api.post(`/admin/users/${id}/reactivate`, { reason });
}

// ---------- Nhật ký ----------
export interface AuditQuery {
  from: string | null;
  to: string | null;
  category: AuditCategory | null;
  q: string;
}

export interface AuditPage extends CursorPage<AuditEntry> {
  deniedCount: number;
}

const AUDIT_LIMIT = 50;

/** "Bây giờ" của nhật ký: mock đứng ở ngày chốt dữ liệu mẫu (22/06/2026) như prototype. */
export const auditNow = (): Date => (USE_MOCK ? AUDIT_NOW : new Date());

export async function listAuditLogs(f: AuditQuery, cursor: string | null): Promise<AuditPage> {
  if (USE_MOCK) {
    await mockDelay(180);
    const n = f.q.trim().toLowerCase();
    const rows = AUDIT_DTOS.filter((r) => {
      const d = r.created_at.slice(0, 10);
      return (
        (!f.from || d >= f.from) &&
        (!f.to || d <= f.to) &&
        (!f.category || r.category === f.category) &&
        (!n || `${r.actor_email} ${r.action} ${r.target} ${r.ip_address}`.toLowerCase().includes(n))
      );
    });
    const p = paginateCursor(rows, { cursor, limit: AUDIT_LIMIT });
    return {
      ...p,
      items: p.items.map(toAudit),
      deniedCount: AUDIT_DTOS.filter((r) => r.result === 'DENIED').length,
    };
  }
  const res = await request<AuditLogDto[]>('/admin/audit-logs', {
    query: {
      from: f.from ?? undefined,
      to: f.to ?? undefined,
      category: f.category ?? undefined,
      q: f.q.trim() || undefined,
      cursor,
      limit: AUDIT_LIMIT,
    },
  });
  const p = res.pagination && 'has_more' in res.pagination ? res.pagination : null;
  return {
    items: res.data.map(toAudit),
    limit: p?.limit ?? AUDIT_LIMIT,
    has_more: p?.has_more ?? false,
    next_cursor: p?.next_cursor ?? null,
    deniedCount: Number((res.summary ?? {}).denied_count ?? 0),
  };
}

export async function listAllAuditLogs(f: AuditQuery): Promise<AuditEntry[]> {
  const out: AuditEntry[] = [];
  let cursor: string | null = null;
  for (let i = 0; i < 40; i += 1) {
    const page: AuditPage = await listAuditLogs(f, cursor);
    out.push(...page.items);
    if (!page.has_more || !page.next_cursor) break;
    cursor = page.next_cursor;
  }
  return out;
}

// ---------- Tài khoản ----------
export async function updateAdminMe(p: {
  fullName?: string;
  avatarObjectKey?: string;
  avatarPreview?: string | null;
}): Promise<void> {
  if (USE_MOCK) {
    await mockDelay(220);
    const cur = adminMeStore.get();
    adminMeStore.set({
      full_name: p.fullName ?? cur.full_name,
      avatar_url: p.avatarPreview ?? cur.avatar_url,
    });
    return;
  }
  await api.patch('/admin/me', {
    ...(p.fullName ? { full_name: p.fullName } : {}),
    ...(p.avatarObjectKey ? { avatar_object_key: p.avatarObjectKey } : {}),
  });
}
