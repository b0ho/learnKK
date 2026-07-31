# Intent Backlog — learnKK (런크크)

<!-- 우선순위화된 proto-Unit 백로그(MoSCoW + dependency-first). units-generation의 입력. 출처: scope-document, intent-statement, feasibility-assessment, constraint-register. -->

의존성 우선 순서로 배열한 proto-Unit(논리 단위 후보) 목록. units-generation에서 정식 Unit으로 정제된다.

| # | Proto-Unit | 설명 | MoSCoW | 의존 |
|---|-----------|------|--------|------|
| 1 | 회원·인증·프로필 | 닉네임+비밀번호 가입(승인 없음), 비밀번호 해시, 프로필(관심사 해시태그·한 줄 소개), 히든 IP 중복방지 | Must | — |
| 2 | 모임(러닝 크루) 개설·관리 | 멘토가 주제·학습기간·모집기간·정원·진행방식·학습자료·공지 입력, 모임 상태 관리 | Must | 1 |
| 3 | 모임 승인·관리자 감독 | 관리자 검토·승인 → 정식 모임 전환 | Must | 2 |
| 4 | 모집·신청·멘티 설문 | 모집기간 내 멘티 신청 + 기본 설문, 모집 마감 시 인원 확정 | Must | 3 |
| 5 | 모임 목록·탐색 | 메인 모임 목록, 상세 조회(역할별 노출) | Must | 2 |
| 6 | 주차별 진행: 자료실·공지 | 주차별 학습자료 업로드, 공지사항 | Must | 4 |
| 7 | 쪽지(메시징) | 멘토 ↔ 멘티, 관리자 ↔ 멘토/멘티 소통 | Must | 4 |
| 8 | 출석 | 멘토 모임 시작/종료, 멘티 출석 체크, 주차별 출석 기록 | Must | 4 |
| 9 | 수료 판정·완료 인정 | 출석율 80% 기반 멘티 수료 판정, 멘토 완료 인정 | Must | 8 |
| 10 | 설문·피드백 | 과정 종료 후 멘티 설문, 멘토의 멘티 피드백 확인 | Must | 9 |
| 11 | 관리자 모니터링 | 전체 모임 현황 대시보드(진행/출석/수료). 관리자의 특이사항 소통은 쪽지(#7)로 처리 | Must | 3,8,9 (cross-cutting) |

## 화면(3탭) 매핑 (intent 메인 구성)

- 탭1 **모임 목록** — proto-Unit 5, 2
- 탭2 **역할별 화면** — 멘토: 내 모임 이력 / 멘티: 참여 중 모임 / 관리자: 모니터링(11)
- 탭3 **내정보** — proto-Unit 1

## Assumptions & Open Questions

- proto-Unit 경계는 units-generation에서 확정되며, 병합/분할될 수 있다.
- Could 항목은 두지 않는다 — 이번 범위는 Must proto-Unit(1~11)만 다룬다.
