import test from 'node:test';
import assert from 'node:assert/strict';
import { authedRequest, minimumBidForLot, requestJson, toLocalDateTimePayload } from './api.js';

test('minimumBidForLot adds current price and bid step', () => {
  assert.equal(minimumBidForLot({ currentPrice: '150.25', startPrice: '100', bidStep: '9.75' }), 160);
});

test('toLocalDateTimePayload keeps backend LocalDateTime format without timezone', () => {
  assert.equal(toLocalDateTimePayload('2026-05-11T15:30'), '2026-05-11T15:30:00');
  assert.equal(toLocalDateTimePayload('2026-05-11T15:30:00'), '2026-05-11T15:30:00');
});

test('authedRequest refreshes token and retries protected request', async () => {
  const calls = [];
  globalThis.fetch = async (url, options = {}) => {
    calls.push({ url, options });

    if (url === '/api/protected' && options.headers.Authorization === 'Bearer old-token') {
      return new Response(JSON.stringify({ message: 'expired' }), { status: 401, headers: { 'Content-Type': 'application/json' } });
    }

    if (url === '/api/auth/refresh') {
      return new Response(JSON.stringify({ accessToken: 'new-token', refreshToken: 'rotated', user: { id: 1 } }), { status: 200, headers: { 'Content-Type': 'application/json' } });
    }

    if (url === '/api/protected' && options.headers.Authorization === 'Bearer new-token') {
      return new Response(JSON.stringify({ ok: true }), { status: 200, headers: { 'Content-Type': 'application/json' } });
    }

    return new Response(JSON.stringify({ message: 'unexpected request' }), { status: 500, headers: { 'Content-Type': 'application/json' } });
  };

  let refreshed = null;
  const result = await authedRequest({
    url: '/api/protected',
    token: 'old-token',
    refreshToken: 'refresh-token',
    onAuthRefresh: (payload) => {
      refreshed = payload;
    },
    options: { method: 'POST' }
  });

  assert.equal(result.res.status, 200);
  assert.deepEqual(result.data, { ok: true });
  assert.equal(refreshed.accessToken, 'new-token');
  assert.equal(calls.length, 3);
});


test('requestJson marks html responses as non-json instead of pretending they are valid data', async () => {
  globalThis.fetch = async () => new Response('<!doctype html><div id="root"></div>', {
    status: 200,
    headers: { 'Content-Type': 'text/html' }
  });

  const result = await requestJson('/api/auction/lots/1');

  assert.equal(result.res.status, 200);
  assert.equal(result.isJson, false);
  assert.equal(result.data, null);
});
