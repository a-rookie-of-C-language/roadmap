@echo off
REM PostGIS Installation - Copy DLLs to PostgreSQL
REM This script copies PostGIS files from the extracted bundle to PostgreSQL

set SOURCE=D:\code\IdeaFiles\RoadMap\postgis-tmp\postgis-bundle-pg18-3.6.1x64
set TARGET=D:\pgsql

echo Copying PostGIS DLLs to PostgreSQL...
echo Source: %SOURCE%
echo Target: %TARGET%
echo.

echo [1/4] Copying bin\*.dll ...
copy /Y "%SOURCE%\bin\*.dll" "%TARGET%\bin\" >nul 2>&1
if errorlevel 1 (
    echo FAILED - Access denied. Please run this script as Administrator!
    echo Right-click this .bat file and select "Run as administrator"
    pause
    exit /b 1
)
echo OK

echo [2/4] Copying bin\*.exe ...
copy /Y "%SOURCE%\bin\*.exe" "%TARGET%\bin\" >nul 2>&1
echo OK

echo [3/4] Copying lib\*.dll ...
copy /Y "%SOURCE%\lib\*.dll" "%TARGET%\lib\" >nul 2>&1
echo OK

echo [4/4] Copying share\extension\* ...
copy /Y "%SOURCE%\share\extension\*" "%TARGET%\share\extension\" >nul 2>&1
echo OK

echo.
echo Verifying key files...
if exist "%TARGET%\bin\libgeos_c.dll" (echo   OK: libgeos_c.dll) else (echo   MISSING: libgeos_c.dll)
if exist "%TARGET%\bin\libgeos.dll" (echo   OK: libgeos.dll) else (echo   MISSING: libgeos.dll)
if exist "%TARGET%\bin\libproj_8_2.dll" (echo   OK: libproj_8_2.dll) else (echo   MISSING: libproj_8_2.dll)
if exist "%TARGET%\lib\postgis-3.dll" (echo   OK: postgis-3.dll) else (echo   MISSING: postgis-3.dll)

echo.
echo PostGIS installation complete!
echo.
echo Next steps:
echo   1. Run: "D:\pgsql\bin\psql.exe" -U postgres -h localhost -d roadmap -c "CREATE EXTENSION IF NOT EXISTS postgis;"
echo   2. Run: "D:\pgsql\bin\psql.exe" -U postgres -h localhost -d roadmap -f "D:\code\IdeaFiles\RoadMap\sql\init.sql"
echo.
pause
