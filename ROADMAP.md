# 🥷 TerminalInNinja - Termius 스타일 개선 계획

---

## 🎨 1. UI/UX 재구성 (Termius 스타일)

### 1.1 네비게이션 구조 변경
**현재**: 사이드바 기반 (Hosts, Snippets, Terminal Tabs)
**변경**: 수평 네비게이션 탭 (2024년 Termius 스타일)

```
┌─────────────────────────────────────────────────────────────┐
│ 🏠 Hosts  📝 Snippets  📁 SFTP  ⚙️ Settings                │ ← 수평 네비게이션
├─────────────────────────────────────────────────────────────┤
│                                                            │
│  [Hosts View]  또는  [Snippets View]  또는  [SFTP View]    │
│                                                            │
└─────────────────────────────────────────────────────────────┘
```

### 1.2 Command Palette 추가
- 단축키: `CTRL+J` (Windows/Linux)
- 기능: 호스트 검색, 빠른 연결, 탭 전환
- UI: 검색 필드 + 퍼지 검색 결과 리스트

### 1.3 Horizontal Tabs 개선
- 탭바 상단 위치 (현재와 유사하지만 더 효율적인 스크롤)
- 탭 닫기 버튼 개선
- 탭 드래그앤드롭 순서 변경

### 1.4 Workspaces (Split View)
- 여러 탭을 그룹화
- Focus Mode: 하나의 터미널을 전체 화면으로
- Split View: 여러 터미널을 동시에 표시

---

## 💾 2. JSON 데이터 구조 개선

### 2.1 기존 구조 유지 + 확장
```json
{
  "hosts": [...],           // 기존 HostInfo
  "groups": [...],          // 기존 HostGroup
  "snippets": [...],        // 새로운 SnippetInfo
  "snippetPackages": [...], // 새로운 SnippetPackage
  "workspaces": [...],      // 새로운 Workspace
  "settings": {...}         // 기존 Settings 확장
}
```

### 2.2 Snippet 데이터 모델
```java
class SnippetInfo {
    String id;
    String name;
    String description;
    String script;           // 실행할 명령어
    String packageId;        // SnippetPackage 참조
    List<String> tags;       // 태그
    LocalDateTime createdAt;
}
```

### 2.3 SnippetPackage 데이터 모델
```java
class SnippetPackage {
    String id;
    String name;
    String icon;
    String color;
    LocalDateTime createdAt;
}
```

### 2.4 Workspace 데이터 모델
```java
class Workspace {
    String id;
    String name;
    List<String> tabIds;    // 포함된 탭 ID들
    String viewMode;        // "FOCUS" or "SPLIT"
    String splitDirection;  // "HORIZONTAL" or "VERTICAL"
}
```

---

## 🔧 3. 핵심 기능 완성

### 3.1 Snippets 기능
**우선순위: 높음**

**기능 목록**:
- [ ] Snippet 생성/편집/삭제
- [ ] SnippetPackage 생성/편집/삭제
- [ ] 터미널에서 Snippet 실행 (단축키 또는 메뉴)
- [ ] 여러 호스트에 동시 실행
- [ ] 연결 시작 시 자동 실행 (Host에 startupCommand 필드 추가)
- [ ] 퍼지 검색 지원

**구현 파일**:
- `model/SnippetInfo.java`
- `model/SnippetPackage.java`
- `controller/SnippetController.java`
- `fxml/SnippetView.fxml`
- `service/SnippetService.java`

### 3.2 SFTP 기능
**우선순위: 높음**

**기능 목록**:
- [ ] SFTP 탭 추가 (터미널 탭과 분리)
- [ ] 원격 파일 브라우저 (TreeView)
- [ ] 파일 업로드 (드래그앤드롭)
- [ ] 파일 다운로드
- [ ] 폴더 생성/삭제
- [ ] 파일 권한 변경

**구현 파일**:
- `service/SftpService.java` (JSch ChannelSftp 사용)
- `controller/SftpController.java`
- `fxml/SftpView.fxml`
- `model/RemoteFile.java`

