# Code Summary — Bolt 3 Enrollment (learnKK / 런크크)

<!-- code-generation 산출물(developer 리드). Bolt 3 = U4 Enrollment. Brownfield: 신규 enrollment 모듈 + 시임 배선. git 브랜치 `bolt3`. 애플리케이션 코드는 워크스페이스 루트. -->

## 목표 달성

멘티 신청 → 현황 → 취소. 선착순 정원 무결성(overbooking 금지)을 어드바이저리 락으로 보장. Bolt 2가 남긴 U3 운영 허브 신청자 placeholder를 배선.

## 생성 파일

### 백엔드 (신규 `com.learnkk.enrollment` 모듈)
- `enrollment/domain/EnrollmentStatus.java`(APPLIED/CANCELLED)
- `enrollment/entity/Enrollment.java`(FK-by-id, `cancel()`)
- `enrollment/repository/EnrollmentRepository.java`(count/find + 네이티브 `lockMeeting` = `pg_advisory_xact_lock`)
- `enrollment/service/EnrollmentService.java`(apply/cancel/listApplicants/listMyEnrollments; MeetingService+UserRepository 의존)
- `enrollment/web/EnrollmentController.java`(4 라우트)
- `enrollment/dto/{EnrollmentResponse,ApplicantResponse}.java`
- `resources/db/migration/V4__enrollment.sql`(identity PK, FK ON DELETE CASCADE, status varchar+CHECK, **UNIQUE(meeting_id,mentee_id)**, 인덱스 2)
- 테스트: `EnrollmentServiceTest`(13), `EnrollmentControllerTest`(@WebMvcTest, 11), `integration/EnrollmentConcurrencyIntegrationTest`(2), `integration/EnrollmentIntegrationTest`(3)

### 프론트엔드
- `api/enrollments.ts`(+`.test.ts`)

## 수정 파일
- `kernel/error/ErrorCodes.java`(+6 ENROLLMENT_*), `kernel/security/Principal.java`(+`isMentee()`)
- `auth/web/SessionAuthInterceptor.java`(신규 4 라우트 보호 + Pattern 3)
- `contract/OpenApiContractTest.java`(+2), `contracts/openapi.yaml`(`0.3.0-bolt3`, enrollments tag, 4 paths, 스키마 3)
- FE: `api/{types,errors,index}.ts`, `MeetingListPage.tsx`(멘티 신청 버튼), `MyLearningPage.tsx`(멘티 현황+취소 / MentorHub 신청자 배선), `README.md`

## 주요 구현 결정
- **정원 무결성(BR-U4-1)**: `@Transactional` apply → 친절 중복 선검사 → `lockMeeting(meetingId)`(pg_advisory_xact_lock, 커밋까지 유지) → count(APPLIED) ≥ capacity → 409 ENROLLMENT_FULL → `saveAndFlush`. DataIntegrityViolation → 409 ENROLLMENT_DUPLICATE(unique 백스톱). U3 meeting 행 미잠금(getMeeting read).
- **cancel**: 본인 APPLIED만(그 외 404), status∈{RECRUITING,READY_TO_START}(그 외 409 CANCEL_FORBIDDEN), CANCELLED+cancelledAt. 취소분은 count 제외(빈자리 복귀). 재신청은 unique로 차단(설계 [assumption]).
- **listApplicants**: 소유 멘토(isMentor && mentorId==userId) 또는 ADMIN(403), 멘티 닉네임 U2 read.
- **FE**: 모집중 카드 멘티 신청 버튼(409→한국어), 멘티 현황(getMeeting 조합·취소 게이팅), MentorHub 신청자 표시(Bolt 2 placeholder 대체).

## 검증 결과
- **백엔드(비통합)**: BUILD SUCCESSFUL, 전부 통과. JaCoCo **line 89.5%**(564/630, ≥80% floor). Spotless/Checkstyle clean.
- **프론트엔드**: build 타입에러 0, **82 테스트/16 파일 통과**, coverage **line 95.31%**, lint 0.
- **라이브 E2E**: 아래 build-test-results(build-and-test 스테이지)에서 실제 앱+DB로 전이·**동시성(잔여 1석 병렬 신청)** 검증 예정/수행.

## 계획 대비 편차
- (환경) Testcontainers 통합 테스트 4건(신규 2 포함) 미실행 — Windows/Rancher docker-java JNA(Bolt 1/2 동일). 코드 결함 아님, 존치.
- 서브에이전트가 `frontend/aidlc/` 하위에 stray `.tsbuildinfo` 생성 → 제거함(레코드 루트 혼동 방지).

## Bolt 4+ 이월
- 멘티 현황 세션 일정(U5/Bolt 6) 조합, 사전설문 응답(U8/Bolt 7). U5~U9.
