import { describe, expect, it } from 'vitest';
import { isKnownPolicy, neighbours } from './policies.rules';
import type { PolicyGroup } from './policies.types';

const G: PolicyGroup[] = [
  {
    slug: 'a',
    title: 'A',
    pages: [
      { slug: 'p1', name: '1' },
      { slug: 'p2', name: '2' },
    ],
  },
  { slug: 'b', title: 'B', pages: [{ slug: 'p3', name: '3' }] },
];

describe('policies.rules', () => {
  it('trang trước / sau xuyên nhóm', () => {
    expect(neighbours(G, 'p1')).toEqual({ prev: null, next: { slug: 'p2', name: '2' } });
    expect(neighbours(G, 'p2').next?.slug).toBe('p3');
    expect(neighbours(G, 'p3').next).toBeNull();
    expect(neighbours(G, 'x')).toEqual({ prev: null, next: null });
  });
  it('isKnownPolicy', () => {
    expect(isKnownPolicy(G, 'p3')).toBe(true);
    expect(isKnownPolicy(G, 'nope')).toBe(false);
  });
});
