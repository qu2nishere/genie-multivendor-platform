// Providers dùng chung (TS 2.4: app/providers.tsx). Phần thân ở shared/providers để island trong
// features cũng dùng được mà không vi phạm luật phụ thuộc (feature không import app/).
export { Providers, createQueryClient } from '@/shared/providers/Providers';
