import { describe, expect, it } from 'vitest';
import { fromFollowedShop } from './shop.mapper';

describe('fromFollowedShop', () => {
  it('đổi dòng GET /me/following sang Shop cho màn "Shop đang theo dõi"', () => {
    const s = fromFollowedShop({
      shop: { id: 15, name: 'TechZone Official', logo_url: null, is_mall: true },
      rating_avg: 4.9,
      follower_count: 128000,
    });
    expect(s).toMatchObject({
      id: 15,
      name: 'TechZone Official',
      authentic: true,
      rating: 4.9,
      followerCount: 128000,
      followed: true,
    });
  });
});