### 3.3 Command Palette
**우선순위: 중**

**기능 목록**:
- [ ] 호스트 퍼지 검색
- [ ] 빠른 연결
- [ ] 열린 탭 전환
- [ ] Snippet 검색 및 실행

**구현 파일**:
- `controller/CommandPaletteController.java`
- `fxml/CommandPaletteView.fxml`
- `util/FuzzySearch.java`

### 3.4 Workspaces
**우선순위: 중**

**기능 목록**:
- [ ] 탭 그룹화 (드래그앤드롭)
- [ ] Focus Mode 전환
- [ ] Split View (수평/수직)
- [ ] 워크스페이스 저장/불러오기

**구현 파일**:
- `model/Workspace.java`
- `controller/WorkspaceController.java`
- `fxml/WorkspaceView.fxml`

---

## 🎯 4. 기존 기능 개선

### 4.1 터미널 에뮬레이션
- [ ] JediTerm 완전 통합 (현재는 TextArea 사용 중)
- [ ] ANSI 코드 정확한 처리
- [ ] 복사/붙여넣기 개선
- [ ] 스크롤 버퍼
- [ ] 마우스 지원 (선택, 터미널 어플리케이션)

### 4.2 호스트 관리
- [ ] 연결 테스트 기능
- [ ] 호스트 Import/Export
- [ ] 대량 편집
- [ ] 태그 시스템

### 4.3 SSH 연결
- [ ] Port Forwarding (로컬, 원격, 다이내믹)
- [ ] Jump Host 설정
- [ ] Proxy 지원
- [ ] SSH Certificate 지원

---

## 🔐 5. 키 관리 (Keychain)

**기능 목록**:
- [ ] SSH 키 저장소
- [ ] 키 생성 (RSA, ED25519)
- [ ] 키 Import/Export
- [ ] 비밀번호 암호화 저장

**구현 파일**:
- `model/SshKey.java`
- `controller/KeychainController.java`
- `service/KeychainService.java`
- `fxml/KeychainView.fxml`

---

## 🎨 6. 테마 및 설정

**설정 확장**:
- [ ] 커스텀 터미널 테마 (색상 팔레트)
- [ ] 폰트 설정 (굵기, 크기)
- [ ] 스크롤 버퍼 크기
- [ ] 단축키 설정
- [ ] 윈도우 투명도

---

## 📦 7. 배포 및 패키징

**Windows 패키징**:
- [ ] EXE 파일 생성 (Launch4j 또는 jpackage)
- [ ] 설치 프로그램 (Inno Setup)
- [ ] 자동 업데이트 기능
- [ ] 아이콘 및 브랜딩

---

## 🗂️ 최종 프로젝트 구조

```
src/main/java/com/ninja/terminal/
├── app/
│   └── MainApp.java
├── controller/
│   ├── MainController.java
│   ├── HostDialogController.java
│   ├── TerminalTabController.java
│   ├── SnippetController.java       [NEW]
│   ├── SftpController.java          [NEW]
│   ├── CommandPaletteController.java [NEW]
│   ├── WorkspaceController.java     [NEW]
│   ├── KeychainController.java      [NEW]
│   └── SettingsController.java      [NEW]
├── model/
│   ├── HostInfo.java
│   ├── HostGroup.java
│   ├── AppConfig.java
│   ├── SnippetInfo.java             [NEW]
│   ├── SnippetPackage.java          [NEW]
│   ├── Workspace.java               [NEW]
│   ├── SshKey.java                  [NEW]
│   └── RemoteFile.java              [NEW]
├── service/
│   ├── ConfigService.java
│   ├── SshService.java
│   ├── SnippetService.java          [NEW]
│   ├── SftpService.java             [NEW]
│   ├── WorkspaceService.java       [NEW]
│   └── KeychainService.java         [NEW]
├── util/
│   ├── FuzzySearch.java             [NEW]
│   └── AnsiUtils.java               [NEW]
└── view/
    ├── MainView.fxml
    ├── HostDialog.fxml
    ├── TerminalTab.fxml
    ├── SnippetView.fxml             [NEW]
    ├── SftpView.fxml                [NEW]
    ├── CommandPaletteView.fxml      [NEW]
    ├── WorkspaceView.fxml           [NEW]
    └── SettingsView.fxml            [NEW]

src/main/resources/
├── css/
│   ├── dark-theme.css
│   └── terminal-theme.css           [NEW]
├── fxml/
│   ├── MainView.fxml
│   ├── HostDialog.fxml
│   ├── TerminalTab.fxml
│   ├── SnippetView.fxml             [NEW]
│   ├── SftpView.fxml                [NEW]
│   ├── CommandPaletteView.fxml      [NEW]
│   ├── WorkspaceView.fxml           [NEW]
│   └── SettingsView.fxml            [NEW]
└── logback.xml
```

