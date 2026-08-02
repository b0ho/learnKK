**Collaborator:** aidlc-quality-agent

## Contribution

품질/테스팅 관점에서 리드 초안의 **Testing Posture**를 독립적으로 검토했다. 이번 워크플로우가 설계 전용(코드·CI 없음)이라는 제약을 전제로, 여기서 확정되는 testing posture는 실행되지 않지만 팀 3인의 개별 구현 워크플로우가 **상속하는 계약**이므로 지금 정확히 고정해두는 것이 이후 리스크를 줄인다는 점을 강조한다.

### 테스트 프레임워크 스택 적합성

리드가 제안한 프레임워크 조합은 스택에 정확히 부합하며 지지한다:

- **백엔드 (Java Spring)** — JUnit 5(Jupiter)를 기본으로, 슬라이스 단위 테스트에 Spring Boot Test, 웹 계층에 **MockMvc**(`@WebMvcTest`), 서비스 계층 격리에 Mockito. 이는 Spring 생태계 표준이고 로컬 실행과 잘 맞는다.
- **프론트엔드 (React)** — 러너는 **Vitest 또는 Jest 중 택1**, 컴포넌트 테스트는 **React Testing Library**, 사용자 상호작용은 `@testing-library/user-event`. Vite 기반 빌드라면 Vitest가 설정·속도 면에서 더 자연스럽고, CRA/webpack 기반이면 Jest가 무난하다 — 이 선택은 프론트엔드 빌드 도구가 확정된 뒤 결정하는 것이 옳다.

**핵심 갭:** 두 개의 별개 스택(React + Spring)이 로컬에서 상호작용하는 구조인데, 초안은 단위 테스트 프레임워크만 짚고 **API 계약(contract) 테스트 계층을 다루지 않는다.** team-assessment가 지목한 최대 리스크가 "단위 간 interface 불일치"인 만큼, 프론트-백엔드 경계의 계약 검증(예: OpenAPI 스키마 기반 검증, 또는 백엔드 MockMvc로 응답 스키마 고정)이 test posture에 명시되어야 리스크가 실제로 완화된다.

### 통합 테스트와 로컬 PostgreSQL

초안은 "통합 테스트는 로컬 PostgreSQL 대상"이라고만 적었는데, 이는 재현성·독립성 원칙(테스트는 순서·공유 상태에 의존하면 안 됨)과 충돌할 소지가 있다. 개발자 3인이 각자 로컬 DB를 쓰면 스키마 드리프트와 "내 머신에선 통과" 문제가 생긴다. 전부-로컬 제약(외부 SaaS·클라우드 금지) 하에서도 **Testcontainers(로컬 Docker로 격리된 PostgreSQL 기동)** 또는 H2 인메모리는 로컬 도구이므로 제약을 위반하지 않는다. 다만 안티-중복계정 로직처럼 PostgreSQL 고유 기능에 의존하는 부분은 H2로는 검증이 부정확하므로 실 PostgreSQL(Testcontainers) 쪽이 안전하다. Deployment 섹션의 docker-compose 표준화 논의와 이 결정을 묶어야 한다.

### Coverage 80% floor

org.md의 `feature`/`mvp` 계열 80% line coverage floor는 이 프로젝트에 합리적이며 **유지**를 권한다. 다만 두 가지를 명확히 해야 한다: (1) floor를 **프론트/백엔드에 각각** 적용할지 통합 지표로 볼지 — 스택이 갈라지므로 각각 별도 측정을 권한다(백엔드 JaCoCo, 프론트 Vitest/Jest `--coverage`). (2) line coverage만으로는 조건 분기(관리자 4지점 승인, 출석률 80% 자동 수료 판정 같은 규칙 로직)를 놓치므로, 이 도메인 규칙에 대해서는 **line이 아닌 branch coverage 관점의 시나리오 테스트**를 별도로 요구하는 것이 의미 있다. Key Principle대로 coverage는 목표가 아니라 가이드다.

