# RoadMap - 自部署路径追踪系统

替代百度地图的自部署 GPS 定位追踪系统。基于 OpenStreetMap 数据，支持定位上报、轨迹生成、实时推送和轨迹回放。

## 技术栈

- **后端**: Java 17 + Spring Boot 3.2 + Hibernate Spatial
- **数据库**: PostgreSQL + PostGIS
- **前端**: MapLibre GL JS (纯 HTML/CSS/JS, 无框架)
- **地图瓦片**: Planetiler (生成) + TileServer-GL (服务)
- **坐标系**: WGS-84 (存储/显示), 支持 GCJ-02/BD-09 输入自动转换

## 项目结构

```
RoadMap/
├── china-260226.osm.pbf            # OSM 中国地图数据
├── pom.xml                          # Maven 配置
├── sql/
│   └── init.sql                     # 数据库初始化脚本
├── scripts/
│   ├── generate-tiles.bat/.sh       # 瓦片生成脚本 (Planetiler)
│   ├── setup-database.bat/.sh       # 数据库初始化脚本
│   └── start-tileserver.bat         # TileServer-GL 启动脚本
└── src/main/
    ├── java/com/roadmap/
    │   ├── RoadMapApplication.java  # 入口
    │   ├── config/                  # WebSocket, CORS, Jackson 配置
    │   ├── controller/              # REST API 控制器
    │   ├── dto/                     # 数据传输对象
    │   ├── entity/                  # JPA 实体 (PostGIS 空间类型)
    │   ├── repository/              # 数据访问 (含原生 PostGIS SQL)
    │   ├── service/                 # 业务逻辑
    │   ├── util/                    # 坐标转换工具
    │   └── websocket/               # WebSocket 处理器
    └── resources/
        ├── application.yml          # 应用配置
        └── static/                  # Demo 前端页面
            ├── index.html
            ├── css/style.css
            └── js/app.js
```

## 快速开始

### 1. 安装 PostgreSQL + PostGIS

确保 PostgreSQL 已安装并启用 PostGIS 扩展。

**Windows (推荐使用 Stack Builder 安装 PostGIS)**:
- 安装 PostgreSQL 16+
- 通过 Stack Builder 安装 PostGIS 3.4+

### 2. 初始化数据库

```bash
# Windows
scripts\setup-database.bat

# 或手动执行
psql -U postgres -c "CREATE DATABASE roadmap;"
psql -U postgres -d roadmap -f sql/init.sql
```

默认连接配置（可在 `application.yml` 修改）：
- 地址: `localhost:5432`
- 数据库: `roadmap`
- 用户名: `postgres`
- 密码: `roadmap123`

### 3. 启动后端

```bash
mvn spring-boot:run
```

后端启动后访问: http://localhost:8090

### 4. 访问 Demo 页面

浏览器打开 http://localhost:8090/index.html

Demo 页面功能：
- 生成模拟轨迹数据
- 查看轨迹列表并在地图上显示
- 轨迹回放动画（支持变速）
- 显示所有定位点详情
- WebSocket 实时位置推送
- 点击地图上报位置

### 5. (可选) 生成自部署地图瓦片

```bash
# Windows
scripts\generate-tiles.bat

# Linux/Mac
bash scripts/generate-tiles.sh
```

这会将 `china-260226.osm.pbf` 转换为 `china.mbtiles` 矢量瓦片文件。

```bash
# 启动瓦片服务
scripts\start-tileserver.bat
```

TileServer-GL 启动后在 http://localhost:8890 提供瓦片服务，默认样式地址为：

```text
http://localhost:8890/styles/roadmap-basic/style.json
```

> Demo 页面默认使用上述自部署 TileServer-GL 样式。如需独立服务器部署，请在 `app.js` 中将 `tileStyle` 改为服务器上的样式地址。

## API 接口

### 位置上报

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/v1/locations/single` | 上报单个位置 |
| POST | `/api/v1/locations/batch` | 批量上报位置 |
| GET | `/api/v1/locations/latest/{userId}` | 获取用户最新位置 |

**单个位置上报示例:**
```json
POST /api/v1/locations/single
{
  "userId": 1,
  "deviceId": "device-001",
  "lng": 116.397,
  "lat": 39.908,
  "speed": 5.2,
  "accuracy": 10.0,
  "coordType": "gcj02",
  "recordedAt": "2026-03-01T10:00:00.000+0800"
}
```

`coordType` 支持: `wgs84`(默认不转换), `gcj02`(微信/高德坐标), `bd09`(百度坐标)

### 轨迹管理

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/v1/trajectories/{userId}?start=...&end=...` | 按时间范围获取轨迹 GeoJSON |
| GET | `/api/v1/trajectories/{userId}/points?start=...&end=...` | 获取所有定位点 GeoJSON |
| POST | `/api/v1/trajectories/trips` | 创建行程 |
| PUT | `/api/v1/trajectories/trips/{tripId}/end` | 结束行程并聚合轨迹 |
| GET | `/api/v1/trajectories/trips/user/{userId}` | 获取用户行程列表 |
| GET | `/api/v1/trajectories/trips/{tripId}/trajectory` | 获取行程轨迹 GeoJSON |

### 设备管理

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/v1/devices/register` | 注册设备 |
| GET | `/api/v1/devices/user/{userId}` | 获取用户设备列表 |
| GET | `/api/v1/devices/{deviceId}/location` | 获取设备当前位置 |

### 模拟数据

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/v1/simulator/generate?userId=1&points=200` | 生成模拟轨迹 |

### WebSocket 实时推送

连接 `ws://localhost:8090/ws/location`，发送 `{"userId": 1}` 订阅用户位置更新。

## 小程序对接

小程序通过 `wx.getLocation` 获取 GCJ-02 坐标，调用 `/api/v1/locations/batch` 接口上报，后端自动转换为 WGS-84 存储。

```javascript
// 小程序示例代码
wx.getLocation({
  type: 'gcj02',
  success(res) {
    wx.request({
      url: 'https://your-server/api/v1/locations/single',
      method: 'POST',
      data: {
        userId: userId,
        deviceId: deviceId,
        lng: res.longitude,
        lat: res.latitude,
        speed: res.speed,
        accuracy: res.accuracy,
        coordType: 'gcj02'
      }
    });
  }
});
```

## 坐标系说明

| 坐标系 | 来源 | 处理 |
|--------|------|------|
| WGS-84 | GPS 原始坐标、OSM | 直接存储 |
| GCJ-02 | 微信、高德、腾讯 | 后端自动转 WGS-84 |
| BD-09 | 百度地图 | 后端自动转 WGS-84 |
