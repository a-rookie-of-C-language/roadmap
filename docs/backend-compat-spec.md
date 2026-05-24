# RoadMap 后端兼容对接文档（设施管理 + 定点打卡）

## 1. 目标

本模块作为“新后端”，需要兼容既有前端（`智慧停车/frontend/trunk/btm`）当前调用方式，在不大改前端的前提下完成：

- 设施基础数据管理（桥梁、隧道、涵洞等）
- 地图点位展示与类型图标区分
- 巡检人员定点打卡（默认 100 米范围内成功）
- 对老系统身份与组织体系的对接

---

## 2. 对接现状结论

前端存在两套 API 风格并存：

1. 旧风格：`/btm/api/...`
- 多为 `POST`
- 返回常见格式：`{ code, msg, data }`

2. 新风格：`/cow-ywgateway/citymanage/...`
- `GET/POST/PUT/DELETE` 混用
- 返回常见格式：`{ data, meta }`

因此，后端必须提供**兼容层**，避免一次性改造大量前端代码。

---

## 3. 推荐总体方案

采用三层结构：

1. 领域层（新标准）
- `Facility`、`FacilityCheckIn`、`Location` 等统一模型

2. 兼容层（Controller Adapter）
- 对外暴露前端已有路径（`/cow-ywgateway/...`、必要时 `/btm/api/...`）
- 负责字段映射和响应包装

3. 网关层（可选）
- 统一鉴权、路由、审计日志

---

## 4. 数据与字段兼容规则

### 4.1 坐标字段

前端旧字段 `dzdt` 使用字符串格式：

- 单点：`"lng,lat"`
- 线/多点：`"lng,lat|lng,lat|..."`

后端标准建议内部统一为：

- `geometry(Point/LineString, 4326)`
- 对外兼容时再与 `dzdt` 互转

### 4.2 设施核心字段映射

- `id` -> `id`
- `name` -> `mc`
- `locationText` -> `szwz`
- `regionName` -> `adminAreaName`
- `manageUnitId` -> `gldw`
- `manageUnitName` -> `gldwname`
- `maintainUnitId` -> `yhdw`
- `maintainUnitName` -> `yhdwname`
- `typeCode` -> `typeid`（示例：桥梁=0、隧道=1 ...）
- `statusCode` -> `zt`
- `geometry` <-> `dzdt`

### 4.3 类型与图标

前端已按类型选择图标（如 `bridge.png`），后端要稳定返回：

- `typeid`（数字/字符串编码）
- `type`（英文类型，可选）
- `typeName`（中文名称，可选）

---

## 5. 老系统需提供的数据

最小必需：

- `userId`
- `orgId`
- `deviceId`
- `timestamp`
- `lng` / `lat`
- `coordType`（`wgs84/gcj02/bd09`）

建议补充：

- `userName`
- `role/permission`
- `taskId/workOrderId`
- `accuracy/speed/heading`

---

## 6. 兼容接口清单（第一期）

以下优先支持 `cow` 风格（前端设施管理当前大量使用）。

### 6.1 设施列表

- `GET /cow-ywgateway/citymanage/fac-management`

请求参数（示例）：

- `typeid`：设施类型
- `dq`：区域
- `pageNo` / `pageSize`
- `keyword`（可选）

响应（建议）：

```json
{
  "data": [
    {
      "id": "123",
      "mc": "XX桥",
      "typeid": "0",
      "adminAreaName": "XX区",
      "szwz": "XX路XX段",
      "gldw": "1001",
      "gldwname": "XX管理单位",
      "yhdw": "2001",
      "yhdwname": "XX养护单位",
      "zt": "1",
      "dzdt": "106.551,29.563|106.552,29.564"
    }
  ],
  "meta": {
    "totalCount": 100,
    "pageNo": 1,
    "pageSize": 20
  }
}
```

### 6.2 设施统计

- `GET /cow-ywgateway/citymanage/fac-management/count`

响应：`{ data: {...} }`，至少包含总数。

### 6.3 按类型详情

