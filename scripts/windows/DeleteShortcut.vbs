Set WshShell = WScript.CreateObject("WScript.Shell")
strDesktop = WshShell.SpecialFolders("Desktop")
strShortcut = strDesktop & "\NinjaInTerminal.lnk"

Set fso = CreateObject("Scripting.FileSystemObject")

If fso.FileExists(strShortcut) Then
    fso.DeleteFile strShortcut
    MsgBox "바탕화면 단축아이콘이 삭제되었습니다.", 0, "NinjaInTerminal"
Else
    MsgBox "바탕화면에 단축아이콘이 없습니다.", 0, "NinjaInTerminal"
End If
