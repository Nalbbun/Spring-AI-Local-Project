# Package Refactor Guide

## 목표
기존 단일 Spring Boot 프로젝트를 도메인 중심 패키지 구조로 재배치하고, 프론트엔드/DB 자원 분리 개념을 반영한다.

## 최상위 구조
- `backend-api`: 실제 실행 모듈
- `frontend-web`: 프론트 분리 대상 리소스 참조
- `db-resources`: DB 스키마 및 migration 자원
- `docs`: 개편 문서

## 백엔드 패키지 기준
- `ai.local.nalbbun.api`: 사용자 공개 API
- `ai.local.nalbbun.admin`: 운영/디버그/API 페이지
- `ai.local.nalbbun.domain.chat`: 채팅 오케스트레이션
- `ai.local.nalbbun.domain.category`: 카테고리 분류 및 핸들러
- `ai.local.nalbbun.domain.runtime`: 모델 선택/실행 정책
- `ai.local.nalbbun.domain.prompt`: 프롬프트 저장/조합
- `ai.local.nalbbun.domain.memory`: 대화 메모리
- `ai.local.nalbbun.domain.rag`: RAG 서브시스템
- `ai.local.nalbbun.domain.search`: 외부 검색
- `ai.local.nalbbun.domain.conversation`: conversation id / 세션 처리
- `ai.local.nalbbun.infra.security.apikey`: API Key 저장/암복호화
- `ai.local.nalbbun.common.sse`: 공통 SSE 유틸/이벤트
- `ai.local.nalbbun.config`: 스프링 설정

## 참고
이번 개편본은 기존 소스를 최대한 유지하며 패키지/디렉터리 책임을 재배치한 1차 구조화 결과이다.
세부 클래스명 정리, Controller 세분화, Prompt/Memory/RAG 서비스 계층 재명명은 다음 단계에서 추가로 다듬는 것이 적절하다.
