import { z } from 'zod';

// PUBLIC_API_URL, INTERNAL_API_URL đọc qua zod (TECH_STACK mục 2.4)
const schema = z.object({
  PUBLIC_API_URL: z.string().url(),
  PUBLIC_S3_URL: z.string().url().optional(),
  INTERNAL_API_URL: z.string().url().optional(),
});

export const env = schema.parse({
  PUBLIC_API_URL: import.meta.env.PUBLIC_API_URL,
  PUBLIC_S3_URL: import.meta.env.PUBLIC_S3_URL,
  INTERNAL_API_URL: import.meta.env.INTERNAL_API_URL,
});