---

## ✅ 우선순위 순서

### 1단계 (필수)
1. Command Palette (빠른 호스트 검색/연결)
2. Snippets 기능 (기본 CRUD)
3. 네비게이션 구조 변경 (수평 탭)

### 2단계 (중요)
4. SFTP 기능 (기본 파일 전송)
5. JediTerm 통합 (진짜 터미널)
6. 키 관리 (Keychain)

### 3단계 (보너스)
7. Workspaces (Split View)
8. Port Forwarding
9. 호스트 Import/Export
10. 커스텀 테마

---

## 💡 기술적 고려사항

1. **JediTerm 통합**: 현재 TextArea 대신 JediTerm의 `JediTermWidget`을 JavaFX SwingNode로 래핑 필요
2. **동시성**: SFTP 및 멀티 탭 연결 시 스레드 관리 주의
3. **JSON 보안**: 비밀번호 암호화 필요 (AES-256)
4. **성능**: 호스트/스니펫이 많을 때 퍼지 검색 최적화
5. **호환성**: JSON 구조 변경 시 마이그레이션 스크립트 필요

---

## 📅 진행 상황

| 기능 | 상태 | 진행률 |
|------|------|--------|
| SSH 연결 | ✅ 완료 | 100% |
| 호스트 CRUD | ✅ 완료 | 100% |
| 그룹 관리 | ✅ 완료 | 100% |
| 멀티 탭 | ✅ 완료 | 100% |
| 검색 필터 | ✅ 완료 | 100% |
| 다크 테마 | ✅ 완료 | 100% |
| JediTerm 통합 | ✅ 완료 | 100% |
| Command Palette | ✅ 완료 | 100% |
| 버그 수정 (ENHANCEMENT-2) | ✅ 완료 | 100% |
| Snippets | ✅ 완료 | 100% |
| SFTP | ✅ 완료 | 100% |
| Settings UI | ✅ 완료 | 100% |
| Workspaces | ⏳ 대기 중 | 0% |
| Keychain | ⏳ 대기 중 | 0% |

---

## 📝 최근 개선 사항 (2025-01-10)

### ✅ 완료된 주요 기능

1. **JediTerm 통합 완료** (100%)
   - `TerminalTabController.java`: JediTermWidget 완전 통합
   - SwingNode를 통한 JavaFX 임베딩
   - 실제 터미널 에뮬레이션 동작 (ANSI 코드 지원)
   - 폰트 설정, 색상 팔레트 커스터마이징

2. **Command Palette 완료** (100%)
   - `CommandPaletteController.java`: 완전 구현
   - `FuzzySearch.java`: 퍼지 검색 알고리즘 구현
   - Ctrl+J 단축키로 빠른 호스트 검색 및 연결
   - 실시간 필터링 및 매칭 하이라이트

