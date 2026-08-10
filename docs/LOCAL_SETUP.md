# CruxMate 로컬 실행 가이드

## 요구사항

- Java 21
- MySQL 8.4 또는 Docker Compose
- 전체 테스트 실행 시 Testcontainers가 Docker에 접근할 수 있는 환경

## 1. 환경변수 준비

```bash
cp .env.example .env
```

```dotenv
DB_USERNAME=cruxmate
DB_PASSWORD=<local-db-password>
MYSQL_ROOT_PASSWORD=<local-root-password>
COMPOSE_DB_USERNAME=cruxmate
MYSQL_PORT=3307
JWT_SECRET=<base64-encoded-secret>
```

`.env`는 Git에서 제외됩니다. `JWT_SECRET`은 Base64 디코딩 후 32바이트 이상이어야 합니다.
`COMPOSE_DB_USERNAME`은 MySQL 컨테이너가 생성할 일반 사용자이므로 `root`를 사용하면 안 됩니다.
호스트의 기존 MySQL 접속에 `DB_USERNAME=root`를 사용하더라도 Compose 내부 계정은 별도로 유지됩니다.

## 2. MySQL 준비

Docker Compose는 기존 MySQL과 충돌하지 않도록 기본적으로 호스트의 `3307` 포트를 사용합니다.
다른 포트를 사용하려면 `.env`에 `MYSQL_PORT`를 지정합니다. 애플리케이션 컨테이너는 호스트 포트와 관계없이 내부의 `mysql:3306`으로 접속합니다.

### Docker Compose 전체 실행

```bash
docker compose up -d --build
```

```bash
docker compose ps
```

`app`과 `mysql`의 `STATUS`가 모두 `healthy`인지 확인합니다.

```bash
curl http://localhost:8080/actuator/health
```

다음 응답이면 애플리케이션과 데이터베이스 연결이 준비된 상태입니다.

```json
{"groups":["liveness","readiness"],"status":"UP"}
```

### 기존 MySQL 사용

`cruxmate` 데이터베이스를 만들고 `.env`에 적은 계정에 권한을 부여합니다.

```sql
CREATE DATABASE cruxmate
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_0900_ai_ci;

CREATE USER 'cruxmate'@'localhost'
    IDENTIFIED BY '<local-db-password>';

GRANT ALL PRIVILEGES ON cruxmate.* TO 'cruxmate'@'localhost';
```

사용자나 데이터베이스가 이미 존재하면 생성 구문은 생략합니다.

Flyway는 이미 존재하는 데이터베이스 안에 테이블과 제약을 생성할 뿐 데이터베이스 자체를 생성하지 않습니다.

## 3. 애플리케이션 실행 방식

Docker Compose 전체 실행을 선택했다면 애플리케이션이 이미 실행 중이므로 이 단계를 생략합니다.
애플리케이션만 IDE 또는 Maven으로 실행하려면 MySQL만 시작합니다.

```bash
docker compose up -d mysql
```

이때 호스트에서 실행하는 애플리케이션의 DB 주소를 Compose MySQL 포트에 맞춥니다.

```dotenv
DB_URL=jdbc:mysql://localhost:3307/cruxmate?serverTimezone=Asia/Seoul&characterEncoding=UTF-8
```

```bash
./mvnw spring-boot:run
```

실행 후 <http://localhost:8080>에 접속합니다.

## 4. 관리자 계정 준비

회원가입으로 생성되는 역할은 `USER`입니다. 관리자 후보 회원을 먼저 생성합니다.

```bash
curl -i -X POST http://localhost:8080/api/members \
  -H 'Content-Type: application/json' \
  -d '{"email":"admin@example.com","password":"password123!"}'
```

### Docker Compose MySQL에서 ADMIN 역할 변경

```bash
docker compose exec mysql sh -c \
  'mysql -u"$MYSQL_USER" -p"$MYSQL_PASSWORD" "$MYSQL_DATABASE"'
```

### 기존 MySQL에서 ADMIN 역할 변경

`.env`에 설정한 애플리케이션용 계정으로 접속합니다.

```bash
mysql -h 127.0.0.1 -P 3306 -u cruxmate -p cruxmate
```

```sql
UPDATE member
SET role = 'ADMIN'
WHERE email = 'admin@example.com';
```

## 5. ADMIN 재로그인과 JWT 발급

승격 전에 발급된 JWT는 계속 `USER` 권한입니다. 승격 후 다시 로그인합니다.

```bash
curl -s -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"admin@example.com","password":"password123!"}'
```

응답의 `accessToken`을 세션 생성 요청에 사용합니다.

## 6. 시연용 세션 생성

즉시 예약까지 시연하려면 `reservationOpenAt`은 현재 시각과 같거나 이전이어야 하고, 나머지 시간은 다음 순서를 만족해야 합니다.

```text
reservationOpenAt <= 현재 시각 < reservationCloseAt <= startAt < endAt
```

아래 시간 placeholder를 실행 시점에 맞게 바꿉니다.

```bash
curl -i -X POST http://localhost:8080/api/sessions \
  -H 'Authorization: Bearer <admin-access-token>' \
  -H 'Content-Type: application/json' \
  -d '{
    "title": "평일 저녁 중급 세션",
    "location": "광주 그랩잇",
    "startAt": "<future-session-start>",
    "endAt": "<future-session-end>",
    "reservationOpenAt": "<now-or-earlier>",
    "reservationCloseAt": "<future-reservation-close>",
    "capacity": 5,
    "level": "INTERMEDIATE"
  }'
```

`http/session.http`의 세션 생성 예제는 실행 시점 기준으로 예약 시작과 마감, 세션 시작과 종료를 자동 생성합니다.

## 7. 일반 사용자 시연

`POST /api/members`로 일반 사용자를 생성하고 `POST /api/auth/login`으로 로그인합니다. 이후 브라우저에서 <http://localhost:8080>에 접속해 다음 흐름을 확인합니다.

```text
로그인 → 세션 예약 → 내 예약 조회 → 예약 취소
```

HTTP 요청은 `http/` 디렉터리에서 확인할 수 있습니다. 발급된 토큰은 커밋하지 않습니다.

`http/reservation.http`의 `Idempotency-Key`는 같은 예약 시도의 재요청에만 재사용합니다. 새로운 예약 요청에는 기존 요청과 겹치지 않는 새 키를 사용해야 합니다.

## 8. Compose 종료 및 초기화

기존 MySQL을 사용했다면 이 절차는 생략합니다. 다음 명령은 컨테이너를 종료하지만 데이터 볼륨은 유지합니다.

```bash
docker compose down
```

```bash
docker compose down -v
```

> **주의:** `docker compose down -v`는 모든 로컬 데이터를 삭제합니다.

## 9. 전체 테스트

```bash
./mvnw --batch-mode --no-transfer-progress clean test
```
