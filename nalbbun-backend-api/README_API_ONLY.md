# nalbbun-backend-api

이 프로젝트는 기존 STS Spring Boot 백엔드를 유지하면서 **API 제공 전용**으로 정리한 버전입니다.

## 원칙
- 기존 비즈니스 로직과 API는 유지
- 기존 관리자/채팅 HTML 화면은 제거
- 최소 UI는 API 목록 페이지(`/`, `/api-docs`)만 제공
- 실제 업무 화면은 별도 `nalbbun-frontend-web` 프로젝트에서 구동

## 실행 후 확인
- API 목록 페이지: `http://localhost:8080/`
- API 카탈로그 JSON: `http://localhost:8080/api/catalog`
