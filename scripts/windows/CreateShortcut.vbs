Set WshShell = WScript.CreateObject("WScript.Shell")
strDesktop = WshShell.SpecialFolders("Desktop")

' 현재 스크립트의 경로를 기준으로 실행 파일 경로 찾기
strPath = WScript.ScriptFullName
strExePath = Left(strPath, InStrRev(strPath, "\") - 1) & "\NinjaInTerminal.exe"

' 바탕화면 단축아이콘 생성
Set oShortcut = WshShell.CreateShortcut(strDesktop & "\NinjaInTerminal.lnk")
oShortcut.TargetPath = strExePath
oShortcut.WorkingDirectory = Left(strPath, InStrRev(strPath, "\") - 1)
oShortcut.Description = "SSH Terminal Client"
oShortcut.Save

MsgBox "바탕화면에 단축아이콘이 생성되었습니다!", 0, "NinjaInTerminal"
