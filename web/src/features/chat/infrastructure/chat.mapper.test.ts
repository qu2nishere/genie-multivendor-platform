// DTO API 8.3 ⇄ domain, thân 8.4 (plan 2026-11-26 chat M06.06)
import { describe, expect, it } from 'vitest';
import { outgoingBody, toMessage, type MessageDto } from './chat.api';

const dto = (over: Partial<MessageDto>): MessageDto => ({
  id: 7,
  sender: 'THEM',
  type: 'TEXT',
  text: 'Xin chào',
  product: null,
  order: null,
  image_url: null,
  sent_at: '2026-06-08T07:00:00Z',
  status: 'DELIVERED',
  client_message_id: null,
  ...over,
});

describe('chat mapper', () => {
  it('tin tự động và trạng thái', () => {
    const m = toMessage(dto({ is_auto: true, status: 'SEEN' }));
    expect(m).toMatchObject({ id: 7, mine: false, auto: true, status: 'SEEN', text: 'Xin chào' });
    expect(toMessage(dto({ sender: 'ME' })).mine).toBe(true);
    expect(toMessage(dto({})).auto).toBe(false);
  });

  it('thẻ sản phẩm hết bán: giá null, available false', () => {
    const m = toMessage(
      dto({
        type: 'PRODUCT_CARD',
        product: {
          id: 1,
          slug: 'tai-nghe',
          name: 'Tai nghe',
          image_url: null,
          price: null,
          is_available: false,
        },
      }),
    );
    expect(m.product).toEqual({
      id: 1,
      slug: 'tai-nghe',
      name: 'Tai nghe',
      image: null,
      price: null,
      available: false,
    });
  });

  it('thân gửi tin snake_case kèm client_message_id', () => {
    expect(outgoingBody({ type: 'TEXT', text: 'Hi' }, 'u1')).toEqual({
      type: 'TEXT',
      text: 'Hi',
      client_message_id: 'u1',
    });
    expect(outgoingBody({ type: 'PRODUCT_CARD', productId: 3, variantId: 9 }, 'u2')).toEqual({
      type: 'PRODUCT_CARD',
      product_id: 3,
      variant_id: 9,
      client_message_id: 'u2',
    });
    expect(
      outgoingBody({ type: 'IMAGE', imageObjectKey: 'tmp/user/1/a.png', previewUrl: null }, 'u3'),
    ).toMatchObject({ image_object_key: 'tmp/user/1/a.png' });
    expect(outgoingBody({ type: 'ORDER_CARD', orderId: 44 }, 'u4')).toMatchObject({ order_id: 44 });
  });
});
