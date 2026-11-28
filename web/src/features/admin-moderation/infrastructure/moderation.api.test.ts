// Unit test mapper & mock filter admin-moderation — UC-CAT-12/13 (c1_auto_product.md mục 5.6; update_gemini.md mục 3.8)
import { describe, expect, it } from 'vitest';
import { listModeration, toAiSettings, toDetail, toRow } from './moderation.api';

describe('toAiSettings', () => {
  it('maps snake_case DTO to camelCase domain object', () => {
    expect(
      toAiSettings({
        enabled: true,
        mode: 'http',
        model: 'gemini-2.5-flash',
        reject_threshold: 85,
        configured: true,
        updated_at: '2026-11-27T18:10:00+07:00',
        updated_by: 'Đặng Hải Yến',
      }),
    ).toEqual({
      enabled: true,
      mode: 'http',
      model: 'gemini-2.5-flash',
      rejectThreshold: 85,
      configured: true,
      updatedAt: '2026-11-27T18:10:00+07:00',
      updatedBy: 'Đặng Hải Yến',
    });
  });

  it('preserves null updated_at and updated_by when never configured', () => {
    expect(
      toAiSettings({
        enabled: false,
        mode: 'stub',
        model: 'stub',
        reject_threshold: 85,
        configured: true,
        updated_at: null,
        updated_by: null,
      }),
    ).toEqual({
      enabled: false,
      mode: 'stub',
      model: 'stub',
      rejectThreshold: 85,
      configured: true,
      updatedAt: null,
      updatedBy: null,
    });
  });
});

describe('moderation mappers & AI filter', () => {
  it('maps ai_check in toRow and fallback + created_at in toDetail', () => {
    const row = toRow({
      product_id: 10,
      name: 'Sản phẩm A',
      image_url: '/img/a.jpg',
      shop: { id: 1, name: 'Shop' },
      status: 'PENDING_APPROVAL',
      is_requeued: false,
      assignee: null,
      rejected_by_ai: false,
      submitted_at: '2026-11-28T10:00:00+07:00',
      ai_check: 'FAILED',
    });
    expect(row.aiCheck).toBe('FAILED');

    const detail = toDetail({
      product_id: 10,
      name: 'Sản phẩm A',
      image_url: '/img/a.jpg',
      shop: { id: 1, name: 'Shop' },
      status: 'PENDING_APPROVAL',
      is_requeued: false,
      assignee: null,
      rejected_by_ai: false,
      submitted_at: '2026-11-28T10:00:00+07:00',
      ai_check: 'FAILED',
      ai_result: {
        decision: 'NEED_REVIEW',
        risk_score: 50,
        violations: [],
        reasoning: null,
        fallback: true,
        created_at: '2026-11-28T10:01:00+07:00',
      },
      decision_reason: null,
      images: [],
      documents: [],
      category_path: 'Điện tử',
      brand: null,
      condition: 'NEW',
      origin: null,
      description_html: null,
      weight_grams: 100,
      dimensions_cm: null,
      ships_from: null,
      shipping_payer: 'BUYER',
      variants: [],
    });
    expect(detail.ai?.fallback).toBe(true);
    expect(detail.ai?.screenedAt).toBe('2026-11-28T10:01:00+07:00');
  });

  it('filters mock listModeration by ai without changing tab counts', async () => {
    const unfiltered = await listModeration('PENDING', '', 1);
    const passedOnly = await listModeration('PENDING', '', 1, 'PASSED');

    expect(passedOnly.items.length).toBeGreaterThan(0);
    expect(passedOnly.items.every((r) => r.aiCheck === 'PASSED')).toBe(true);
    expect(passedOnly.counts).toEqual(unfiltered.counts);
  });
});
