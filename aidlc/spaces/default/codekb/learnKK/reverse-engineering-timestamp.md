# 리버스 엔지니어링 타임스탬프

## 수행 기록
- **일시**: 2026-08-27 (초점 최신성 검증 패스 — apply-button-state bugfix)
- **직전 전면 스캔**: 2026-08-24, 커밋 e427071
- **현재 커밋**: 805d312 (main)
- **저장소**: learnKK (단일 monorepo — frontend / backend / contracts)
- **프로젝트 유형**: Brownfield

## 분석 범위
- 이번 인텐트(apply-button-state bugfix)는 minimal-depth로, 대상 표면을 다음으로 한정해 현재 소스를 직접 확인함:
  - `frontend/src/features/meetings/MeetingListPage.tsx` (모집 목록 · 신청 버튼)
  - `frontend/src/api/{types,meetings,enrollments}.ts` (MeetingSummary FE 타입, listRecruiting, enrollments.mine/apply)
  - `backend/.../meeting/dto/MeetingSummary.java`, `meeting/service/MeetingService.java`
  - `backend/.../enrollment/service/EnrollmentService.java`, `enrollment/repository`(countByMeetingIdAndStatus)
- 전면 9종 산출물은 2026-08-24 스캔본을 유지(광역 드리프트는 다음 feature 스코프 인텐트에서 전면 재생성).

## 갱신 트리거
이후 도메인·계약·마이그레이션 변경 시 전면 재생성한다. 특히 architecture/component-inventory는 design-system·ux-bugfixes-2 변경(shadcn 컴포넌트 추가, 세션 완료 플래그 V9, 관리자 역전이 엔드포인트)을 아직 완전 반영하지 않았을 수 있으므로, 다음 feature/enterprise 스코프 실행 시 codekb 전면 재생성을 권장한다.
