-- =====================================================
-- RoadMap 自部署地图系统 - 数据库初始化脚本
-- 数据库: PostgreSQL 14+ with PostGIS 3.x
-- 使用方式: psql -U postgres -d roadmap -f init.sql
-- =====================================================

-- 启用 PostGIS 扩展
CREATE EXTENSION IF NOT EXISTS postgis;
CREATE EXTENSION IF NOT EXISTS postgis_topology;

-- =====================================================
-- 1. GPS 定位点表 (核心表)
-- =====================================================
CREATE TABLE IF NOT EXISTS gps_points (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT NOT NULL,
    device_id   VARCHAR(64),
    geom        GEOMETRY(POINT, 4326) NOT NULL,     -- WGS-84 坐标
    altitude    DOUBLE PRECISION,                    -- 海拔 (米)
    speed       DOUBLE PRECISION,                    -- 速度 (m/s)
    accuracy    DOUBLE PRECISION,                    -- 精度 (米)
    heading     DOUBLE PRECISION,                    -- 方向角 (度, 0-360)
    recorded_at TIMESTAMPTZ NOT NULL,                -- GPS 采集时间
    created_at  TIMESTAMPTZ DEFAULT NOW()            -- 入库时间
);

-- 空间索引 (GiST)
CREATE INDEX IF NOT EXISTS idx_gps_points_geom ON gps_points USING GIST (geom);
-- 用户+时间联合索引 (轨迹查询核心索引)
CREATE INDEX IF NOT EXISTS idx_gps_points_user_time ON gps_points (user_id, recorded_at DESC);
-- 设备+时间索引
CREATE INDEX IF NOT EXISTS idx_gps_points_device_time ON gps_points (device_id, recorded_at DESC);

-- =====================================================
-- 2. 轨迹/行程聚合表
-- =====================================================
CREATE TABLE IF NOT EXISTS trips (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT NOT NULL,
    device_id       VARCHAR(64),
    name            VARCHAR(255),
    start_time      TIMESTAMPTZ,
    end_time        TIMESTAMPTZ,
    point_count     INTEGER DEFAULT 0,
    distance_meters DOUBLE PRECISION,                    -- 总距离 (米)
    trajectory      GEOMETRY(LINESTRING, 4326),          -- 完整轨迹线
    simplified_geom GEOMETRY(LINESTRING, 4326),          -- 简化轨迹 (缩略显示用)
    status          VARCHAR(32) DEFAULT 'recording',     -- recording / completed / cancelled
    created_at      TIMESTAMPTZ DEFAULT NOW(),
    updated_at      TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_trips_geom ON trips USING GIST (trajectory);
CREATE INDEX IF NOT EXISTS idx_trips_user ON trips (user_id, start_time DESC);
CREATE INDEX IF NOT EXISTS idx_trips_status ON trips (status);

-- =====================================================
-- 3. 设备表
-- =====================================================
CREATE TABLE IF NOT EXISTS devices (
    id            BIGSERIAL PRIMARY KEY,
    user_id       BIGINT NOT NULL,
    device_id     VARCHAR(64) NOT NULL UNIQUE,
    device_name   VARCHAR(128),
    device_type   VARCHAR(32),                           -- miniprogram / android / ios / web
    last_location GEOMETRY(POINT, 4326),
    last_seen     TIMESTAMPTZ,
    created_at    TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_devices_user ON devices (user_id);
CREATE INDEX IF NOT EXISTS idx_devices_location ON devices USING GIST (last_location);

-- =====================================================
-- 4. 电子围栏表 (可选，区域监控)
-- =====================================================
CREATE TABLE IF NOT EXISTS geofences (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(255) NOT NULL,
    description TEXT,
    boundary    GEOMETRY(POLYGON, 4326) NOT NULL,
    created_at  TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_geofences_boundary ON geofences USING GIST (boundary);

-- =====================================================
-- 5. 存储过程: 从定位点聚合生成轨迹
-- =====================================================
CREATE OR REPLACE FUNCTION aggregate_trip(p_trip_id BIGINT) RETURNS void AS $$
BEGIN
    UPDATE trips SET
        trajectory = sub.line,
        simplified_geom = ST_SimplifyPreserveTopology(sub.line, 0.0001),
        point_count = sub.cnt,
        distance_meters = ST_Length(sub.line::geography),
        start_time = sub.first_time,
        end_time = sub.last_time,
        updated_at = NOW()
    FROM (
        SELECT
            ST_MakeLine(gp.geom ORDER BY gp.recorded_at) AS line,
            COUNT(*)::INTEGER AS cnt,
            MIN(gp.recorded_at) AS first_time,
            MAX(gp.recorded_at) AS last_time
        FROM gps_points gp
        JOIN trips t ON gp.user_id = t.user_id
            AND gp.recorded_at BETWEEN t.start_time AND COALESCE(t.end_time, NOW())
        WHERE t.id = p_trip_id
        GROUP BY t.id
    ) sub
    WHERE trips.id = p_trip_id;
END;
$$ LANGUAGE plpgsql;

-- =====================================================
-- 6. 视图: 用户最新位置
-- =====================================================
CREATE OR REPLACE VIEW v_user_latest_location AS
SELECT DISTINCT ON (user_id)
    id, user_id, device_id, geom,
    ST_X(geom) AS longitude,
    ST_Y(geom) AS latitude,
    altitude, speed, accuracy, heading, recorded_at
FROM gps_points
ORDER BY user_id, recorded_at DESC;

-- =====================================================
-- 7. 定位打卡表
-- =====================================================
CREATE TABLE IF NOT EXISTS check_ins (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT NOT NULL,
    device_id   VARCHAR(64),
    name        VARCHAR(255),                           -- 打卡标题
    address     TEXT,                                   -- 地址描述
    note        TEXT,                                   -- 备注
    geom        GEOMETRY(POINT, 4326) NOT NULL,         -- WGS-84 坐标
    checked_at  TIMESTAMPTZ DEFAULT NOW(),              -- 打卡时间
    created_at  TIMESTAMPTZ DEFAULT NOW()               -- 入库时间
);

CREATE INDEX IF NOT EXISTS idx_check_ins_geom ON check_ins USING GIST (geom);
CREATE INDEX IF NOT EXISTS idx_check_ins_user_time ON check_ins (user_id, checked_at DESC);

-- =====================================================
-- 完成
-- =====================================================
