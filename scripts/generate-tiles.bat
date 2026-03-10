@echo off
REM =====================================================
REM  Planetiler - 从 OSM PBF 生成矢量瓦片 (.mbtiles)
REM  使用方式: 双击运行或在命令行执行
REM  前置条件: 安装 Java 17+
REM =====================================================

setlocal

set SCRIPT_DIR=%~dp0
set PROJECT_DIR=%SCRIPT_DIR%..
set PBF_FILE=%PROJECT_DIR%\china-260226.osm.pbf
set OUTPUT_DIR=%PROJECT_DIR%\data
set OUTPUT_FILE=%OUTPUT_DIR%\china.mbtiles
set PLANETILER_JAR=%SCRIPT_DIR%\planetiler.jar
set PLANETILER_VERSION=0.8.2
set DOWNLOAD_URL=https://github.com/onthegomap/planetiler/releases/download/v%PLANETILER_VERSION%/planetiler-openmaptiles-%PLANETILER_VERSION%-with-deps.jar

echo =====================================================
echo  RoadMap - 瓦片生成工具
echo =====================================================
echo.

REM 检查 PBF 文件
if not exist "%PBF_FILE%" (
    echo [错误] 未找到 PBF 文件: %PBF_FILE%
    echo 请将 .osm.pbf 文件放在项目根目录
    pause
    exit /b 1
)

REM 创建输出目录
if not exist "%OUTPUT_DIR%" mkdir "%OUTPUT_DIR%"

REM 下载 Planetiler (如果不存在)
if not exist "%PLANETILER_JAR%" (
    echo [信息] 下载 Planetiler v%PLANETILER_VERSION% ...
    echo [信息] URL: %DOWNLOAD_URL%
    powershell -Command "Invoke-WebRequest -Uri '%DOWNLOAD_URL%' -OutFile '%PLANETILER_JAR%'"
    if errorlevel 1 (
        echo [错误] 下载失败，请手动下载:
        echo %DOWNLOAD_URL%
        echo 放置到: %PLANETILER_JAR%
        pause
        exit /b 1
    )
    echo [信息] 下载完成
)

echo.
echo [信息] 开始生成瓦片...
echo [信息] 输入: %PBF_FILE%
echo [信息] 输出: %OUTPUT_FILE%
echo [信息] 预计时间: 15-40分钟 (取决于机器配置)
echo.

java -Xmx4g -jar "%PLANETILER_JAR%" ^
    --osm_path="%PBF_FILE%" ^
    --output="%OUTPUT_FILE%" ^
    --nodemap_type=sparsebitset ^
    --force

if errorlevel 1 (
    echo.
    echo [错误] 瓦片生成失败!
    pause
    exit /b 1
)

echo.
echo =====================================================
echo [完成] 瓦片文件已生成: %OUTPUT_FILE%
for %%A in ("%OUTPUT_FILE%") do echo [信息] 文件大小: %%~zA bytes
echo.
echo 下一步: 使用 TileServer-GL 启动瓦片服务
echo   npx tileserver-gl --mbtiles data\china.mbtiles --port 8080
echo =====================================================
pause
