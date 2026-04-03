# SettleOps — 중고거래 안전결제 정산 · 운영통제(Hold) · 환불 차감정산 · Trace/Audit 재현 플랫폼 (TEAM README)

결제 승인 이후의 **정산 생성(T+N) · Hold/Release 운영통제 · 4-Eyes 지급 승인 · 환불 차감정산 · 요청 단위 Trace/Audit 재현**까지 다루는  
**정산 운영 백오피스 플랫폼**입니다.

실제 PG 연동이나 실결제는 포함하지 않으며, 내부에서 생성한 **주문 · 결제 · 정산 · 환불 · Hold 데이터**를 기준으로  
운영자가 설명 가능한 상태전이와 재현 가능한 운영 흐름을 구현하는 데 초점을 맞췄습니다.

> 본 프로젝트의 최신 기준선은 `docs/` 내 기획, 상태전이, API 계약, ERD 문서를 따릅니다.  
> 문서와 구현 간 차이가 발생할 경우, 팀 합의 PR 기준으로 최신화합니다.

---

## 목차

1. [프로젝트 개요](#1-프로젝트-개요)  
2. [팀 정보 / 역할 분담](#2-팀-정보--역할-분담)  
3. [기술 스택 / 실행 환경](#3-기술-스택--실행-환경)  
4. [로컬 실행 순서](#4-로컬-실행-순서)  
5. [환경 설정 / Profile 정책](#5-환경-설정--profile-정책)  
6. [인증 / 요청 추적 정책](#6-인증--요청-추적-정책)  
7. [화면 구성 / 라우팅 맵](#7-화면-구성--라우팅-맵)  
8. [핵심 API 엔드포인트](#8-핵심-api-엔드포인트)  
9. [핵심 기능 요약 (동작 플로우)](#9-핵심-기능-요약-동작-플로우)  
10. [핵심 설계 원칙 / 도메인 규칙](#10-핵심-설계-원칙--도메인-규칙)  
11. [수동 테스트 시나리오](#11-수동-테스트-시나리오)  
12. [ERD](#12-erd)  
13. [API Docs](#13-api-docs)  
14. [Demo Video](#14-demo-video)  
15. [Proof Pack](#15-proof-pack)  
16. [레포 구조](#16-레포-구조)  
17. [Troubleshooting](#17-troubleshooting)

---

## 1. 프로젝트 개요

대부분의 데모 프로젝트는 결제 이전의 사용자 플로우에 집중하지만,  
실제 운영 환경에서는 결제 이후의 **정산, 지급 통제, 환불 반영, 감사 추적성**이 더 중요합니다.

SettleOps는 아래 운영 흐름을 하나의 서비스 안에서 재현하는 팀 프로젝트입니다.

- 주문 생성
- 결제 승인 (`CAPTURED`)
- 인수확정 (`PAYMENT_CONFIRMED` event)
- 정산 배치 실행 (`T+N`)
- Hold 생성 / 승인 / 해제
- 지급 요청 / 4-Eyes 지급 승인
- 환불 요청 / 승인 / 거절
- 다음 배치에서 `REFUND` line 차감 반영
- `request_id` 기준 Trace / Audit 재현

### 프로젝트 목표

- 결제 이후의 **정산 운영 흐름**을 end-to-end로 재현
- T+N 배치 기반 **정산 생성 및 정합성 검증**
- Hold / Refund / 4-Eyes 기반 **운영 통제**
- `request_id` 기반 **요청 단위 Trace / Audit 재현성 확보**
- 운영자가 **왜 막혔는지 / 다음에 무엇을 해야 하는지** 설명 가능한 UX 제공

---

## 2. 팀 정보 / 역할 분담

- **A(yeowoniing)**: Settlement Batch / Settlement / 4-Eyes Payout
- **B(chenmei17)**: Payment / Idempotency / Confirm
- **C(heyin-hein)**: Refund / Refund Adjustment
- **D(ggeuri)**: Hold / Trace / Audit / 운영 허브 연계

> ERD, 상태전이, API 계약, REASON_CODE, request_id 규칙은 팀 합의 기반으로 고정했습니다.

---

## 3. 기술 스택 / 실행 환경

### Backend
- Java 21
- Spring Boot 3.5.10
- Spring Data JPA
- QueryDSL
- JdbcTemplate (batch option)
- Gradle Wrapper

### Frontend
- React 18
- Vite
- Admin Web

### Data / Infra
- MySQL 8.0.44
- Redis
- Docker Compose

### Collaboration
- GitHub
- Jira
- Confluence

### Local Infra (LOCKED)
- Docker Compose 기반 MySQL + Redis 사용
- 단일 DB / 모듈러 모놀리식 구조

---

## 4. 로컬 실행 순서

### 1) Infra 실행

```bash
docker compose up -d
```

### 2) Backend 실행

```bash
cd server
./gradlew bootRun
```

### 3) Frontend 실행

```bash
cd admin-web
npm install
npm run dev
```

### 4) 접속 확인

- Backend: Spring Boot local profile 기준 포트
- Frontend(Admin): Vite dev server 기준 포트
- MySQL / Redis: Docker Compose 기준 포트 사용

> 실제 포트와 환경 값은 `docker-compose.yml`, `server` 설정 파일, `admin-web` 설정 파일 기준으로 확인합니다.

---

## 5. 환경 설정 / Profile 정책

### application profile

- `local`: 로컬 개발 실행
- `test`: H2 기반 빠른 테스트
- `test-db`: Docker Compose MySQL 기준 통합 테스트

### 테스트 프로필 기준

- `test`
  - 단위 테스트
  - slice 테스트
  - 일부 경량 통합 테스트

- `test-db`
  - 실제 MySQL 정합성 검증
  - 제약조건 검증
  - 트랜잭션 / 이벤트 적재 검증
  - QueryDSL 조회 결과 검증
  - Repository / Integration 테스트

### 주의사항

- 테스트 클래스 내부 datasource 하드코딩은 사용하지 않습니다.
- 어떤 테스트를 `test` / `test-db`로 분류할지는 **실제 MySQL 정합성 의존 여부**를 기준으로 합니다.

---

## 6. 인증 / 요청 추적 정책

### 인증

- 세션 기반 인증
- 프론트 요청은 `credentials: "include"` 기준
- 최종 권한 판단은 서버 401 / 403 응답 기준

### 요청 추적

- 요청 헤더 `X-Request-Id`가 있으면 그대로 사용
- 없으면 서버가 UUID 생성
- 모든 API 응답 헤더에 `X-Request-Id` 포함
- `request_id` 생성/주입은 단일 Filter에서만 수행

### request_id 바디 포함 범위

운영/재현 목적 응답만 JSON 바디에 `requestId`를 포함합니다.

- A1 Trace 조회
- A2 배치 실행 / 결과
- 운영 액션 응답
  - hold create / approve / release
  - refund approve / reject
  - settlement request-paid / approve-paid

그 외 일반 API는 헤더의 `X-Request-Id`만 사용합니다.

---

## 7. 화면 구성 / 라우팅 맵

### Consumer

- **C1** 거래 생성
- **C2** 결제 상세 / 승인
- **C3** 내 주문 / 결제 내역

### Merchant

- **U2** 결제 조회 / 검색
- **U3** 결제 상세
- **U4** 정산 리스트
- **U5** 정산 상세
- **U6** 환불 요청 / 현황

### Admin

- **A1** Trace / Audit 검색
- **A2** 배치 실행 / 이력
- **A3** 정산 관리
- **A4** 정산 상세 (운영 허브)
- **A5** Hold 큐
- **A6** 환불 큐

### Admin 운영 허브 기준

- 운영팀의 작업 시작점은 `settlementId`
- A3 → A4 → A5 / A6 / A1 / A2로 이어지는 운영 동선을 사용합니다.

---

## 8. 핵심 API 엔드포인트

### Consumer

- `POST /api/consumer/orders`
- `GET /api/consumer/orders/{orderId}`
- `POST /api/consumer/orders/{orderId}/pay`
- `POST /api/consumer/payments/{paymentId}/confirm`

### Merchant

- `GET /api/merchants/{merchantId}/payments`
- `GET /api/payments/{paymentId}`
- `GET /api/payments/{paymentId}/refund-context`
- `POST /api/refunds`
- `GET /api/merchants/{merchantId}/settlements`
- `GET /api/merchants/{merchantId}/settlements/{settlementId}`
- `GET /api/me/refunds`

### Admin

- `POST /api/admin/settlement-batches/run?baseDate=YYYY-MM-DD`
- `GET /api/admin/settlements`
- `PATCH /api/admin/settlements/{settlementId}/request-paid`
- `PATCH /api/admin/settlements/{settlementId}/approve-paid`
- `POST /api/admin/holds`
- `PATCH /api/admin/holds/{holdId}/approve`
- `PATCH /api/admin/holds/{holdId}/release`
- `GET /api/admin/refunds`
- `PATCH /api/admin/refunds/{refundId}/approve`
- `PATCH /api/admin/refunds/{refundId}/reject`
- `GET /api/admin/audit-logs`
- `GET /api/admin/audit-events?requestId=`
- `GET /api/admin/settlement-batches/history`
- `GET /api/admin/settlements/{settlementId}/trace-entry`
- `GET /api/admin/refunds/{refundId}/trace-entry`

> 상세 요청/응답 스펙은 `docs/api/` 경로를 참고합니다.

---

## 9. 핵심 기능 요약 (동작 플로우)

```text
1. Consumer 주문 생성
2. Consumer pay 호출 → payment CAPTURED
3. Consumer confirm 호출 → PAYMENT_CONFIRMED event 생성
4. Admin이 A2에서 batch run 실행 → settlement 생성
5. A4에서 settlement 상세 확인
6. 필요 시 Hold 생성 → A5에서 approve / release
7. request-paid → PAY_REQUESTED
8. 다른 Admin이 approve-paid → PAID
9. Merchant 환불 요청 → Admin 승인/거절
10. APPROVED refund는 다음 batch에서 REFUND line으로 반영
11. A1에서 request_id 기준 Trace / Audit 재현
```

### 운영 가드레일

- **Idempotency**
  - `POST /api/consumer/orders/{orderId}/pay`
  - `X-Idempotency-Key` 기반
  - 동일 `(orderId, X-Idempotency-Key)` 재요청은 no-op 200

- **no-op 200 표준화**
  - 이미 처리된 동일 액션은 에러 대신 no-op 200으로 응답
  - 운영자 재시도와 실제 실패를 구분 가능하게 설계

- **4-Eyes**
  - 지급 요청자와 승인자는 달라야 함
  - 동일 승인자 지급 승인 시 `409 SAME_APPROVER_NOT_ALLOWED`

- **Refund Adjustment Guard**
  - 승인된 환불이 아직 다음 배치에 반영되지 않았으면 지급 요청 차단
  - `409 REFUND_ADJUSTMENT_PENDING`

- **Append-only Event / Audit**
  - `payment_event`, `refund_event`, `hold_event`는 insert-only
  - `audit_log`는 행위 중심 insert-only 로그

---

## 10. 핵심 설계 원칙 / 도메인 규칙

### 1) CAPTURED와 CONFIRMED 분리

- `CAPTURED` = 결제 승인 완료
- `CONFIRMED` = 정산 대상화 완료

`CONFIRMED`는 `payment.status`가 아니라  
`payment_event(PAYMENT_CONFIRMED)` insert-only 이벤트로만 표현합니다.

### 2) 환불 승인과 정산 반영 완료 분리

- 승인 사실 SoT → `refund.status = APPROVED`
- 반영 완료 증거 SoT → `refund_settlement_link`

즉, 환불 승인과 다음 정산 반영 완료는 같은 사실로 취급하지 않습니다.

### 3) Settlement 결과를 SoT로 유지

- `settlement`는 배치가 계산한 집계 결과 SoT
- `settlement_line`은 `PAYMENT / REFUND` 근거 설명용

정합성 검증은 아래 단일 식을 사용합니다.

```text
settlement.net = Σ(settlement_line.amount × sign)

PAYMENT: +1
REFUND: -1
```

### 4) 운영자는 settlementId를 앵커로 탐색

운영팀의 시작점은 `settlementId`이며,  
A4 정산 상세에서 관련 Hold / Refund / Trace / Batch로 연결됩니다.

### 5) 운영 액션은 request_id 기준 재현 가능해야 함

- 모든 운영 액션은 `request_id` 단위로 추적 가능해야 함
- no-op 요청도 audit_log에 기록하여 재현성을 보장합니다.

---

## 11. 수동 테스트 시나리오

아래 시나리오는 SettleOps의 핵심 흐름을 빠르게 검증하기 위한 최소 셋입니다.

### 1) 주문 생성 → 결제 승인 → 인수확정

- 주문 생성
- `pay` 호출
- `payment.status = CAPTURED`
- `confirm` 호출
- `PAYMENT_CONFIRMED` event 생성 확인

### 2) pay 멱등성 검증

- 동일 `(orderId, X-Idempotency-Key)`로 `pay` 재호출
- no-op 200 응답
- 기존 결과 재사용 확인

### 3) 배치 실행 → settlement 생성

- A2에서 `baseDate` 기준 배치 실행
- settlement 생성 확인
- 정합성 `OK / FAIL` 결과 확인

### 4) Hold 승인 / 해제

- A4에서 Hold 생성 진입
- A5에서 approve
- settlement 상태 `HOLD_ACTIVE` 확인
- request-paid 차단 확인
- release 후 `READY` 복구 확인

### 5) 4-Eyes 지급 검증

- request-paid
- 동일 Admin approve-paid 시도 → 409 확인
- 다른 Admin approve-paid → `PAID` 확인

### 6) 환불 승인 → 다음 배치 차감 반영

- Merchant 환불 요청
- Admin 승인
- 즉시 settlement 수정 없음 확인
- 다음 batch 실행 후 `REFUND` line 생성 확인
- `refund_settlement_link` 생성 확인

### 7) Trace / Audit 재현

- 운영 액션 이후 `request_id` 확인
- A1에서 동일 `request_id`로 조회
- audit_log + 관련 event 타임라인 재현 확인

---

## 12. ERD

SettleOps의 핵심 SoT는 아래 테이블을 중심으로 구성됩니다.

- `orders`
- `payment`
- `payment_event`
- `idempotency_record`
- `settlement_batch`
- `settlement`
- `settlement_line`
- `hold`
- `hold_event`
- `refund`
- `refund_event`
- `refund_settlement_link`
- `audit_log`

특히 환불 승인 사실과 정산 반영 완료 증거를 분리하기 위해  
`refund_settlement_link`를 별도 SoT로 두었습니다.

ERD 문서는 `docs/erd/` 경로를 참고합니다.

---

## 13. API Docs

상세 API 문서는 `docs/api/` 경로를 참고합니다.

---

## 14. Demo Video

- [SettleOps Demo Video](https://drive.google.com/file/d/1t3vDtS_M5B1EI2acZ0hZpwu4rb620kS-/view?usp=drive_link)

---

## 15. Proof Pack

주요 시나리오 캡처 및 증빙 자료는 `docs/proof-pack/` 경로를 참고합니다.

예상 포함 항목:

1. payment CAPTURED
2. pay 멱등 재시도
3. PAYMENT_CONFIRMED
4. batch run + OK/FAIL
5. batch 재실행 SKIP
6. Hold 생성 / 승인 / 해제
7. request-paid / approve-paid / 4-Eyes 409
8. REFUND line 차감정산
9. request_id 기반 Trace / Audit 재현

---

## 16. 레포 구조

```text
docs/         # 기획서 / ERD / API / ADR / Proof Pack / Runbook
server/       # Spring Boot 3.5.10 backend (Gradle)
admin-web/    # React 18 + Vite frontend
```

---

## 17. Troubleshooting

### Docker Compose가 정상 기동하지 않을 때

- Docker Desktop 실행 여부 확인
- MySQL / Redis 포트 충돌 여부 확인
- `docker compose ps` 로 컨테이너 상태 확인

### test-db 통합 테스트가 실패할 때

- Docker Compose의 MySQL이 기동 중인지 확인
- `settleops_test` DB 연결 정보 확인
- Flyway migration 적용 여부 확인

### CORS / 세션 쿠키 문제가 발생할 때

- 프론트 요청에 `credentials: "include"` 적용 여부 확인
- 서버 CORS에 `allowCredentials(true)` 및 허용 Origin 명시 여부 확인
- 브라우저 네트워크 탭에서 `Set-Cookie` / `Cookie` 전송 여부 확인

### request_id 추적이 안 될 때

- 응답 헤더의 `X-Request-Id` 확인
- event / audit insert 시 `request_id NOT NULL` 제약 위반 여부 확인
- A1 조회 시 `requestId` exact 검색으로 재현 가능한지 확인

### request-paid가 막힐 때

아래 운영 가드레일을 우선 확인합니다.

- `HOLD_ACTIVE`
- `SETTLEMENT_NOT_READY`
- `BATCH_FAILED`
- `REFUND_ADJUSTMENT_PENDING`

### approve-paid가 실패할 때

- 현재 settlement 상태가 `PAY_REQUESTED`인지 확인
- 요청자와 승인자가 동일한지 확인
- 동시성 구간이면 `IN_PROGRESS` 가능 여부 확인

---

## 문서 SoT

본 프로젝트의 최신 기준선은 아래 문서를 따릅니다.

- 기획 / 상태전이 / 도메인 규칙: `docs/`
- API 문서: `docs/api/`
- ERD 문서: `docs/erd/`
- Proof Pack: `docs/proof-pack/`
- Runbook: `docs/runbook/`
- DB / 마이그레이션 기준: `server`
- 운영 화면 기준: `admin-web`

팀 합의가 필요한 LOCKED / Freeze 규칙 변경은 합의 PR 1회로만 반영합니다.