3. **Snippets 기능 완성** (100% 완료)
   - ✅ SnippetView UI 완성
   - ✅ SnippetController 구현
   - ✅ CRUD 기능 (생성/수정/삭제)
   - ✅ SnippetPackage 관리
   - ✅ 클립보드 복사 기능
   - ✅ **터미널에서 스니펫 실행 기능 구현**
     - 활성 터미널에 자동 실행
     - 여러 호스트에 동시 실행
     - 호스트 선택하여 연결 후 실행
   - ✅ **Startup Command 기능**
     - Host 모델에 startupCommand 필드 추가
     - 연결 시 자동으로 명령어 실행
     - Host Dialog에 UI 추가

4. **SFTP 기능 구현** (100% 완료)
   - ✅ **RemoteFile 모델** - 파일 정보 모델링
   - ✅ **SftpService** - JSch ChannelSftp 기반 SFTP 작업
     - 파일 목록 조회
     - 디렉토리 탐색
     - 업로드/다운로드
     - 파일/폴더 생성/삭제
     - 이름 변경
     - 권한 변경 (chmod)
   - ✅ **SftpView UI** - 파일 브라우저 인터페이스
     - 호스트 선택 및 연결
     - 파일 테이블 뷰
     - 툴바 (업로드/다운로드/새 폴더/삭제)
     - 경로 네비게이션
   - ✅ **SftpController** - UI 컨트롤러
     - 더블클릭으로 폴더 열기
     - 컨텍스트 메뉴 (다운로드/이름 변경/권한/삭제)
     - 진행 상태 표시
   - ✅ **CSS 스타일링** - 파일 타입별 아이콘 및 다크 테마

5. **Settings UI 기능 구현** (100% 완료)
   - ✅ **SettingsView.fxml** - 포괄적인 설정 UI
     - 스크롤 가능한 설정 패널
     - 섹션별 정리된 레이아웃 (Terminal, SSH Connection, Appearance)
     - 각 설정 항목에 대한 도움말 텍스트
     - Save/Reset 버튼
   - ✅ **SettingsController.java** - 설정 관리 로직
     - 터미널 설정 (폰트 패밀리, 폰트 크기, 스크롤 버퍼 크기)
     - SSH 연결 설정 (기본 포트, 연결 타임아웃)
     - 외관 설정 (테마 선택)
     - 유효성 검증이 포함된 Spinner 컨트롤
     - 10가지 공통 모노스페이스 폰트 제공
     - 기본값 복원 기능
   - ✅ **CSS 스타일링** - Settings 뷰에 대한 일관된 다크 테마
     - 설정 섹션 스타일
     - 커스텀 Spinner 화살표
     - 정보 박스 스타일링 (파란색 강조)
   - ✅ **MainView.fxml 업데이트** - Settings 탭 활성화

### 버그 수정 및 개선 (ENHANCEMENT-2)

1. ✅ **SnippetService 경로 통일**
   - `.ninja-terminal` → `.ninja-in-terminal` (ConfigService와 통일)

2. ✅ **FileChooser 안전 처리**
   - HostDialogController: `.ssh` 폴더 미존재 시 홈 디렉토리로 fallback

3. ✅ **Null Safety 개선**
   - MainController: `findHostsViewComponents()` 메소드에 타입 체크 추가
   - ClassCastException 방지를 위한 `instanceof` 패턴 적용

4. ✅ **SSH 에러 핸들링 강화**
   - TerminalTabController: 사용자 친화적인 에러 메시지 제공
   - 인증 실패, 타임아웃, 연결 거부 등 상세한 에러 메시지
   - disconnect() 메소드 안전성 개선

### 🚧 다음 작업

1. **Port Forwarding** (우선순위: 높음)
   - 로컬 포트 포워딩
   - 원격 포트 포워딩
   - 다이내믹 포트 포워딩
   - UI에서 포트 포워딩 관리

2. **Workspaces** (우선순위: 중)
   - 탭 그룹화
   - Split View (수평/수직)
   - Focus Mode
   - 워크스페이스 저장/불러오기

3. **Keychain** (우선순위: 낮음)
   - SSH 키 저장소
   - 키 생성 (RSA, ED25519)
   - 키 Import/Export
   - 비밀번호 암호화 저장

---

*마지막 업데이트: 2025-01-10*
