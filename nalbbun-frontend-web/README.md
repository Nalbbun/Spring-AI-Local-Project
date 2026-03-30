# nalbbun-frontend-web

백엔드와 분리된 프론트엔드 프로젝트입니다.

## 목표
- 기존 Spring Boot 템플릿 화면을 더 이상 주 화면으로 쓰지 않음
- React + TypeScript + Vite 기반으로 화면을 독립 관리
- 민감정보(API Key, DB 접속 정보, 서버 경로)를 프론트에 두지 않음
- 백엔드 API(`/api`, `/debug`)를 호출해서만 데이터 표시

## 실행
```bash
npm install
npm run dev
```

기본 개발 서버는 `http://localhost:5173` 입니다.
백엔드는 `http://localhost:8080` 에서 실행 중이라고 가정하고 Vite proxy를 설정해 두었습니다.
