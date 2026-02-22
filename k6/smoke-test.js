import http from 'k6/http';
import { check } from 'k6';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

export const options = {
  vus: 1,
  iterations: 10,
  thresholds: {
    http_req_failed: ['rate<0.05'],
    http_req_duration: ['p(95)<800'],
  },
};

export default function () {
  const healthResponse = http.get(`${BASE_URL}/actuator/health`);
  check(healthResponse, {
    'health status is 200': (response) => response.status === 200,
  });

  const shortenPayload = JSON.stringify({
    longUrl: `https://example.com/smoke/${Date.now()}`,
  });

  const shortenResponse = http.post(
    `${BASE_URL}/api/v1/data/shorten`,
    shortenPayload,
    {
      headers: { 'Content-Type': 'application/json' },
    }
  );

  check(shortenResponse, {
    'shorten status is 201 or 200':
      (response) => response.status === 201 || response.status === 200,
  });

  if (shortenResponse.status !== 201 && shortenResponse.status !== 200) {
    return;
  }

  const body = shortenResponse.json();
  const shortUrl = body.shortUrl;

  if (!shortUrl) {
    return;
  }

  const redirectResponse = http.get(`${BASE_URL}/api/v1/${shortUrl}`, {
    redirects: 0,
  });

  check(redirectResponse, {
    'redirect status is 301/302':
      (response) => response.status === 301 || response.status === 302,
  });
}
