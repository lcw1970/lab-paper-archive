# Windows 랩 PC 배포 준비 (Tailscale)

이 서비스는 랩 PC의 `localhost:8080`만 사용합니다. 외부 기기는 Tailscale Serve가 제공하는 HTTPS 주소로만 접속합니다. 공유기 포트 포워딩은 하지 마세요.

## 1. PC 사전 조건

- Windows 10 22H2 이상 또는 Windows 11, 64비트
- BIOS/UEFI의 CPU 가상화(Intel VT-x / AMD-V) 활성화
- RAM 8GB 이상, 논문과 백업을 위한 충분한 D: 드라이브 여유 공간
- Windows 전원 설정에서 절전 모드와 최대 절전 모드를 `안 함`으로 변경
- 랩 PC용 별도 Windows 관리자 계정 하나를 정해 Docker Desktop과 Tailscale을 그 계정에서 관리

## 2. 폴더 만들기

관리자 PowerShell에서 아래를 한 번 실행합니다.

```powershell
New-Item -ItemType Directory -Force `
  D:\lab-paper-archive\papers, `
  D:\lab-paper-archive\postgres, `
  D:\lab-paper-archive\logs, `
  D:\lab-paper-archive\backups, `
  D:\lab-paper-archive\app
```

`papers`와 `postgres`는 절대 지우지 마세요. Docker 컨테이너를 재생성해도 논문과 DB가 남는 영구 저장소입니다.

## 3. WSL 2 및 Docker Desktop 설치

관리자 권한 PowerShell에서 실행하고 재부팅합니다.

```powershell
wsl --install
wsl --update
```

그 다음 Docker Desktop for Windows를 설치합니다. 설치 화면에서 **Use WSL 2 instead of Hyper-V**를 선택하고, 실행 후 Settings > General에서 **Use the WSL 2 based engine**이 활성화됐는지 확인합니다.

Docker Desktop Settings > General에서 다음도 켭니다.

- Start Docker Desktop when you sign in to your computer
- Use the WSL 2 based engine

PowerShell에서 확인합니다.

```powershell
docker version
docker compose version
```

## 4. Tailscale 설치 및 네트워크 설정

1. Windows용 Tailscale을 설치합니다.
2. 작업 표시줄 오른쪽의 Tailscale 아이콘을 열고, 본인의 Tailscale 계정으로 로그인합니다.
3. Mac에도 같은 계정으로 Tailscale을 로그인합니다.
4. Tailscale 관리자 콘솔의 Machines에서 랩 PC 이름을 `paper-archive-server`처럼 식별하기 쉽게 바꿉니다.
5. 관리자 콘솔의 DNS에서 MagicDNS와 HTTPS certificates를 활성화합니다. 인증서 설정은 Tailnet 도메인 및 기기 이름이 공개 인증서 투명성 로그에 기록될 수 있다는 안내를 확인한 뒤 진행합니다.

모든 랩 구성원이 접속해야 한다면, 각 구성원의 기기에도 Tailscale을 설치하고 관리자 콘솔에서 승인합니다. 모르는 기기나 더 이상 사용하지 않는 기기는 Machines에서 제거합니다.

## 5. 배포 파일과 비밀값 준비

이 프로젝트 전체를 `D:\lab-paper-archive\app`에 복사하거나 Git으로 clone합니다. 그 폴더에서 `.env.example`을 `.env`로 복사해 실제 값을 입력합니다.

```powershell
Set-Location D:\lab-paper-archive\app
Copy-Item .env.example .env
notepad .env
```

`.env`의 예시는 다음처럼 설정합니다. 비밀번호는 각각 길고 서로 다른 무작위 값으로 바꾸세요.

```dotenv
POSTGRES_DB=papers
POSTGRES_USER=papers
POSTGRES_PASSWORD=replace-with-a-long-random-db-password

ADMIN_EMAIL=your-email@univ.ac.kr
ADMIN_PASSWORD=replace-with-a-long-random-admin-password

HOST_STORAGE_PATH=D:/lab-paper-archive/papers
HOST_DB_PATH=D:/lab-paper-archive/postgres
HOST_LOG_PATH=D:/lab-paper-archive/logs
```

`DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`, `STORAGE_ROOT`, `SPRING_PROFILES_ACTIVE`는 compose 파일이 자동 설정하므로 바꾸지 않습니다. `.env`는 Git에 올리거나 메신저로 공유하지 마세요.

## 6. 앱 시작과 확인

```powershell
Set-Location D:\lab-paper-archive\app
docker compose --env-file .env -f docker/compose.prod.yml up -d --build
docker compose --env-file .env -f docker/compose.prod.yml ps
docker compose --env-file .env -f docker/compose.prod.yml logs -f app
```

앱이 정상 기동하면 브라우저에서 랩 PC의 `http://127.0.0.1:8080`으로 먼저 로그인 화면이 나오는지 확인합니다. 이 주소는 랩 PC에서만 동작해야 정상입니다.

## 7. Tailscale HTTPS 연결

PowerShell에서 아래 명령을 실행합니다. 처음 실행 시 HTTPS 사용 승인을 위한 링크가 표시되면 브라우저에서 승인합니다.

```powershell
tailscale serve --https=443 http://127.0.0.1:8080
tailscale serve status
```

출력되는 `https://paper-archive-server.<tailnet>.ts.net` 주소를 Mac이나 승인된 다른 기기에서 열어 로그인합니다. 이 주소가 운영 주소입니다. `tailscale funnel`은 인터넷 전체에 공개하므로 사용하지 않습니다.

## 8. 배포 후 필수 확인

1. Mac의 Tailscale 연결 상태에서 운영 HTTPS 주소로 로그인한다.
2. PDF 업로드, 한 건 다운로드, 여러 건 ZIP 다운로드를 각각 확인한다.
3. 랩 PC를 재부팅하고 Windows 로그인 후 Docker Desktop과 앱이 자동 기동하는지 확인한다.
4. `docker compose ... ps`에서 앱과 DB가 `Up`인지 확인한다.
5. DB와 `papers` 폴더를 모두 포함한 백업을 만든 뒤, 별도 위치에서 복구 가능 여부를 점검한다.

## 운영 중 자주 쓰는 명령

```powershell
# 상태와 로그
docker compose --env-file .env -f docker/compose.prod.yml ps
docker compose --env-file .env -f docker/compose.prod.yml logs -f app

# 코드 업데이트 후 재배포
docker compose --env-file .env -f docker/compose.prod.yml up -d --build

# 중지 (데이터를 지우지 않음)
docker compose --env-file .env -f docker/compose.prod.yml down
```

`down -v`, Docker Desktop의 Reset to factory defaults, `D:\lab-paper-archive\postgres`와 `papers` 폴더 삭제는 데이터 삭제로 이어질 수 있으니 실행하지 마세요.
