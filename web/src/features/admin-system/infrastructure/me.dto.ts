// DTO snake_case (API_SPEC 11.1).
export interface AdminMeDto {
  id: number;
  full_name: string;
  email: string;
  avatar_url: string | null;
  role: 'STAFF' | 'ADMIN';
  staff_title: 'MODERATOR' | 'CS_SPECIALIST' | 'FINANCE_OFFICER' | 'CUSTOM' | null;
  permissions: string[];
  status: 'ACTIVE' | 'LOCKED' | 'PENDING_ACTIVATION';
}
