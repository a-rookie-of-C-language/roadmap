@echo off
REM =====================================================
REM  初始化 PostgreSQL + PostGIS 数据库
REM  前置条件: 已安装 PostgreSQL 和 PostGIS 扩展
REM =====================================================

setlocal

set PGHOST=localhost
set PGPORT=5432
set PGUSER=postgres
set DB_NAME=roadmap

echo =====================================================
echo  RoadMap - 数据库初始化
echo =====================================================
echo.

REM 创建数据库 (如果不存在)
echo [信息] 创建数据库 %DB_NAME% ...
psql -h %PGHOST% -p %PGPORT% -U %PGUSER% -tc "SELECT 1 FROM pg_database WHERE datname='%DB_NAME%'" | findstr /r "1" >nul
if errorlevel 1 (
    psql -h %PGHOST% -p %PGPORT% -U %PGUSER% -c "CREATE DATABASE %DB_NAME%"
    echo [信息] 数据库 %DB_NAME% 创建成功
) else (
    echo [信息] 数据库 %DB_NAME% 已存在
)

REM 执行初始化脚本
echo [信息] 执行初始化脚本...
psql -h %PGHOST% -p %PGPORT% -U %PGUSER% -d %DB_NAME% -f "%~dp0..\sql\init.sql"

echo.
echo =====================================================
echo [完成] 数据库初始化成功!
echo [信息] 数据库: %DB_NAME% @ %PGHOST%:%PGPORT%
echo =====================================================
pause
