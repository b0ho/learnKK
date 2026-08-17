# Unit of Work Dependency — learnKK (런크크)

<!-- units-generation 산출물. Unit 간 의존 DAG(위상만) + 통합점 + 병렬 기회 + 기계판독 yaml edge block. 출처: unit-of-work.md, application-design(component-dependency), team-practices(3계약). 빌드 순서·크리티컬 패스는 delivery-planning 소관 — 여기서 정하지 않음. -->

## 의존 DAG (A depends on B: A는 B가 먼저 있어야 구현 통합 가능)

<!-- Text fallback: U1이 뿌리(계약·커널). U2는 U1에 의존. U3은 U1,U2. U4/U6/U7은 U1,U2,U3. U5는 U1,U2,U3,U4. U8은 U1,U2,U3,U4. U9는 U1,U2,U3,U4,U5,U8. 사이클 없음. -->

```
U1 (Contracts&Kernel)
└─ U2 (Auth & App Shell)
   └─ U3 (Meeting)
      ├─ U4 (Enrollment)
      │   ├─ U5 (Session/Attendance)
      │   └─ U8 (Survey/Feedback)
      ├─ U6 (Content)
      └─ U7 (Messaging)
   U9 (Admin/Monitoring) ← U3,U4,U5,U8 (+U1,U2)
```

(위 트리는 주요 의존 경로 시각화이며, 각 Unit의 정확한 depends_on은 아래 edge block이 정본. U9는 다중 부모라 트리로는 완전 표현 불가 — edge block 참조.)

## Unit 간 통합점(계약)

- **#1 OpenAPI(REST):** 모든 도메인 Unit(U2~U9)의 클라이언트↔서버 경계. U1이 스펙 소유·선고정, 각 Unit이 자기 엔드포인트를 스펙에 맞춰 구현.
- **#2 DB 스키마:** 각 Unit이 자기 테이블 소유(U1 baseline + Flyway). 교차 접근은 소유 Unit Service 경유.
- **#3 도메인 타입(C0):** 모임 상태·수료 enum·에러·RBAC — U1 소유, 전 Unit 참조(컴파일 공유, 리프).
- **read 상호참조:** U3↔U4(모집중·정원/신청자), U3↔U5(진행중 상태/전 세션 종료 — ③완료 전제조건) → read 포트/컨트롤러 조합(application-design ADR-007). 쓰기 전이 순환 없음.

## 병렬 개발 기회 (순서 강제 아님)

- U1 → U2는 순차(계약→인증·셸 기반).
- U2 이후 **U3**가 공통 도메인 허브.
- **U3 완료 후 U4·U6·U7 상호 독립 → 병렬 가능.**
- U5는 U4 이후, U8은 U4 이후(서로 독립 → 병렬 가능).
- U9는 U3/U4/U5/U8에 의존 → 상대적으로 후행.
- (실제 빌드 순서·동시 배치·크리티컬 패스는 delivery-planning이 이 DAG를 입력으로 결정.)

## 기계판독 Edge Block (정본)

```yaml
units:
  - name: U1-contracts-kernel
    kind: spec
    depends_on: []
  - name: U2-auth-shell
    kind: service
    depends_on: [U1-contracts-kernel]
  - name: U3-meeting
    kind: service
    depends_on: [U1-contracts-kernel, U2-auth-shell]
  - name: U4-enrollment
    kind: service
    depends_on: [U1-contracts-kernel, U2-auth-shell, U3-meeting]
  - name: U5-session-attendance
    kind: service
    depends_on: [U1-contracts-kernel, U2-auth-shell, U3-meeting, U4-enrollment]
  - name: U6-content
    kind: service
    depends_on: [U1-contracts-kernel, U2-auth-shell, U3-meeting]
  - name: U7-messaging
    kind: service
    depends_on: [U1-contracts-kernel, U2-auth-shell, U3-meeting]
  - name: U8-survey-feedback
    kind: service
    depends_on: [U1-contracts-kernel, U2-auth-shell, U3-meeting, U4-enrollment]
  - name: U9-admin-monitoring
    kind: service
    depends_on: [U1-contracts-kernel, U2-auth-shell, U3-meeting, U4-enrollment, U5-session-attendance, U8-survey-feedback]
```

## 비순환 확인

- 위상 순서 존재: U1 → U2 → U3 → (U4, U6, U7) → (U5, U8) → U9. 모든 edge가 이 순서를 거스르지 않음 → **DAG(사이클 없음)**.
- 자기참조 없음, 모든 depends_on 대상이 선언된 Unit.

## Assumptions & Open Questions

- read 상호참조(U3↔U4, U3↔U5)의 물리 배치(read 포트 vs 컨트롤러 조합)는 functional-design 확정(ADR-007) — 쓰기 의존이 아니라 DAG 순환 아님.
- 빌드 순서·병렬 배치·MVP 경계는 delivery-planning.
