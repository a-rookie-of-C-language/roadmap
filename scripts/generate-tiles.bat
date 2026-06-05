@echo off
setlocal EnableExtensions

REM =====================================================
REM  RoadMap - generate vector tiles with Planetiler
REM  Requirements:
REM    - Java 17+
REM    - A real .osm.pbf file at project root
REM =====================================================

set "SCRIPT_DIR=%~dp0"
set "PROJECT_DIR=%SCRIPT_DIR%.."
set "PBF_FILE=%PROJECT_DIR%\china-260226.osm.pbf"
set "OUTPUT_DIR=%PROJECT_DIR%\data"
set "OUTPUT_FILE=%OUTPUT_DIR%\china.mbtiles"
set "PLANETILER_JAR=%SCRIPT_DIR%planetiler.jar"
set "PLANETILER_VERSION=0.7.0"
set "DOWNLOAD_URL=https://github.com/onthegomap/planetiler/releases/download/v%PLANETILER_VERSION%/planetiler.jar"

REM Optional proxy for Planetiler downloads.
REM Example in PowerShell before running this script:
REM   $env:TILE_PROXY_HOST="127.0.0.1"
REM   $env:TILE_PROXY_PORT="7890"
if defined TILE_PROXY_HOST if defined TILE_PROXY_PORT (
    set "JAVA_TOOL_OPTIONS=-Dhttp.proxyHost=%TILE_PROXY_HOST% -Dhttp.proxyPort=%TILE_PROXY_PORT% -Dhttps.proxyHost=%TILE_PROXY_HOST% -Dhttps.proxyPort=%TILE_PROXY_PORT%"
    set "HTTP_PROXY=http://%TILE_PROXY_HOST%:%TILE_PROXY_PORT%"
    set "HTTPS_PROXY=http://%TILE_PROXY_HOST%:%TILE_PROXY_PORT%"
    echo [INFO] Using proxy: %TILE_PROXY_HOST%:%TILE_PROXY_PORT%
)

echo =====================================================
echo  RoadMap - Tile Generator
echo =====================================================
echo.

if not exist "%PBF_FILE%" (
    echo [ERROR] PBF file not found:
    echo         "%PBF_FILE%"
    echo.
    echo Put a real .osm.pbf file in the project root, or run:
    echo   git lfs pull
    exit /b 1
)

for %%A in ("%PBF_FILE%") do (
    if %%~zA LSS 1048576 (
        echo [ERROR] The PBF file is too small: %%~zA bytes
        echo.
        echo This is probably a Git LFS pointer, not the real map data.
        echo Run:
        echo   git lfs install
        echo   git lfs pull
        echo.
        echo Or replace "%PBF_FILE%" with a real .osm.pbf file.
        exit /b 1
    )
)

if not exist "%OUTPUT_DIR%" mkdir "%OUTPUT_DIR%"

if not exist "%PLANETILER_JAR%" (
    echo [INFO] Downloading Planetiler v%PLANETILER_VERSION% ...
    echo [INFO] %DOWNLOAD_URL%
    powershell -NoProfile -ExecutionPolicy Bypass -Command "[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12; Invoke-WebRequest -Uri '%DOWNLOAD_URL%' -OutFile '%PLANETILER_JAR%'"
    if errorlevel 1 (
        echo [WARN] PowerShell download failed, trying curl.exe ...
        curl.exe -L --retry 3 --output "%PLANETILER_JAR%" "%DOWNLOAD_URL%"
    )
    if errorlevel 1 (
        echo [ERROR] Failed to download Planetiler.
        echo Download it manually and save it as:
        echo   "%PLANETILER_JAR%"
        exit /b 1
    )
)

for %%A in ("%PLANETILER_JAR%") do (
    if %%~zA LSS 1048576 (
        echo [ERROR] Planetiler jar is invalid: %%~zA bytes
        echo.
        echo Delete this file and run the script again:
        echo   "%PLANETILER_JAR%"
        echo.
        echo Or download it manually:
        echo   %DOWNLOAD_URL%
        exit /b 1
    )
)

echo.
echo [INFO] Generating tiles...
echo [INFO] Input : "%PBF_FILE%"
echo [INFO] Output: "%OUTPUT_FILE%"
echo [INFO] This may take 15-40 minutes depending on your machine.
echo.

java -Xmx4g -jar "%PLANETILER_JAR%" ^
    --download ^
    --osm_path="%PBF_FILE%" ^
    --output="%OUTPUT_FILE%" ^
    --force

if errorlevel 1 (
    echo.
    echo [ERROR] Tile generation failed.
    exit /b 1
)

echo.
echo =====================================================
echo [DONE] Generated:
echo        "%OUTPUT_FILE%"
for %%A in ("%OUTPUT_FILE%") do echo [INFO] Size: %%~zA bytes
echo.
echo Next step:
echo   scripts\start-tileserver.bat
echo =====================================================
