import http from 'k6/http';
import { check, sleep } from 'k6';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

export const options = {
  scenarios: {
    shorten_and_redirect: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '30s', target: 20 },
        { duration: '1m', target: 6000 },
        { duration: '2m', target: 1000 },
        { duration: '30s', target: 0 },
      ],
      gracefulRampDown: '30s',
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.02'],
    http_req_duration: ['p(95)<1000', 'p(99)<2000'],
  },
};

export default function () {
  const shortenPayload = JSON.stringify({
    longUrl: `https://example.com/load/${__VU}-${__ITER}-${Date.now()}`,
  });

  const shortenResponse = http.post(
    `${BASE_URL}/api/v1/data/shorten`,
    shortenPayload,
    {
      headers: { 'Content-Type': 'application/json' },
      tags: { endpoint: 'shorten' },
    }
  );

  const shortenOk = check(shortenResponse, {
    'shorten returns success':
      (response) => response.status === 201 || response.status === 200,
  });

  if (!shortenOk) {
    sleep(1);
    return;
  }

  const body = shortenResponse.json();
  const shortUrl = body.shortUrl;

  if (shortUrl) {
    const redirectResponse = http.get(`${BASE_URL}/api/v1/${shortUrl}`, {
      redirects: 0,
      tags: { endpoint: 'redirect' },
    });

    check(redirectResponse, {
      'redirect returns 301/302':
        (response) => response.status === 301 || response.status === 302,
    });

    const analyticsResponse = http.get(
      `${BASE_URL}/api/v1/analytics/${shortUrl}`,
      {
        tags: { endpoint: 'analytics' },
      }
    );

    check(analyticsResponse, {
      'analytics returns 200': (response) => response.status === 200,
    });
  }

  sleep(1);
}
