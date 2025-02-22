![logo](./logo.PNG)

## 트래블링독 백엔드 서버

-   Dockerfile로 빌드 및 이미지 생성
-   docker-compose.yml로 컨테이너 실행

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
