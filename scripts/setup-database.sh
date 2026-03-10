#!/bin/bash
# =====================================================
#  初始化 PostgreSQL + PostGIS 数据库
#  前置条件: 已安装 PostgreSQL 和 PostGIS 扩展
# =====================================================

set -e

PGHOST="${PGHOST:-localhost}"
PGPORT="${PGPORT:-5432}"
PGUSER="${PGUSER:-postgres}"
DB_NAME="roadmap"
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

echo "====================================================="
echo " RoadMap - 数据库初始化"
echo "====================================================="
echo

# 创建数据库 (如果不存在)
echo "[信息] 创建数据库 ${DB_NAME} ..."
if psql -h "$PGHOST" -p "$PGPORT" -U "$PGUSER" -lqt | cut -d\| -f1 | grep -qw "$DB_NAME"; then
    echo "[信息] 数据库 ${DB_NAME} 已存在"
else
    psql -h "$PGHOST" -p "$PGPORT" -U "$PGUSER" -c "CREATE DATABASE ${DB_NAME}"
    echo "[信息] 数据库 ${DB_NAME} 创建成功"
fi

# 执行初始化脚本
echo "[信息] 执行初始化脚本..."
psql -h "$PGHOST" -p "$PGPORT" -U "$PGUSER" -d "$DB_NAME" -f "$SCRIPT_DIR/../sql/init.sql"

echo
echo "====================================================="
echo "[完成] 数据库初始化成功!"
echo "[信息] 数据库: ${DB_NAME} @ ${PGHOST}:${PGPORT}"
echo "====================================================="
