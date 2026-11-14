// Form danh mục, thuộc tính, từ chối đề xuất (zod; API 11.11).
import { z } from 'zod';
import { ATTR_CODE, MAX_COMMISSION, needsValues, parseValues, SLUG } from './catalog.rules';

export const categorySchema = z.object({
  name: z
    .string()
    .trim()
    .min(1, 'admin-catalog.cat.err.name')
    .max(100, 'admin-catalog.cat.err.name_long'),
  slug: z.string().trim().regex(SLUG, 'admin-catalog.cat.err.slug'),
  parentId: z.number().int().nullable(),
  icon: z.string().min(1),
  commissionRate: z
    .number({ invalid_type_error: 'admin-catalog.cat.err.rate' })
    .min(0, 'admin-catalog.cat.err.rate')
    .max(MAX_COMMISSION, 'admin-catalog.cat.err.rate'),
  active: z.boolean(),
});
export type CategoryValues = z.infer<typeof categorySchema>;

export const attributeSchema = z
  .object({
    code: z.string().trim().toLowerCase().regex(ATTR_CODE, 'admin-catalog.attr.err.code'),
    name: z
      .string()
      .trim()
      .min(1, 'admin-catalog.attr.err.name')
      .max(100, 'admin-catalog.attr.err.name'),
    dataType: z.enum(['TEXT', 'NUMBER', 'BOOLEAN', 'ENUM', 'MULTI_ENUM', 'DATE']),
    valuesText: z.string(),
    unit: z.string().trim().max(20),
    required: z.boolean(),
    filterable: z.boolean(),
    variantAxis: z.boolean(),
  })
  .refine((v) => !needsValues(v.dataType) || parseValues(v.valuesText).length > 0, {
    path: ['valuesText'],
    message: 'admin-catalog.attr.err.values',
  });
export type AttributeValues = z.infer<typeof attributeSchema>;

export const proposalRejectSchema = z.object({
  reason: z
    .string()
    .trim()
    .min(1, 'admin-catalog.proposals.err.reason')
    .max(500, 'admin-catalog.proposals.err.long'),
});
export type ProposalRejectValues = z.infer<typeof proposalRejectSchema>;