- 桥梁：`GET /cow-ywgateway/citymanage/bridge/{id}`
- 隧道：`GET /cow-ywgateway/citymanage/tunnel/{id}`
- 涵洞：`GET /cow-ywgateway/citymanage/culver/{id}`
- 人行天桥：`GET /cow-ywgateway/citymanage/footbridge/{id}`
- 人行地通道：`GET /cow-ywgateway/citymanage/personsouterrain/{id}`
- 车行下穿：`GET /cow-ywgateway/citymanage/carsouterrain/{id}`
- 挡墙：`GET /cow-ywgateway/citymanage/barricade/{id}`
- 照明：`GET /cow-ywgateway/citymanage/lighting/{id}`

响应建议统一：`{ data: 设施详情对象 }`

### 6.4 新增设施

- 例：`POST /cow-ywgateway/citymanage/bridge`
- 其他类型同模式

请求体兼容前端字段（如 `mc/dzdt/gldw...`）。

响应建议：

```json
{ "data": { "id": "new-id" } }
```

### 6.5 更新设施

- 例：`PUT /cow-ywgateway/citymanage/bridge/renew/{id}`
- 其他类型同模式

响应建议：

```json
{ "data": true }
```

### 6.6 删除（当前前端已见照明）

- `DELETE /cow-ywgateway/citymanage/lighting/drop?ids=1,2,3`

响应建议：`{ "data": true }`

### 6.7 名称重复校验

- `GET /cow-ywgateway/citymanage/ramp/checkRepeat`
- `GET /cow-ywgateway/citymanage/lighting/checkRepeat`
- 或统一：`GET /cow-ywgateway/citymanage/common/checkRepeat`

---

## 7. 定点打卡接口（本项目核心新增）

### 7.1 打卡提交

- `POST /cow-ywgateway/citymanage/dailypatrol/add`

请求体建议：

```json
{
  "userId": 10001,
  "deviceId": "wx-iphone-001",
  "facilityId": "123",
  "lng": 106.551,
  "lat": 29.563,
  "coordType": "gcj02",
  "timestamp": "2026-05-16T10:30:00+08:00",
  "taskId": "task-001"
}
```

判定规则：

- 读取设施中心点与允许半径（默认 `100m`）
- 计算人员定位点到设施中心点距离
- `distanceMeters <= radiusMeters` 判定成功

响应建议：

```json
{
  "data": {
    "success": true,
    "distanceMeters": 36.2,
    "radiusMeters": 100,
    "checkInId": "98765"
  }
}
```

### 7.2 打卡记录查询

- `GET /cow-ywgateway/citymanage/dailypatrol`
- 支持按 `userId/facilityId/dateFrom/dateTo/pageNo/pageSize` 查询
- 响应建议：`{ data: [], meta: { totalCount } }`

---

## 8. 响应封装兼容建议

为避免前端混乱，建议统一在兼容层提供两个包装器：

1. `CowResponse<T>`

```json
{ "data": ..., "meta": ... }
```

2. `BtmResponse<T>`

```json
{ "code": 1, "msg": "success", "data": ... }
```

并按路由前缀选择包装格式。

---

## 9. 与当前 RoadMap 项目整合建议

当前已有：

- `LocationController`（定位上报/最新位置）
- `CheckInController`（普通打卡）

建议新增：

1. `FacilityController`（标准域）
- `/api/v1/facilities` 标准 CRUD

2. `FacilityCheckInController`（标准域）
- `/api/v1/facility-checkins` 定点打卡

3. `CityManageCompatController`（兼容域）
- `/cow-ywgateway/citymanage/...` 对接老前端

4. `BtmCompatController`（可选）
- `/btm/api/...` 旧接口兼容

---

## 10. 实施顺序（建议）

1. 打通核心链路
- 设施列表
- 设施详情
- 定点打卡提交
- 打卡记录列表

2. 做字段映射与响应兼容
- `dzdt` 转换
- `data/meta` 与 `code/msg` 双格式

3. 再补齐扩展接口
- 统计、打印、删除、校验重复

4. 压测与核验
- PostGIS 距离计算准确性
- 坐标系转换一致性（GCJ02/WGS84）

---

## 11. 风险提示

- 前端模块对字段命名依赖强，建议不要轻易改字段名
- 老接口存在“同业务多路径”现象，需逐页回归验证
- 坐标系不统一会导致打卡误判，必须强制传 `coordType`

---

## 12. 验收标准（第一期）

- 可按类型/区域查询设施并在地图展示
- 不同类型设施可正确显示对应图标
- 小程序位置回传后可完成 100 米内打卡成功判定
- 打卡结果可追溯（人员、时间、设施、距离、任务）

