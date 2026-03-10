@echo off
REM =====================================================
REM  启动 TileServer-GL 瓦片服务 (本地 Node.js)
REM  前置条件: 安装 Node.js 16+
REM =====================================================

setlocal

set PROJECT_DIR=%~dp0..
set MBTILES=%PROJECT_DIR%\data\china.mbtiles
set PORT=8080

echo =====================================================
echo  RoadMap - TileServer-GL 瓦片服务
echo =====================================================
echo.

if not exist "%MBTILES%" (
    echo [错误] 未找到瓦片文件: %MBTILES%
    echo 请先运行 generate-tiles.bat 生成瓦片
    pause
    exit /b 1
)

echo [信息] 启动 TileServer-GL ...
echo [信息] 瓦片文件: %MBTILES%
echo [信息] 端口: %PORT%
echo [信息] 访问地址: http://localhost:%PORT%
echo.

npx tileserver-gl --mbtiles "%MBTILES%" --port %PORT% --verbose
