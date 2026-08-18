import http from 'k6/http';
import { check, fail } from 'k6';
import exec from 'k6/execution';
import { Counter, Rate, Trend } from 'k6/metrics';

const USER_COUNT = 100;
const SESSION_CAPACITY = 30;
const PARTICIPANT_COUNT = 1;

const reservationRequests = new Counter('reservation_requests');
const reservationSuccesses = new Counter('reservation_successes');
const reservationFailures = new Counter('reservation_failures');
const reservationCapacityRejections = new Counter(
  'reservation_capacity_rejections',
);
const reservationUnexpectedFailures = new Counter(
  'reservation_unexpected_failures',
);
const reservationSuccessRate = new Rate('reservation_success_rate');
const reservationDuration = new Trend('reservation_duration', true);

export const options = {
  setupTimeout: '10m',
  scenarios: {
    reservation_contention: {
      executor: 'per-vu-iterations',
      vus: USER_COUNT,
      iterations: 1,
      maxDuration: '2m',
    },
  },
};

function requiredEnvironment(name) {
  const value = __ENV[name];

  if (!value) {
    fail(`필수 환경변수 ${name}가 설정되지 않았습니다.`);
  }

  return value;
}

function normalizedBaseUrl() {
  return requiredEnvironment('BASE_URL').replace(/\/+$/, '');
}

function jsonHeaders(accessToken) {
  const headers = { 'Content-Type': 'application/json' };

  if (accessToken) {
    headers.Authorization = `Bearer ${accessToken}`;
  }

  return headers;
}

function responseBody(response) {
  try {
    return JSON.stringify(response.json());
  } catch (_) {
    return response.body;
  }
}

function requireStatus(response, expectedStatus, operation) {
  if (response.status !== expectedStatus) {
    fail(
      `${operation} 실패: expected=${expectedStatus}, actual=${response.status}, body=${responseBody(response)}`,
    );
  }
}

function requireJsonField(response, field, operation) {
  let body;

  try {
    body = response.json();
  } catch (_) {
    fail(`${operation} 응답이 JSON이 아닙니다: body=${response.body}`);
  }

  if (body[field] === undefined || body[field] === null || body[field] === '') {
    fail(`${operation} 응답에 ${field}가 없습니다: body=${responseBody(response)}`);
  }

  return body[field];
}

// The application Clock is fixed to Asia/Seoul. The API accepts LocalDateTime,
// so convert an instant to a zone-less Asia/Seoul date-time string.
function seoulLocalDateTime(epochMilliseconds) {
  const KOREA_OFFSET_MILLISECONDS = 9 * 60 * 60 * 1000;
  return new Date(epochMilliseconds + KOREA_OFFSET_MILLISECONDS)
    .toISOString()
    .slice(0, 19);
}

function login(baseUrl, email, password, operation) {
  const response = http.post(
    `${baseUrl}/api/auth/login`,
    JSON.stringify({ email, password }),
    { headers: jsonHeaders(), tags: { phase: 'setup', operation: 'login' } },
  );

  requireStatus(response, 200, operation);
  return requireJsonField(response, 'accessToken', operation);
}

export function setup() {
  const baseUrl = normalizedBaseUrl();
  const adminEmail = requiredEnvironment('ADMIN_EMAIL');
  const adminPassword = requiredEnvironment('ADMIN_PASSWORD');
  const runId = `${Date.now()}-${Math.floor(Math.random() * 1_000_000)}`;
  const testPassword = `CruxK6!${Date.now()}`;

  const adminAccessToken = login(
    baseUrl,
    adminEmail,
    adminPassword,
    '관리자 로그인',
  );

  const now = Date.now();
  const sessionResponse = http.post(
    `${baseUrl}/api/sessions`,
    JSON.stringify({
      title: `k6 reservation contention ${runId}`,
      location: 'k6-load-test',
      reservationOpenAt: seoulLocalDateTime(now - 10 * 60 * 1000),
      reservationCloseAt: seoulLocalDateTime(now + 60 * 60 * 1000),
      startAt: seoulLocalDateTime(now + 90 * 60 * 1000),
      endAt: seoulLocalDateTime(now + 150 * 60 * 1000),
      capacity: SESSION_CAPACITY,
      level: 'ALL_LEVELS',
    }),
    {
      headers: jsonHeaders(adminAccessToken),
      tags: { phase: 'setup', operation: 'create_session' },
    },
  );

  requireStatus(sessionResponse, 201, '세션 생성');
  const sessionId = requireJsonField(sessionResponse, 'sessionId', '세션 생성');
  const users = [];

  for (let index = 0; index < USER_COUNT; index += 1) {
    const email = `cruxmate-k6-${runId}-${index}@example.com`;
    const createMemberResponse = http.post(
      `${baseUrl}/api/members`,
      JSON.stringify({ email, password: testPassword }),
      {
        headers: jsonHeaders(),
        tags: { phase: 'setup', operation: 'create_member' },
      },
    );

    requireStatus(createMemberResponse, 201, `테스트 회원 ${index + 1} 생성`);

    const accessToken = login(
      baseUrl,
      email,
      testPassword,
      `테스트 회원 ${index + 1} 로그인`,
    );

    users.push({
      accessToken,
      idempotencyKey: `reservation-contention-${runId}-${index}`,
    });
  }

  console.log(
    `contention test ready: sessionId=${sessionId}, capacity=${SESSION_CAPACITY}, users=${users.length}`,
  );

  return { baseUrl, sessionId, users };
}

export default function (data) {
  // idInTest is 1-based and unique for each VU in this scenario.
  const userIndex = exec.vu.idInTest - 1;
  const user = data.users[userIndex];

  if (!user) {
    fail(`VU ${exec.vu.idInTest}에 할당할 테스트 사용자가 없습니다.`);
  }

  const response = http.post(
    `${data.baseUrl}/api/reservations`,
    JSON.stringify({
      sessionId: data.sessionId,
      participantCount: PARTICIPANT_COUNT,
    }),
    {
      headers: {
        ...jsonHeaders(user.accessToken),
        'Idempotency-Key': user.idempotencyKey,
      },
      tags: { phase: 'contention', operation: 'create_reservation' },
    },
  );

  const succeeded = response.status === 201;
  let errorCode = null;

  if (!succeeded) {
    try {
      errorCode = response.json('code');
    } catch (_) {
      // Count a non-JSON response as an unexpected failure below.
    }
  }

  const expectedCapacityRejection = response.status === 400 && errorCode === 'COM001';

  reservationRequests.add(1);
  reservationSuccessRate.add(succeeded);
  reservationDuration.add(response.timings.duration);

  if (succeeded) {
    reservationSuccesses.add(1);
  } else {
    reservationFailures.add(1);

    if (expectedCapacityRejection) {
      reservationCapacityRejections.add(1);
    } else {
      reservationUnexpectedFailures.add(1);
    }
  }

  check(response, {
    'reservation returned expected status': () =>
      succeeded || expectedCapacityRejection,
  });
}
