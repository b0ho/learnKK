# 아키텍처 — learnKK

## 전체 구조
모듈러 모놀리스 + 계약 우선(contract-first) + 3계층. monorepo 3영역:
- `/frontend` — React 18 + TypeScript + Vite, 모바일 우선 SPA. 하단 탭 네비(모임/내 러닝/쪽지/내정보).
- `/backend` — Spring Boot 3.x + Java 21 + Gradle. 3계층(Controller→Service→Repository), 패키지 루트 `com.learnkk`, 모듈: `kernel, auth, meeting, enrollment, content, messaging, session, survey`.
- `/contracts` — `openapi.yaml` 단일 API 계약(현재 `0.7.0-bolt7`).

데이터: PostgreSQL 16, Flyway 마이그레이션 V1~V8. 전부 로컬(docker-compose), 외부 SaaS 금지.

## 핵심 패턴
- **인증**: 세션 토큰(불투명 토큰, `sessions` 테이블) + `SessionAuthInterceptor`로 보호 경로 판정. RBAC(MENTOR/MENTEE/ADMIN).
- **에러**: 전역 `{code, message, details}` 스키마(`@RestControllerAdvice`), 코드 상수 `ErrorCodes`. FE는 `resolveErrorMessage`로 한글 매핑.
- **경계 규약**: JSON camelCase, JPA physical naming snake_case. Entity는 API 밖으로 노출 안 함(DTO만).
- **모임 상태전이**: `MeetingRepository.transitionStatus(id, from, to, reason)` 조건부 UPDATE(WHERE status=from)로 원자적 전이 — 유일한 전이 가드. 0행이면 `MEETING_INVALID_TRANSITION`.
- **크로스모듈 read 포트**: 테이블 직접 조인 대신 서비스 메서드(예: `EnrollmentService.isActiveParticipant`, `MeetingService.getMeeting`)로만 읽는다(ADR-007).
- **동시성**: 신청은 `pg_advisory_xact_lock(meetingId)`로 count-then-insert 직렬화.
- **출석 시간창**: 스케줄러리스(ADR-005). `windowEnd = scheduledAt + checkInWindowMinutes`, 요청 시점 비교. 세션 종료는 시간 파생(현재 상태 컬럼 없음).

## 상호작용 다이어그램

### 모임 라이프사이클 (관리자 승인 4지점)
```
멘토: 개설 ──> PENDING_APPROVAL
관리자 ①승인(T1) ──> RECRUITING ──(멘티 신청/취소)
관리자 모집확정(T3) ──> READY_TO_START
관리자 ②시작(T5) ──> IN_PROGRESS ──(세션·출석·설문·피드백)
관리자 ③완료(T6, 모든 세션 종료 게이트) ──> COMPLETED
관리자 ④수료확정 ──> 멘티 COMPLETED
반려(T2)──>REJECTED · 모집취소(T4)──>CANCELLED
```

### 출석 체크인 (시간창)
```
멘토 세션 생성(IN_PROGRESS) ──> meeting_session
멘티 [scheduledAt, scheduledAt+window] 내 checkIn ──> attendance(UNIQUE session,mentee 멱등)
출석율 a/S ──> a*100>=80*S 이면 수료 후보 ──> 관리자 ④확정
```

텍스트 대체: 위 흐름은 상태별 관리자/멘토/멘티 액션의 순차 전이를 나타낸다.

## 확장 지점(이번 버그픽스 관련)
- 역전이(되돌리기)는 `transitionStatus`의 from/to를 반대로 호출하면 원자적으로 안전.
- 세션 "완료 처리"는 시간 파생 종료를 명시 플래그로 보강해야 함(스키마 변경 수반 가능).
