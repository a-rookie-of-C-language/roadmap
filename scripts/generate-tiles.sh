#!/bin/bash
# =====================================================
#  Planetiler - 从 OSM PBF 生成矢量瓦片 (.mbtiles)
#  使用方式: bash scripts/generate-tiles.sh
#  前置条件: 安装 Java 17+
# =====================================================

set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"
PBF_FILE="$PROJECT_DIR/china-260226.osm.pbf"
OUTPUT_DIR="$PROJECT_DIR/data"
OUTPUT_FILE="$OUTPUT_DIR/china.mbtiles"
PLANETILER_JAR="$SCRIPT_DIR/planetiler.jar"
PLANETILER_VERSION="0.8.2"
DOWNLOAD_URL="https://github.com/onthegomap/planetiler/releases/download/v${PLANETILER_VERSION}/planetiler-openmaptiles-${PLANETILER_VERSION}-with-deps.jar"

echo "====================================================="
echo " RoadMap - 瓦片生成工具"
echo "====================================================="
echo

# 检查 PBF 文件
if [ ! -f "$PBF_FILE" ]; then
    echo "[错误] 未找到 PBF 文件: $PBF_FILE"
    echo "请将 .osm.pbf 文件放在项目根目录"
    exit 1
fi

# 创建输出目录
mkdir -p "$OUTPUT_DIR"

# 下载 Planetiler (如果不存在)
if [ ! -f "$PLANETILER_JAR" ]; then
    echo "[信息] 下载 Planetiler v${PLANETILER_VERSION} ..."
    curl -L -o "$PLANETILER_JAR" "$DOWNLOAD_URL"
    echo "[信息] 下载完成"
fi

echo
echo "[信息] 开始生成瓦片..."
echo "[信息] 输入: $PBF_FILE"
echo "[信息] 输出: $OUTPUT_FILE"
echo "[信息] 预计时间: 15-40分钟 (取决于机器配置)"
echo

java -Xmx4g -jar "$PLANETILER_JAR" \
    --osm_path="$PBF_FILE" \
    --output="$OUTPUT_FILE" \
    --nodemap_type=sparsebitset \
    --force

echo
echo "====================================================="
echo "[完成] 瓦片文件已生成: $OUTPUT_FILE"
echo "[信息] 文件大小: $(du -h "$OUTPUT_FILE" | cut -f1)"
echo
echo "下一步: 启动 TileServer-GL 瓦片服务"
echo "  npx tileserver-gl --mbtiles data/china.mbtiles --port 8080"
echo "====================================================="
