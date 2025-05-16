![logo](./logo.PNG)

## 트래블링독 백엔드 서버

- Dockerfile로 빌드 및 이미지 생성
- docker-compose.yml로 컨테이너 실행

```bash
docker-compose up -d --build
```

---

# Azure의 도커 컴포즈 파일에 환경변수 주입하기

이 문서는 GitHub Actions를 통해 Azure VM에 배포할 때, GitHub Secrets에 정의된 환경변수(예: `POSTGRES_USER`, `POSTGRES_PASSWORD`)를 VM의 환경변수로 주입하고, 이를 Azure에 있는 Docker Compose 파일에서 읽어 PostgreSQL 컨테이너 등을 올바르게 초기화하는 방법을 설명합니다.

---

## 개요

프로젝트 배포 시 다음과 같은 과정이 진행됩니다:

1. **GitHub Secrets에 민감 정보 설정**

   - GitHub 저장소의 Secrets에 `POSTGRES_USER`와 `POSTGRES_PASSWORD` 등의 민감 정보를 안전하게 저장합니다.

2. **GitHub Actions 워크플로우에서 Azure VM 접속**

   - 배포 워크플로우(`deploy.yml`)에서 SSH를 이용해 Azure VM에 접속합니다.
   - 접속 직후, GitHub Secrets로부터 전달받은 값을 VM의 환경변수로 export 합니다.

3. **Docker Compose 파일에서 환경변수 사용**
   - Azure VM에 있는 Docker Compose 파일에서는 쉘 변수 표기법인 `${POSTGRES_USER}`와 `${POSTGRES_PASSWORD}`를 사용하여 환경변수를 참조합니다.
   - Docker Compose가 실행될 때, export 된 환경변수를 읽어서 PostgreSQL 등의 서비스에 전달합니다.

이렇게 하면 민감 정보를 코드나 Compose 파일에 직접 노출하지 않고, 동적으로 값을 주입할 수 있어 보안성과 유연성을 높일 수 있습니다.

---

## Azure VM의 Docker Compose 파일 설정

Azure VM에 배포할 때 사용하는 Docker Compose 파일(예: `docker-compose.development.yml`)은 아래와 같이 작성합니다.  
중요한 점은 GitHub Actions 변수 표기법 `${{ secrets.POSTGRES_USER }}` 대신, 쉘 변수 표기법인 `${POSTGRES_USER}`를 사용해야 한다는 점입니다.

---

# 🐕 트래블링독 (Traveling Dog) 프로젝트 문서

## 📋 요구사항 정의서

### 프로젝트 개요

트래블링독은 사용자 맞춤형 여행 계획을 AI 기반으로 제안하고 관리하는 서비스입니다. 사용자는 자신의 선호도와 관심사에 맞는 여행 계획을 생성하고, 저장하며, 공유할 수 있습니다.

### 기능 요구사항

#### 1. 사용자 관리

- **회원가입**: 이메일, 비밀번호, 닉네임을 입력하여 사용자 계정 생성
- **로그인**: 이메일과 비밀번호를 통한 인증
- **토큰 관리**: JWT 기반의 인증 및 인가 시스템 구현
- **프로필 관리**: 사용자 정보 조회 및 수정

#### 2. 여행 계획 관리

- **AI 기반 여행 계획 생성**:
  - 목적지, 날짜, 여행 스타일, 예산 등의 정보를 입력
  - AI(Gemini/GPT)를 통한 맞춤형 여행 계획 생성
- **여행 계획 조회**: 생성된 여행 계획 목록 및 상세 정보 조회
- **여행 계획 수정**: 생성된 계획 수정 가능
- **여행 계획 삭제**: 생성된 계획 삭제 가능
- **일정 관리**: 일별 여행 일정 관리 기능

#### 3. 여행 경험 공유

- **좋아요 기능**: 여행 계획에 대한 좋아요 표시
- **조회수 추적**: 여행 계획 조회수 집계

#### 4. 추천 시스템

- **맛집 추천**: 여행지별 맛집 추천 기능
- **숙소 추천**: 여행지별 숙소 추천 기능
- **AI 기반 여행 스타일 맞춤 추천**: 사용자의 여행 스타일, 관심사에 따른 추천

### 비기능 요구사항

#### 1. 보안

- JWT 기반 인증 시스템 구현
- 사용자 비밀번호 암호화 저장
- HTTPS 프로토콜 사용

#### 2. 성능

- 적절한 응답 시간 (여행 계획 생성은 최대 10초 이내)
- 동시 사용자 처리 능력

#### 3. 확장성

- 마이크로서비스 아키텍처 지향
- 컨테이너 기반 배포 환경

#### 4. 사용성

- RESTful API 설계
- 클라이언트 애플리케이션과의 원활한 통합

---

## 🏗️ 아키텍처 설계서

