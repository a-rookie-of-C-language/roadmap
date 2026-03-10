Set UAC = CreateObject("Shell.Application")
UAC.ShellExecute "cmd.exe", "/c ""D:\code\IdeaFiles\RoadMap\scripts\install-postgis-admin.bat""", "", "runas", 1