### CI 품질 게이트 (설계 전용 유의)

이번 워크플로우는 설계 전용이라 게이트가 실행되지 않는다는 리드의 프레이밍은 정확하다. 다만 team.md에 상속되는 **게이트 정의 자체는 지금 확정**해 둘 가치가 있다: "병합 전 테스트 통과 + coverage floor 충족"이라는 게이트 규칙을 구현 워크플로우가 물려받도록. 실행 플랫폼(GitHub Actions 등)은 미정으로 두되, 게이트의 **의미(pass/fail 기준)** 는 posture에 박아두는 것을 권한다.

### 도메인 규칙의 테스트 가능성 (shift-left)

project.md의 Decided 항목들(비밀번호 해시 저장, 히든 안티-중복계정 신호, 관리자 4지점 승인, 출석률 80% 자동 수료 판정, 멘토 자유 설문 문항)은 모두 **명확한 검증 가능 조건을 가진 테스트 대상**이다. 특히 출석률 80% 경계(79%/80%/81%)와 4개 승인 지점의 상태 전이는 경계값·에러 경로 테스트의 1급 후보다. 이 항목들이 이후 user-stories의 acceptance criteria로 내려갈 때 테스트 가능한 형태로 서술되도록, testing posture가 "acceptance criteria를 직접 검증하는 테스트를 작성한다"는 원칙을 명시하기를 권한다.

## Positions

1. **프레임워크 조합 지지, 단 러너 확정은 빌드 도구에 종속.** 백엔드 JUnit 5 + Spring Boot Test + MockMvc + Mockito를 확정으로 권고. 프론트엔드는 RTL을 확정하되 Vitest vs Jest는 프론트엔드 빌드 도구(Vite/webpack)가 정해진 뒤 결정.
2. **계약(contract) 테스트 계층을 posture에 추가하라.** 프론트-백엔드 interface 불일치가 최대 리스크이므로, API 응답 스키마 고정(OpenAPI 또는 MockMvc 스키마 검증)을 단위/통합과 별도로 명시.
3. **통합 테스트 DB는 Testcontainers(로컬 Docker PostgreSQL) 권장.** 전부-로컬 제약을 위반하지 않으면서 재현성·독립성을 확보. Deployment의 docker-compose 표준화 결정과 묶어서 판단.
4. **80% line coverage floor 유지.** 단 프론트/백엔드 **각각** 측정(JaCoCo / Vitest·Jest coverage), 그리고 도메인 규칙 로직은 branch/시나리오 커버리지로 별도 보강.
5. **CI 게이트의 실행 플랫폼은 미정으로 두되, 게이트의 pass/fail 의미(테스트 통과 + coverage 충족)는 team.md에 지금 고정**하여 구현 워크플로우가 상속하게 한다.

**인터뷰가 물어야 할 질문:**

- Q1. 프론트엔드 빌드 도구는 Vite인가 webpack/CRA인가? (Vitest vs Jest 결정의 선행 조건)
- Q2. 80% coverage floor를 프론트/백엔드 각각에 적용하는가, 통합 지표로 보는가? floor를 유지·상향·하향할 것인가?
- Q3. 테스트 방법론은 TDD / BDD / test-after 중 무엇인가? (org 기본은 "코드와 함께 작성"까지만 명시)
- Q4. 통합 테스트의 PostgreSQL을 Testcontainers(로컬 Docker)로 격리할 것인가, 개발자 각자의 로컬 인스턴스를 공유할 것인가?
- Q5. 프론트-백엔드 API 계약을 무엇으로 고정할 것인가(OpenAPI 스펙 / 계약 테스트 / 문서만)? 계약의 소유자는 누구인가?
- Q6. "병합 전 테스트 통과 + coverage 충족"이라는 게이트 규칙을 team.md에 상속 계약으로 박아둘 것인가, 아니면 구현 워크플로우에 전적으로 위임할 것인가?
