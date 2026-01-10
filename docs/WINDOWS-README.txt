TerminalInNinja v1.0.0 - Windows Portable
============================================

사용법
-----
1. 압축 해제
   - 원하는 위치에 ZIP 파일 압축 해제

2. 바탕화면 단축아이콘 생성 (선택)
   방법 A: VBS 스크립트 사용
   - CreateShortcut.vbs 더블클릭

   방법 B: 앱 내부 메뉴 사용
   - Tools → Create Desktop Shortcut

3. 앱 실행
   - TerminalInNinja.exe 더블클릭

요구사항
-------
- Windows 10/11 64비트
- Java 설치 불필요 (내장 JRE 포함)
- 최소 512MB 여유 디스크 공간

첫 실행 시
----------
- 방화벽 허용 요청 → 허용 클릭
- SSH 호스트 정보 입력 필요
- 설정 파일은 사용자 폴더(~/.ninja-terminal)에 생성

기능
-----
- SSH 연결
- 다중 탭
- Command Palette (CTRL+J)
- Snippets (스크립트 저장/실행)
- JediTerm 터미널 (vim, htop, ls --color 지원)

단축아이콘 삭제
--------------
- DeleteShortcut.vbs 더블클릭

참고
----
- 폴더 이동 가능 (Portable)
- 관리자 권한 불필요
- 업데이트 시: 기존 폴더 삭제 후 새 버전 설치

문제 해결
----------
- 실행 안 됨: 바이러스 백신에서 앱이 차단될 수 있음 → 예외 추가
- 방화벽 경고: 첫 실행 시 한 번만 나옴 → 허용 클릭
- SSH 연결 실패: 호스트 정보 확인

지원
-----
GitHub: https://github.com/yourusername/ninja-in-terminal
버그 리포트: https://github.com/yourusername/ninja-in-terminal/issues
