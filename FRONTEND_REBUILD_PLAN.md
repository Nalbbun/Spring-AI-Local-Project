# v1.0 프론트 재구성 적용 내역

## 적용 목적
기존 STS 기반 운영 화면의 흐름을 React v1.0 프론트에 다시 반영하여,
단순 JSON 확인형 화면이 아니라 운영자가 실제로 사용하는 콘솔형 UI로 복원한다.

## 페이지별 적용 범위

### 1. ChatWorkspace 공통 채팅 콘솔
- 프롬프트 선택 콤보 복원
- SSE 이벤트 기반 단계 요약 패널 추가
- 현재 대화 메모리 조회/초기화 패널 추가
- URL, 응답, 이벤트 로그를 운영 관점에서 재배치

### 2. AgentManagementPage
- 에이전트 현황 카드 복원
- 카테고리별 모델 배정 콤보 추가
- 웹 검색 테스트 / 에이전트 실행 테스트 / 작업 로그 통합
- 원본 설정 JSON 동시 노출

### 3. PromptManagementPage
- 저장소 요약 / 카테고리 필터 / 기본 프롬프트 지정 / 편집 흐름 재구성
- 연결된 채팅 화면으로 바로 이동할 수 있는 테스트 동선 추가
- 작업 로그 패널 추가

### 4. RagDocumentsPage
- 상태/DB/임베딩/인입/소스목록/작업로그 통합
- 텍스트/URL/단일파일/멀티파일 인입 흐름 정리
- 임베딩 설정과 후보 모델 노출

### 5. RagSearchTestPage
- 공통 채팅 콘솔을 기반으로 검색 테스트 흐름 유지
- 임베딩 설정/후보 모델 동시 노출

### 6. KeyManagementPage
- Provider 현황 + 저장 키 목록 + 편집 + 이벤트 로그 흐름 재구성

## 실무 기준 컴포넌트 분해 단위
- ChatWorkspace: 질문/프롬프트/메모리/SSE 로그 공통 운영 컴포넌트
- AppCard: 섹션 카드 공통 래퍼
- LogPanel: 페이지별 작업 이력 누적 패널
- DataTable: 목록/관리 표 공통 컴포넌트
- useEventLog: 페이지별 localStorage 기반 운영 로그 관리

## 필요한 API 매핑 기준
- `/api/chat/stream`
- `/api/prompt-entries`
- `/api/memory/*`
- `/debug/api/memory/clear`
- `/debug/api/ollama/*`
- `/debug/api/rag/*`
- `/api/agent/execute`
- `/api/search/web`
- `/api/api-keys/*`

## 검증 결과
- TypeScript 빌드(`tsc -b`) 통과
- Vite 번들은 현재 업로드된 `node_modules`의 Rollup optional dependency 누락으로 미실행
  - 오류: `@rollup/rollup-linux-x64-gnu` 누락
  - 소스 자체의 TypeScript 컴파일 오류는 없음
