Set UAC = CreateObject("Shell.Application")
Set fso = CreateObject("Scripting.FileSystemObject")
scriptFolder = fso.GetParentFolderName(WScript.ScriptFullName)
batPath = scriptFolder & "\" & "install-postgis-admin.bat"
UAC.ShellExecute "cmd.exe", "/c """ & batPath & """", "", "runas", 1
