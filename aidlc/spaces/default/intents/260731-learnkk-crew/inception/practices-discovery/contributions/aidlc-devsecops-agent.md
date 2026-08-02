**Collaborator:** aidlc-devsecops-agent

## Contribution

devsecops 렌즈로 리드 초안(team-practices.md, discovered-rules.md, evidence.md)을 **독립적으로** 검토했다. 확정 제약(전부 로컬·외부 SaaS/클라우드 미사용, 이번은 설계 전용, PII 최소·특정 규제 해당 없음)과 잠금 결정(비밀번호 해시, 히든 안티-중복계정 장치 — IP 등 신호 목적 한정·최소보관·비노출)을 기준선으로 삼았다. 리드가 클라우드 CD 기본값을 이 로컬·설계 워크플로우에서 걷어낸 판단, 배포/CI를 후속 구현 워크플로우로 이월한 판단은 타당하다. 클라우드 인프라(Security Hub/GuardDuty/Inspector/KMS/IAM 등)는 이 스코프 밖이므로 나는 그 방향으로 범위를 넓히지 않았고, **로컬 도구 + 설계 산출물이 상속할 보안 자세**에만 집중한다.

핵심 관찰:

- **Lint/format의 보안 측면** — 리드가 제안한 Prettier+ESLint(FE), Spotless/Checkstyle(BE)은 스타일용이며 보안 정적분석을 커버하지 않는다. 로컬에서 무료로 얹을 수 있는 보안 린트 계층이 별도로 필요하다. FE는 `eslint-plugin-security` / `eslint-plugin-react`(XSS·`dangerouslySetInnerHTML` 오용 탐지), BE는 **SpotBugs + FindSecBugs**(SQL injection, 약한 암호, 하드코딩 시크릿 탐지)가 대표적이며 전부 오프라인 로컬 실행 가능하다.
- **SAST/DAST 자세** — 이번 워크플로우는 코드가 없어 실제 스캔은 없다. 다만 이 자세는 team.md에 확정되어 후속 구현 워크플로우가 상속한다. SAST는 로컬형(SpotBugs/FindSecBugs, ESLint security, 선택적으로 Semgrep 로컬 룰셋)으로 병합 전 게이트에 넣을 것을 권고한다. 클라우드 SaaS(CodeGuru, Snyk 서버형)는 T3 로컬 제약과 충돌하므로 **채택하지 않는다**. DAST는 실행 가능한 앱을 전제로 하므로 이 설계 워크플로우 범위 밖이며, 후속 구현 워크플로우에서 로컬 기동된 앱 대상으로 도입할지만 남겨둔다.
- **시크릿 취급** — 로컬이라도 DB 비밀번호·세션 시크릿 등이 등장한다. `.env`/`application-local.properties`류를 repo에 커밋하지 않고 gitignore로 배제, 환경변수·로컬 설정으로 주입하는 원칙을 명시해야 한다. 리드 초안·discovered-rules에 시크릿 비커밋 규칙이 없어 이는 gap이다. 잠긴 결정인 비밀번호 해시도 알고리즘을 못박는 편이 좋다(Spring Security의 bcrypt/argon2 등 적응형 해시, 평문·가역 암호 금지).
- **의존성/공급망** — 리드 초안에 dependency scanning이 빠져 있다. FE는 `package-lock.json` 커밋 + 버전 핀 + `npm audit`(로컬), BE는 Maven/Gradle 중 택1 후 **OWASP Dependency-Check**(NVD 로컬 캐시로 오프라인 실행 가능) + 의존성 락(Gradle `dependencyLocking` 또는 Maven 버전 고정)을 권고한다. 모두 외부 SaaS 없이 로컬에서 돈다.
- **안티-중복계정 신호 처리의 컴플라이언스** — 잠긴 결정이지만 설계상 지켜야 할 세부(목적 한정·최소보관·비노출)가 team-practices에는 없다. 원본 IP를 그대로 저장하지 말고 salted hash 또는 부분 마스킹으로 저장, 명시적 TTL/보관 창 후 파기, UI·API 응답·로그·에러 메시지 어디에도 노출 금지, 접근은 안티-중복 판정 로직으로만 한정. 이 데이터 플로우와 보관 정책은 nfr-requirements/functional-design로 명시적으로 이월할 항목이다.

## Positions

구체 포지션/권고:

1. **discovered-rules에 시크릿 비커밋 hard rule 추가 권고** — `NEVER 비밀번호·DB 자격증명·세션 시크릿 등 비밀값을 repo에 커밋한다(로컬 설정/환경변수로 주입, `.env`류는 gitignore)`. 이는 사람 확정 없이도 secure-by-default에서 도출되는 안전한 기본값이다.
2. **비밀번호 해시 규칙 구체화 권고** — 기존 `ALWAYS 비밀번호 해시` 규칙을 "적응형 해시(bcrypt/argon2/scrypt), 평문·가역 암호 저장 금지"로 좁혀 확정. 알고리즘 선택은 인터뷰 확인 항목.
3. **보안 정적분석 계층을 Testing Posture/Code Style에 분리 명시 권고** — 스타일 린터와 별개로 SpotBugs+FindSecBugs(BE)·eslint-plugin-security(FE)를 병합 전 게이트로 두는 자세를 team.md에 확정. 전부 로컬 실행.
4. **의존성 스캔 자세를 Testing Posture에 추가 권고** — `npm audit`(FE) + OWASP Dependency-Check(BE) + lockfile 커밋/버전 핀. 후속 구현 워크플로우가 상속.
5. **안티-중복계정 신호의 보관·비노출 규약을 명시적 이월 항목으로 evidence에 기록 권고** — salted hash/부분 마스킹 저장, 명시적 보관 창 후 파기, 노출 금지, 접근 한정. nfr-requirements/functional-design로 전달.

인터뷰가 해결해야 할 gap(질문):

- **Q-SEC-1** 비밀번호 해시 알고리즘을 무엇으로 고정하나(bcrypt / argon2id / scrypt)? Spring Security `PasswordEncoder` 기본에 맡길지?
- **Q-SEC-2** 안티-중복계정 신호(IP 등)의 보관 형태(원본 vs salted hash vs 마스킹)와 보관 창(예: N일 후 파기)을 어떻게 정하나? 비노출 경계(로그·에러·관리자 화면 포함 여부)는?
- **Q-SEC-3** 로컬 시크릿 주입 방식을 표준화하나(`.env` + gitignore, Spring profile `application-local.properties`, docker-compose env)? 예시 시크릿 파일(`.env.example`)을 둘지?
- **Q-SEC-4** 보안 정적분석(SpotBugs+FindSecBugs, eslint-plugin-security)과 의존성 스캔(npm audit, OWASP Dependency-Check)을 이번 설계에서 team.md에 자세로 확정하고 후속 구현 워크플로우가 상속하게 할지?
- **Q-SEC-5** 관리자 4지점 승인·멘티 피드백 열람 등 권한 경계에 대해, 이번 설계 단계에서 인증/인가(role 기반 접근제어) 요구를 nfr-requirements로 넘길 항목으로 못박을지? (least-privilege·assume-breach 관점의 최소 기록)

범위 주의: 위 권고는 전부 로컬 도구·설계 산출물 상속에 한정하며, 클라우드 인프라·SaaS 보안 서비스로 확장하지 않는다(T3 준수).