### 시스템 아키텍처 개요

트래블링독 백엔드는 Spring Boot 기반의 RESTful API 서버로, 사용자 인증, 여행 계획 관리, AI 기반 추천 기능을 제공합니다.

### 기술 스택

- **프레임워크**: Spring Boot
- **데이터베이스**: PostgreSQL (PostGIS 확장)
- **캐시 저장소**: Redis
- **인증**: JWT (JSON Web Token)
- **API 문서화**: Swagger/OpenAPI
- **AI 통합**: Google Gemini AI / OpenAI GPT
- **컨테이너화**: Docker, Docker Compose
- **CI/CD**: GitHub Actions

### 계층 구조

1. **Controller Layer**: 클라이언트 요청 처리 및 응답 반환
2. **Service Layer**: 비즈니스 로직 처리
3. **Repository Layer**: 데이터 접근 및 관리
4. **Model Layer**: 도메인 모델 정의

### 주요 모듈 구성

1. **인증 모듈**

   - AuthController: 회원가입, 로그인, 토큰 갱신 API 제공
   - AuthService: 사용자 인증 및 토큰 관리 비즈니스 로직

2. **여행 계획 모듈**

   - TravelPlanController: 여행 계획 CRUD API 제공
   - TravelPlanService: 여행 계획 관리 및 AI 통합 비즈니스 로직

3. **일정 활동 모듈**

   - ItineraryActivityController: 일정 활동 CRUD API 제공
   - ItineraryActivityService: 일정 활동 관리 비즈니스 로직

4. **AI 통합 모듈**
   - GptResponseHandler: AI 응답 처리 및 변환 로직

### 배포 아키텍처

- Docker 컨테이너화된 애플리케이션
- PostgreSQL 및 Redis 서비스와 함께 Docker Compose로 구성
- GitHub Actions를 통한 CI/CD 파이프라인
- Azure VM에 배포

### 보안 아키텍처

- JWT 기반 인증 시스템
- 환경 변수를 통한 민감 정보 관리
- HTTPS 통신

---

## 📊 ER 다이어그램 (Entity-Relationship Diagram)

```
  User
+-------+     1     +------------+
|       |<----------| TravelPlan |
+-------+           +------------+
    ^                     |
    |                     | 1
    |                     V
    |                +-----------+
    |                | Itinerary |
    |                +-----------+
    |                     |
    |                     | 1
    |                     V
    |               +---------------+
    |               |ItineraryActivity|
    |               +---------------+
    |
    |    +----------+
    |--->| PlanLike |
         +----------+
```

### 주요 엔티티 및 관계 설명

#### User (사용자)

- 속성: id, nickname, password, email, roles, preferredTravelStyle, favoriteDestinations
- 관계:
  - User는 여러 개의 TravelPlan을 가질 수 있습니다. (1:N)

#### TravelPlan (여행 계획)

- 속성: id, title, country, city, startDate, endDate, budget, transportationTips, viewCount, status, deletedAt
- 관계:
  - TravelPlan은 하나의 User에 속합니다. (N:1)
  - TravelPlan은 여러 개의 Itinerary를 가질 수 있습니다. (1:N)
  - TravelPlan은 여러 개의 TravelStyle, Interest, AccommodationType, Transportation을 가질 수 있습니다. (1:N)
  - TravelPlan은 여러 개의 RestaurantRecommendation, AccommodationRecommendation을 가질 수 있습니다. (1:N)
  - TravelPlan은 여러 개의 PlanLike를 가질 수 있습니다. (1:N)

#### Itinerary (일정)

- 속성: id, date, location
- 관계:
  - Itinerary는 하나의 TravelPlan에 속합니다. (N:1)
  - Itinerary는 여러 개의 ItineraryActivity를 가질 수 있습니다. (1:N)

#### ItineraryActivity (일정 활동)

- 속성: id, title, description, locationName
- 관계:
  - ItineraryActivity는 하나의 Itinerary에 속합니다. (N:1)

#### PlanLike (좋아요)

- 속성: id
- 관계:
  - PlanLike는 하나의 User에 속합니다. (N:1)
  - PlanLike는 하나의 TravelPlan에 속합니다. (N:1)

#### 부가 엔티티

- TravelStyle (여행 스타일): name
- Interest (관심사): name
- AccommodationType (숙소 유형): name
- Transportation (교통 수단): name
- RestaurantRecommendation (맛집 추천): locationName, description
- AccommodationRecommendation (숙소 추천): locationName, description

### 데이터베이스 특징

- PostgreSQL 기반으로 구현
- JPA/Hibernate를 사용한 ORM 매핑
- 감사(Auditing) 기능을 통한 생성/수정 시간 자동 기록
- 소프트 삭제(Soft Delete) 구현을 통한 데이터 보존
