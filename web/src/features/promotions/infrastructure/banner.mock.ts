// Banner mẫu — dùng kho chung shared/api/mock/banners.ts (Admin quản lý cùng bộ dữ liệu, G12.36).
import { BANNER_SEED } from '@/shared/api/mock/banners';
import type { BannerDto } from './banner.dto';

export const BANNER_DTOS: BannerDto[] = BANNER_SEED;
