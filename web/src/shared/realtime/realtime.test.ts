import { describe, expect, it } from 'vitest';
import { toForcedLogoutReason, wsUrlOf } from './accountChannel';
import { encodeFrame, parseFrames } from './stomp';

describe('STOMP frames', () => {
  it('encodes and parses round trip', () => {
    const raw = encodeFrame('SUBSCRIBE', { id: 'sub-0', destination: '/user/queue/account' });
    expect(raw.endsWith('\0')).toBe(true);
    expect(parseFrames(raw)).toEqual([
      {
        command: 'SUBSCRIBE',
        headers: { id: 'sub-0', destination: '/user/queue/account' },
        body: '',
      },
    ]);
  });

  it('splits several frames and skips heart-beats', () => {
    const data =
      '\nMESSAGE\nsubscription:sub-0\ndestination:/user/queue/account\n\n{"type":"SESSION_REVOKED"}\0\n' +
      'ERROR\nmessage:bad\\ctoken\n\n\0';
    const frames = parseFrames(data);
    expect(frames).toHaveLength(2);
    expect(frames[0].body).toBe('{"type":"SESSION_REVOKED"}');
    expect(frames[1].headers.message).toBe('bad:token');
  });
});

describe('account channel', () => {
  it('derives the websocket url from the api url', () => {
    expect(wsUrlOf('http://localhost:8080/api/v1')).toBe('ws://localhost:8080/ws');
    expect(wsUrlOf('https://api.genie.vn/api/v1')).toBe('wss://api.genie.vn/ws');
  });

  it('maps unknown reasons to DEFAULT', () => {
    expect(toForcedLogoutReason('SUSPENDED')).toBe('SUSPENDED');
    expect(toForcedLogoutReason('SUSPICIOUS_REUSE')).toBe('DEFAULT');
    expect(toForcedLogoutReason(undefined)).toBe('DEFAULT');
  });
});
