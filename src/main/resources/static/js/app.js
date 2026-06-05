/**
 * 路径追踪系统 - Demo Frontend
 * 纯 Vanilla JS, 连接 Spring Boot 后端 API
 */

// ===== 配置 =====
const CONFIG = {
  // API 基础地址：同源时用相对路径，跨域时用绝对地址
  apiBase: detectApiBase(),
  wsBase: detectWsBase(),
  defaultCenter: [116.397, 39.908], // 北京
  defaultZoom: 12,
  tileStyle: 'http://localhost:8890/styles/roadmap-basic/style.json',
  reconnectInterval: 3000,
  // 轨迹整体偏移：左下角（西南方向）45° 移动 300m
  // trajectoryOffsetDistance: 1000, // 偏移距离（米）
  // trajectoryOffsetBearing: 240,  // 偏移方位角（度），225° = 西南/左下 45°
  trajectoryOffsetDistance: 0, // 偏移距离（米）
  trajectoryOffsetBearing: 240,  
};

function detectApiBase() {
  // 同源访问时用相对路径（任何端口都行）
  return '';
}

/**
 * 将坐标数组整体偏移指定距离和方向
 * @param {Array} coordinates - [[lng, lat], ...] 坐标数组
 * @param {number} distanceMeters - 偏移距离（米）
 * @param {number} bearingDeg - 偏移方位角（度，北=0，顺时针，225=西南/左下45°）
 * @returns {Array} 偏移后的新坐标数组
 */
function offsetCoordinates(coordinates, distanceMeters, bearingDeg) {
  if (!coordinates || coordinates.length === 0 || distanceMeters === 0) return coordinates;
  const R = 6378137; // 地球半径（米）
  const bearingRad = bearingDeg * Math.PI / 180;
  // 用第一个坐标点的纬度来计算经度方向上的米/度比值
  const refLat = coordinates[0][1];
  const latRad = refLat * Math.PI / 180;
  // 偏移量（度）
  const dLat = (distanceMeters * Math.cos(bearingRad)) / R * (180 / Math.PI);
  const dLng = (distanceMeters * Math.sin(bearingRad)) / (R * Math.cos(latRad)) * (180 / Math.PI);
  return coordinates.map(([lng, lat, ...rest]) => [lng + dLng, lat + dLat, ...rest]);
}

function detectWsBase() {
  const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
  return `${protocol}//${window.location.host}`;
}

// ===== 坐标系转换（前端版，与后端 CoordTransformUtil 算法一致）=====
const _COORD_A = 6378245.0;
const _COORD_EE = 0.00669342162296594323;
const _COORD_X_PI = Math.PI * 3000.0 / 180.0;

function _coordOutOfChina(lng, lat) {
  return lng < 72.004 || lng > 137.8347 || lat < 0.8293 || lat > 55.8271;
}

function _coordTransformLat(lng, lat) {
  let ret = -100.0 + 2.0 * lng + 3.0 * lat + 0.2 * lat * lat
    + 0.1 * lng * lat + 0.2 * Math.sqrt(Math.abs(lng));
  ret += (20.0 * Math.sin(6.0 * lng * Math.PI) + 20.0 * Math.sin(2.0 * lng * Math.PI)) * 2.0 / 3.0;
  ret += (20.0 * Math.sin(lat * Math.PI) + 40.0 * Math.sin(lat / 3.0 * Math.PI)) * 2.0 / 3.0;
  ret += (160.0 * Math.sin(lat / 12.0 * Math.PI) + 320 * Math.sin(lat * Math.PI / 30.0)) * 2.0 / 3.0;
  return ret;
}

function _coordTransformLng(lng, lat) {
  let ret = 300.0 + lng + 2.0 * lat + 0.1 * lng * lng
    + 0.1 * lng * lat + 0.1 * Math.sqrt(Math.abs(lng));
  ret += (20.0 * Math.sin(6.0 * lng * Math.PI) + 20.0 * Math.sin(2.0 * lng * Math.PI)) * 2.0 / 3.0;
  ret += (20.0 * Math.sin(lng * Math.PI) + 40.0 * Math.sin(lng / 3.0 * Math.PI)) * 2.0 / 3.0;
  ret += (150.0 * Math.sin(lng / 12.0 * Math.PI) + 300.0 * Math.sin(lng / 30.0 * Math.PI)) * 2.0 / 3.0;
  return ret;
}

function _wgs84ToGcj02(lng, lat) {
  if (_coordOutOfChina(lng, lat)) return [lng, lat];
  let dLat = _coordTransformLat(lng - 105.0, lat - 35.0);
  let dLng = _coordTransformLng(lng - 105.0, lat - 35.0);
  const radLat = lat / 180.0 * Math.PI;
  let magic = Math.sin(radLat);
  magic = 1 - _COORD_EE * magic * magic;
  const sqrtMagic = Math.sqrt(magic);
  dLat = (dLat * 180.0) / ((_COORD_A * (1 - _COORD_EE)) / (magic * sqrtMagic) * Math.PI);
  dLng = (dLng * 180.0) / (_COORD_A / sqrtMagic * Math.cos(radLat) * Math.PI);
  return [lng + dLng, lat + dLat];
}

/**
 * GCJ-02（火星坐标/高德/微信）→ WGS-84
 */
function gcj02ToWgs84(lng, lat) {
  if (_coordOutOfChina(lng, lat)) return [lng, lat];
  const gcj = _wgs84ToGcj02(lng, lat);
  return [lng * 2 - gcj[0], lat * 2 - gcj[1]];
}

/**
 * BD-09（百度坐标）→ GCJ-02
 */
function bd09ToGcj02(lng, lat) {
  const x = lng - 0.0065;
  const y = lat - 0.006;
  const z = Math.sqrt(x * x + y * y) - 0.00002 * Math.sin(y * _COORD_X_PI);
  const theta = Math.atan2(y, x) - 0.000003 * Math.cos(x * _COORD_X_PI);
  return [z * Math.cos(theta), z * Math.sin(theta)];
}

/**
 * BD-09（百度坐标）→ WGS-84
 */
function bd09ToWgs84(lng, lat) {
  const gcj = bd09ToGcj02(lng, lat);
  return gcj02ToWgs84(gcj[0], gcj[1]);
}

/**
 * 统一转换入口：将任意坐标系转为 WGS-84
 * @param {number} lng
 * @param {number} lat
 * @param {string} coordType - 'wgs84' | 'gcj02' | 'bd09'
 * @returns {[number, number]} [lng, lat] WGS-84
 */
function toWgs84(lng, lat, coordType) {
  switch ((coordType || 'wgs84').trim().toLowerCase()) {
    case 'bd09': return bd09ToWgs84(lng, lat);
    case 'gcj02': return gcj02ToWgs84(lng, lat);
    default: return [lng, lat];
  }
}

// ===== 全局状态 =====
const state = {
  map: null,
  userId: 1,
  deviceId: 'demo-device-1',
  trips: [],
  activeTripId: null,
  ws: null,
  wsConnected: false,
  wsReconnectTimer: null,
  playback: {
    playing: false,
    paused: false,
    speed: 1,
    index: 0,
    points: [],
    timer: null,
    marker: null,
  },
  clickPopup: null,
  realtimeMarker: null,
  realtimeAccuracyCircle: null,
  trajectoryLayerAdded: false,
  pointsLayerAdded: false,
  showingPoints: false,
  checkIns: [],
  pendingCheckInCoord: null,   // [lng, lat] 从地图点击暂存的坐标
};

// Promise 等待地图加载完毕
let _resolveMapReady;
const mapReadyPromise = new Promise((resolve) => { _resolveMapReady = resolve; });

// ===== 初始化 =====
document.addEventListener('DOMContentLoaded', init);

function init() {
  initMap();
  bindEvents();
  updateStatus('backend', 'checking');
  checkBackendHealth();
}

// ===== 地图初始化 =====
function initMap() {
  state.map = new maplibregl.Map({
    container: 'map',
    style: CONFIG.tileStyle,
    center: CONFIG.defaultCenter,
    zoom: CONFIG.defaultZoom,
    attributionControl: false,
  });

  state.map.addControl(new maplibregl.NavigationControl(), 'top-left');
  state.map.addControl(new maplibregl.ScaleControl({ unit: 'metric' }), 'bottom-left');
  state.map.addControl(new maplibregl.AttributionControl({ compact: true }), 'bottom-right');

  // 点击地图上报位置
  state.map.on('click', onMapClick);

  state.map.on('load', () => {
    // 添加空的轨迹数据源
    state.map.addSource('trajectory', {
      type: 'geojson',
      data: emptyFeatureCollection(),
    });
    state.map.addSource('trajectory-points', {
      type: 'geojson',
      data: emptyFeatureCollection(),
    });
    state.map.addSource('start-end-markers', {
      type: 'geojson',
      data: emptyFeatureCollection(),
    });

    // 轨迹线图层
    state.map.addLayer({
      id: 'trajectory-line',
      type: 'line',
      source: 'trajectory',
      layout: { 'line-join': 'round', 'line-cap': 'round' },
      paint: {
        'line-color': '#4a90d9',
        'line-width': 4,
        'line-opacity': 0.85,
      },
    });

    // 定位点图层（默认隐藏）
    state.map.addLayer({
      id: 'trajectory-points-layer',
      type: 'circle',
      source: 'trajectory-points',
      paint: {
        'circle-radius': 4,
        'circle-color': '#4a90d9',
        'circle-stroke-width': 1,
        'circle-stroke-color': '#fff',
        'circle-opacity': 0.7,
      },
      layout: { visibility: 'none' },
    });

    // 起止点图层
    state.map.addLayer({
      id: 'start-end-layer',
      type: 'circle',
      source: 'start-end-markers',
      paint: {
        'circle-radius': 7,
        'circle-color': ['get', 'color'],
        'circle-stroke-width': 2,
        'circle-stroke-color': '#fff',
      },
    });

    // 打卡点数据源
    state.map.addSource('checkins', {
      type: 'geojson',
      data: emptyFeatureCollection(),
    });

    // 打卡点图层
    state.map.addLayer({
      id: 'checkins-layer',
      type: 'circle',
      source: 'checkins',
      paint: {
        'circle-radius': 9,
        'circle-color': '#f39c12',
        'circle-stroke-width': 2,
        'circle-stroke-color': '#fff',
        'circle-opacity': 0.9,
      },
    });

    state.trajectoryLayerAdded = true;
    state.pointsLayerAdded = true;

    // 打卡点点击事件
    state.map.on('click', 'checkins-layer', onCheckInClick);
    state.map.on('mouseenter', 'checkins-layer', () => {
      state.map.getCanvas().style.cursor = 'pointer';
    });
    state.map.on('mouseleave', 'checkins-layer', () => {
      state.map.getCanvas().style.cursor = '';
    });

    // 通知地图已就绪
    _resolveMapReady();

    // 点击定位点弹出详情
    state.map.on('click', 'trajectory-points-layer', onPointClick);
    state.map.on('mouseenter', 'trajectory-points-layer', () => {
      state.map.getCanvas().style.cursor = 'pointer';
    });
    state.map.on('mouseleave', 'trajectory-points-layer', () => {
      state.map.getCanvas().style.cursor = '';
    });
  });
}

// ===== 事件绑定 =====
function bindEvents() {
  // 用户/设备输入
  document.getElementById('userId').addEventListener('change', (e) => {
    state.userId = parseInt(e.target.value) || 1;
  });
  document.getElementById('deviceId').addEventListener('change', (e) => {
    state.deviceId = e.target.value || 'demo-device-1';
  });

  // 模拟数据
  document.getElementById('btnGenerate').addEventListener('click', generateSimulation);

  // 导入轨迹
  document.getElementById('btnImport').addEventListener('click', importTrajectory);
  document.getElementById('btnImportSample').addEventListener('click', fillImportSample);

  // 轨迹列表
  document.getElementById('btnRefreshTrips').addEventListener('click', loadTrips);

  // 显示所有定位点
  document.getElementById('btnTogglePoints').addEventListener('click', togglePoints);

  // 实时定位
  document.getElementById('btnRealtime').addEventListener('click', toggleRealtime);

  // 轨迹播放控制
  document.getElementById('btnPlay').addEventListener('click', togglePlayback);
  document.getElementById('btnStop').addEventListener('click', stopPlayback);
  document.getElementById('playbackSpeed').addEventListener('change', (e) => {
    state.playback.speed = parseFloat(e.target.value);
  });

  // 进度条点击
  document.getElementById('playbackProgress').addEventListener('click', seekPlayback);

  // 移动端侧边栏切换
  const toggle = document.getElementById('sidebarToggle');
  if (toggle) {
    toggle.addEventListener('click', () => {
      document.getElementById('sidebar').classList.toggle('open');
    });
  }

  // 打卡功能
  document.getElementById('btnCheckInCenter').addEventListener('click', checkInAtCenter);
  document.getElementById('btnRefreshCheckIns').addEventListener('click', loadCheckIns);
}

// ===== API 调用封装 =====
async function api(method, path, body) {
  const url = `${CONFIG.apiBase}${path}`;
  const options = {
    method,
    headers: { 'Content-Type': 'application/json' },
  };
  if (body) {
    options.body = JSON.stringify(body);
  }

  const response = await fetch(url, options);
  if (!response.ok) {
    throw new Error(`HTTP ${response.status}: ${response.statusText}`);
  }
  return response.json();
}

// ===== 后端健康检查 =====
async function checkBackendHealth() {
  try {
    // 尝试调用一个简单的 API 确认后端在线
    await api('GET', '/api/v1/devices/user/0');
    updateStatus('backend', 'connected');
    toast('后端服务已连接', 'success');
  } catch (e) {
    updateStatus('backend', 'disconnected');
    toast('后端服务未连接，请确认 Spring Boot 运行在 8090 端口', 'error');
  }
}

// ===== 模拟数据生成 =====
async function generateSimulation() {
  const btn = document.getElementById('btnGenerate');
  const pointCount = parseInt(document.getElementById('pointCount').value) || 200;

  btn.disabled = true;
  btn.innerHTML = '<span class="spinner"></span> 生成中...';

  try {
    const params = new URLSearchParams({
      userId: state.userId,
      centerLng: CONFIG.defaultCenter[0],
      centerLat: CONFIG.defaultCenter[1],
      points: pointCount,
      intervalSeconds: 5,
    });

    const resp = await api('POST', `/api/v1/simulator/generate?${params}`);
    if (resp.code === 0) {
      const tripId = resp.data.tripId;
      const saved = resp.data.pointCount;
      toast(`已生成 ${saved} 个轨迹点，Trip ID: ${tripId}`, 'success');

      // 自动刷新并加载新轨迹
      await loadTrips();
      await loadTripTrajectory(tripId);
    } else {
      toast(`生成失败: ${resp.message}`, 'error');
    }
  } catch (e) {
    toast(`请求失败: ${e.message}`, 'error');
  } finally {
    btn.disabled = false;
    btn.textContent = '生成模拟轨迹';
  }
}

// ===== 导入轨迹 =====
async function importTrajectory() {
  const btn = document.getElementById('btnImport');
  const coordText = document.getElementById('importCoords').value.trim();
  const importName = document.getElementById('importName').value.trim();
  const coordType = document.getElementById('importCoordType').value;

  if (!coordText) {
    toast('请输入坐标点', 'error');
    return;
  }

  // 解析坐标点：用 | 或换行分隔，每个点格式 “经度,纬度”
  const lines = coordText.split(/[|\n]/).map(l => l.trim()).filter(l => l && !l.startsWith('#') && !l.startsWith('//'));
  const points = [];
  for (const line of lines) {
    const parts = line.split(/[,\s\t]+/);
    if (parts.length >= 2) {
      const lng = parseFloat(parts[0]);
      const lat = parseFloat(parts[1]);
      if (!isNaN(lng) && !isNaN(lat) && Math.abs(lng) <= 180 && Math.abs(lat) <= 90) {
        points.push({ lng, lat });
      }
    }
  }

  if (points.length < 2) {
    toast(`解析出 ${points.length} 个有效坐标点，至少需要2个`, 'error');
    return;
  }

  // 若输入坐标系不是 WGS-84，前端先完成转换，统一以 WGS-84 上报
  let finalPoints = points;
  if (coordType !== 'wgs84') {
    finalPoints = points.map(({ lng, lat }) => {
      const [wLng, wLat] = toWgs84(lng, lat, coordType);
      return { lng: wLng, lat: wLat };
    });
    toast(`已将 ${points.length} 个点从 ${coordType.toUpperCase()} 转换为 WGS-84`, 'info');
  }

  btn.disabled = true;
  btn.innerHTML = '<span class="spinner"></span> 导入中...';

  try {
    const resp = await api('POST', '/api/v1/trajectories/import', {
      userId: state.userId,
      deviceId: state.deviceId,
      name: importName || null,
      coordType: 'wgs84',  // 前端已完成转换，统一告知后端为 WGS-84
      points: finalPoints,
    });

    if (resp.code === 0) {
      const tripId = resp.data.tripId;
      const saved = resp.data.pointCount;
      toast(`已导入 ${saved} 个轨迹点，Trip ID: ${tripId}`, 'success');

      // 自动刷新并加载新轨迹
      await loadTrips();
      await loadTripTrajectory(tripId);
    } else {
      toast(`导入失败: ${resp.message}`, 'error');
    }
  } catch (e) {
    toast(`请求失败: ${e.message}`, 'error');
  } finally {
    btn.disabled = false;
    btn.textContent = '导入轨迹';
  }
}

function fillImportSample() {
  document.getElementById('importCoords').value =
    '116.3970,39.9080|116.3975,39.9085|116.3982,39.9090|116.3990,39.9095|116.3998,39.9100|116.4005,39.9108|116.4015,39.9115|116.4025,39.9120|116.4035,39.9128|116.4042,39.9135';
  toast('已填入示例坐标（北京中心区域 10 个点）', 'info');
}

// ===== 轨迹列表 =====
async function loadTrips() {
  const btn = document.getElementById('btnRefreshTrips');
  const listEl = document.getElementById('tripList');

  btn.disabled = true;
  btn.innerHTML = '<span class="spinner"></span>';

  try {
    const resp = await api('GET', `/api/v1/trajectories/trips/user/${state.userId}`);
    if (resp.code === 0) {
      state.trips = resp.data || [];
      renderTripList();
    } else {
      toast(`加载失败: ${resp.message}`, 'error');
    }
  } catch (e) {
    listEl.innerHTML = '<div class="empty-list">加载失败，请确认后端已启动</div>';
    toast(`请求失败: ${e.message}`, 'error');
  } finally {
    btn.disabled = false;
    btn.textContent = '刷新';
  }
}

function renderTripList() {
  const listEl = document.getElementById('tripList');
  if (state.trips.length === 0) {
    listEl.innerHTML = '<div class="empty-list">暂无轨迹数据，请先生成模拟数据</div>';
    return;
  }

  listEl.innerHTML = state.trips.map((trip) => {
    const isActive = trip.id === state.activeTripId;
    const distance = trip.distanceMeters
      ? `${(trip.distanceMeters / 1000).toFixed(2)} km`
      : '—';
    const timeStr = trip.startTime
      ? new Date(trip.startTime).toLocaleString('zh-CN', { month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' })
      : '—';
    return `
      <div class="trip-item ${isActive ? 'active' : ''}" onclick="loadTripTrajectory(${trip.id})">
        <div class="trip-name">${escapeHtml(trip.name || '未命名轨迹')}</div>
        <div class="trip-meta">
          <span>${timeStr}</span>
          <span>${distance}</span>
          <span>${trip.pointCount || 0} 点</span>
        </div>
      </div>
    `;
  }).join('');
}

// ===== 加载轨迹 =====
async function loadTripTrajectory(tripId) {
  await mapReadyPromise;
  state.activeTripId = tripId;
  renderTripList();

  // 隐藏已有定位点
  state.showingPoints = false;
  updateTogglePointsBtn();
  if (state.pointsLayerAdded) {
    state.map.setLayoutProperty('trajectory-points-layer', 'visibility', 'none');
  }

  try {
    const resp = await api('GET', `/api/v1/trajectories/trips/${tripId}/trajectory?tolerance=0.00005`);
    if (resp.code !== 0 || !resp.data) {
      toast('该轨迹无数据', 'info');
      return;
    }

    // resp.data 是 GeoJSON geometry 字符串
    let geom;
    try {
      geom = typeof resp.data === 'string' ? JSON.parse(resp.data) : resp.data;
    } catch (e) {
      toast('轨迹数据格式错误', 'error');
      return;
    }

    if (!geom || !geom.coordinates || geom.coordinates.length === 0) {
      toast('该轨迹无坐标数据', 'info');
      return;
    }

    // 整体坐标偏移（左下角45°方向移动300m）
    geom.coordinates = offsetCoordinates(
      geom.coordinates,
      CONFIG.trajectoryOffsetDistance,
      CONFIG.trajectoryOffsetBearing
    );

    // 构建 GeoJSON Feature
    const feature = {
      type: 'Feature',
      geometry: geom,
      properties: {},
    };
    const featureCollection = {
      type: 'FeatureCollection',
      features: [feature],
    };

    // 更新地图数据源
    state.map.getSource('trajectory').setData(featureCollection);

    // 起止点
    const coords = geom.coordinates;
    const startEndFc = {
      type: 'FeatureCollection',
      features: [
        {
          type: 'Feature',
          geometry: { type: 'Point', coordinates: coords[0] },
          properties: { color: '#2ecc71', label: '起点' },
        },
        {
          type: 'Feature',
          geometry: { type: 'Point', coordinates: coords[coords.length - 1] },
          properties: { color: '#e74c3c', label: '终点' },
        },
      ],
    };
    state.map.getSource('start-end-markers').setData(startEndFc);

    // 缩放到轨迹范围
    const bounds = new maplibregl.LngLatBounds();
    coords.forEach((c) => bounds.extend(c));
    state.map.fitBounds(bounds, { padding: 60, maxZoom: 16 });

    // 准备播放数据
    state.playback.points = coords;
    state.playback.index = 0;
    document.getElementById('playbackPanel').classList.add('visible');
    updatePlaybackInfo();

    toast(`已加载轨迹，共 ${coords.length} 个坐标点`, 'success');
  } catch (e) {
    toast(`加载轨迹失败: ${e.message}`, 'error');
  }
}

// ===== 显示所有定位点 =====
function togglePoints() {
  if (!state.activeTripId) {
    toast('请先选择一条轨迹', 'info');
    return;
  }

  state.showingPoints = !state.showingPoints;
  updateTogglePointsBtn();

  if (state.showingPoints) {
    loadAllPoints();
  } else {
    state.map.setLayoutProperty('trajectory-points-layer', 'visibility', 'none');
  }
}

function updateTogglePointsBtn() {
  const btn = document.getElementById('btnTogglePoints');
  btn.textContent = state.showingPoints ? '隐藏定位点' : '显示定位点';
}

async function loadAllPoints() {
  await mapReadyPromise;
  // 找到当前轨迹的时间范围
  const trip = state.trips.find((t) => t.id === state.activeTripId);
  if (!trip || !trip.startTime || !trip.endTime) {
    // 没有时间范围，用轨迹坐标当定位点
    const coords = state.playback.points;
    if (coords.length > 0) {
      const fc = {
        type: 'FeatureCollection',
        features: coords.map((c, i) => ({
          type: 'Feature',
          geometry: { type: 'Point', coordinates: c },
          properties: { index: i },
        })),
      };
      state.map.getSource('trajectory-points').setData(fc);
      state.map.setLayoutProperty('trajectory-points-layer', 'visibility', 'visible');
    }
    return;
  }

  try {
    const params = new URLSearchParams({
      start: trip.startTime,
      end: trip.endTime,
    });
    const resp = await api('GET', `/api/v1/trajectories/${state.userId}/points?${params}`);
    if (resp.code === 0 && resp.data) {
      let fc;
      try {
        fc = typeof resp.data === 'string' ? JSON.parse(resp.data) : resp.data;
      } catch (e) {
        return;
      }
      if (fc && fc.features) {
        state.map.getSource('trajectory-points').setData(fc);
        state.map.setLayoutProperty('trajectory-points-layer', 'visibility', 'visible');
        toast(`已加载 ${fc.features.length} 个定位点`, 'info');
      }
    }
  } catch (e) {
    toast(`加载定位点失败: ${e.message}`, 'error');
  }
}

function onPointClick(e) {
  if (!e.features || e.features.length === 0) return;
  const props = e.features[0].properties;
  const coords = e.features[0].geometry.coordinates;

  const parts = [];
  parts.push(`坐标: ${coords[0].toFixed(6)}, ${coords[1].toFixed(6)}`);
  if (props.speed != null) parts.push(`速度: ${parseFloat(props.speed).toFixed(1)} m/s`);
  if (props.accuracy != null) parts.push(`精度: ${parseFloat(props.accuracy).toFixed(1)} m`);
  if (props.altitude != null) parts.push(`海拔: ${parseFloat(props.altitude).toFixed(1)} m`);
  if (props.heading != null) parts.push(`方向: ${parseFloat(props.heading).toFixed(0)}°`);
  if (props.recorded_at) parts.push(`时间: ${new Date(props.recorded_at).toLocaleString('zh-CN')}`);

  new maplibregl.Popup({ closeOnClick: true })
    .setLngLat(coords)
    .setHTML(`<div style="font-size:12px;line-height:1.6">${parts.join('<br>')}</div>`)
    .addTo(state.map);
}

// ===== 轨迹播放 =====
function togglePlayback() {
  if (state.playback.points.length === 0) return;

  if (state.playback.playing && !state.playback.paused) {
    // 暂停
    state.playback.paused = true;
    clearInterval(state.playback.timer);
    document.getElementById('btnPlay').textContent = '继续';
    return;
  }

  if (state.playback.paused) {
    // 继续
    state.playback.paused = false;
    startPlaybackTimer();
    document.getElementById('btnPlay').textContent = '暂停';
    return;
  }

  // 开始播放
  state.playback.playing = true;
  state.playback.paused = false;
  state.playback.index = 0;

  // 创建播放标记
  if (state.playback.marker) {
    state.playback.marker.remove();
  }
  const el = document.createElement('div');
  el.className = 'pulse-dot';
  state.playback.marker = new maplibregl.Marker({ element: el })
    .setLngLat(state.playback.points[0])
    .addTo(state.map);

  document.getElementById('btnPlay').textContent = '暂停';
  startPlaybackTimer();
}

function startPlaybackTimer() {
  const baseInterval = 50; // 50ms 基准间隔
  state.playback.timer = setInterval(() => {
    state.playback.index++;
    if (state.playback.index >= state.playback.points.length) {
      stopPlayback();
      toast('轨迹播放完成', 'info');
      return;
    }

    const coord = state.playback.points[state.playback.index];
    state.playback.marker.setLngLat(coord);
    updatePlaybackInfo();
  }, baseInterval / state.playback.speed);
}

function stopPlayback() {
  state.playback.playing = false;
  state.playback.paused = false;
  state.playback.index = 0;
  clearInterval(state.playback.timer);
  if (state.playback.marker) {
    state.playback.marker.remove();
    state.playback.marker = null;
  }
  document.getElementById('btnPlay').textContent = '播放';
  updatePlaybackInfo();
}

function seekPlayback(e) {
  if (state.playback.points.length === 0) return;
  const rect = e.currentTarget.getBoundingClientRect();
  const ratio = (e.clientX - rect.left) / rect.width;
  state.playback.index = Math.floor(ratio * (state.playback.points.length - 1));
  if (state.playback.marker) {
    state.playback.marker.setLngLat(state.playback.points[state.playback.index]);
  }
  updatePlaybackInfo();
}

function updatePlaybackInfo() {
  const total = state.playback.points.length;
  const current = state.playback.index;
  const progress = total > 0 ? (current / (total - 1)) * 100 : 0;

  document.getElementById('progressFill').style.width = `${progress}%`;
  document.getElementById('playbackCurrent').textContent = `${current + 1}`;
  document.getElementById('playbackTotal').textContent = `${total}`;

  if (total > 0 && current < total) {
    const coord = state.playback.points[current];
    document.getElementById('playbackCoord').textContent =
      `${coord[0].toFixed(5)}, ${coord[1].toFixed(5)}`;
  }
}

// ===== 实时定位 WebSocket =====
function toggleRealtime() {
  const btn = document.getElementById('btnRealtime');

  if (state.wsConnected) {
    disconnectWs();
    btn.textContent = '开始实时定位';
    btn.classList.remove('btn-danger');
    btn.classList.add('btn-success');
  } else {
    connectWs();
    btn.textContent = '停止实时定位';
    btn.classList.remove('btn-success');
    btn.classList.add('btn-danger');
  }
}

function connectWs() {
  updateStatus('ws', 'connecting');
  const wsUrl = `${CONFIG.wsBase}/ws/location`;

  try {
    state.ws = new WebSocket(wsUrl);
  } catch (e) {
    toast(`WebSocket 连接失败: ${e.message}`, 'error');
    updateStatus('ws', 'disconnected');
    return;
  }

  state.ws.onopen = () => {
    state.wsConnected = true;
    updateStatus('ws', 'connected');
    toast('实时定位已连接', 'success');
    // 订阅当前用户
    state.ws.send(JSON.stringify({ userId: state.userId }));
  };

  state.ws.onmessage = (event) => {
    try {
      const location = JSON.parse(event.data);
      updateRealtimeLocation(location);
    } catch (e) {
      // 忽略解析错误
    }
  };

  state.ws.onclose = () => {
    state.wsConnected = false;
    updateStatus('ws', 'disconnected');

    // 自动重连
    if (document.getElementById('btnRealtime').textContent === '停止实时定位') {
      state.wsReconnectTimer = setTimeout(() => {
        toast('正在重新连接...', 'info');
        connectWs();
      }, CONFIG.reconnectInterval);
    }
  };

  state.ws.onerror = () => {
    toast('WebSocket 连接错误', 'error');
  };
}

function disconnectWs() {
  clearTimeout(state.wsReconnectTimer);
  if (state.ws) {
    state.ws.close();
    state.ws = null;
  }
  state.wsConnected = false;
  updateStatus('ws', 'disconnected');

  // 移除实时位置标记
  if (state.realtimeMarker) {
    state.realtimeMarker.remove();
    state.realtimeMarker = null;
  }
}

function updateRealtimeLocation(location) {
  const lng = location.lng;
  const lat = location.lat;
  if (!lng || !lat) return;

  // 更新侧边栏信息
  document.getElementById('rtLng').textContent = lng.toFixed(6);
  document.getElementById('rtLat').textContent = lat.toFixed(6);
  document.getElementById('rtSpeed').textContent =
    location.speed != null ? `${location.speed.toFixed(1)} m/s` : '—';
  document.getElementById('rtAccuracy').textContent =
    location.accuracy != null ? `${location.accuracy.toFixed(0)} m` : '—';
  document.getElementById('rtTime').textContent = new Date().toLocaleTimeString('zh-CN');

  // 更新/创建地图标记
  if (!state.realtimeMarker) {
    const el = document.createElement('div');
    el.className = 'pulse-dot';
    state.realtimeMarker = new maplibregl.Marker({ element: el })
      .setLngLat([lng, lat])
      .addTo(state.map);
  } else {
    state.realtimeMarker.setLngLat([lng, lat]);
  }
}

// ===== 地图点击上报位置 =====
function onMapClick(e) {
  // 如果点击了定位点图层，不触发上报
  const features = state.map.queryRenderedFeatures(e.point, { layers: ['trajectory-points-layer'] });
  if (features.length > 0) return;

  const { lng, lat } = e.lngLat;

  // 显示点击弹窗
  const popup = new maplibregl.Popup({ closeOnClick: true, maxWidth: '240px' })
    .setLngLat([lng, lat])
    .setHTML(`
      <div style="font-size:12px">
        <div style="margin-bottom:8px;color:#666;font-family:monospace">
          ${lng.toFixed(6)}, ${lat.toFixed(6)}
        </div>
        <button onclick="reportLocation(${lng}, ${lat})"
                style="padding:4px 12px;background:#4a90d9;color:#fff;border:none;
                       border-radius:3px;cursor:pointer;font-size:12px;width:100%;margin-bottom:6px">
          上报此位置
        </button>
        <button onclick="checkInAtCoord(${lng}, ${lat})"
                style="padding:4px 12px;background:#f39c12;color:#fff;border:none;
                       border-radius:3px;cursor:pointer;font-size:12px;width:100%">
          📍 在此处打卡
        </button>
      </div>
    `)
    .addTo(state.map);
}

// 上报单个位置（全局函数，供 popup 内按钮调用）
window.reportLocation = async function (lng, lat) {
  try {
    const resp = await api('POST', '/api/v1/locations/single', {
      userId: state.userId,
      deviceId: state.deviceId,
      lng: lng,
      lat: lat,
      coordType: 'wgs84', // 地图上点击的已经是 WGS-84
      recordedAt: new Date().toISOString(),
    });

    if (resp.code === 0) {
      toast(`位置已上报: ${lng.toFixed(4)}, ${lat.toFixed(4)}`, 'success');

      // 添加临时标记
      new maplibregl.Marker({ color: '#e74c3c' })
        .setLngLat([lng, lat])
        .addTo(state.map);
    } else {
      toast(`上报失败: ${resp.message}`, 'error');
    }
  } catch (e) {
    toast(`上报失败: ${e.message}`, 'error');
  }
};

// 加载轨迹（全局函数，供列表 onclick 调用）
window.loadTripTrajectory = loadTripTrajectory;

// 从地图点击 popup 触发的打卡（挂在 window 上供 popup 按钮调用）
window.checkInAtCoord = async function (lng, lat) {
  state.pendingCheckInCoord = [lng, lat];
  await doCheckIn(lng, lat);
};

// ===== 定位打卡 =====

/**
 * 在地图当前中心打卡
 */
async function checkInAtCenter() {
  const center = state.map.getCenter();
  await doCheckIn(center.lng, center.lat);
}

/**
 * 执行打卡请求
 */
async function doCheckIn(lng, lat) {
  const name = document.getElementById('checkInName').value.trim() || '打卡';
  const note = document.getElementById('checkInNote').value.trim();

  const btn = document.getElementById('btnCheckInCenter');
  btn.disabled = true;
  btn.textContent = '打卡中...';

  try {
    const resp = await api('POST', '/api/v1/checkins', {
      userId: state.userId,
      deviceId: state.deviceId,
      name,
      note: note || null,
      lng,
      lat,
      coordType: 'wgs84',
      checkedAt: new Date().toISOString(),
    });

    if (resp.code === 0) {
      toast(`打卡成功：${name} (${lng.toFixed(4)}, ${lat.toFixed(4)})`, 'success');
      // 清空输入框
      document.getElementById('checkInName').value = '';
      document.getElementById('checkInNote').value = '';
      // 刷新打卡列表
      await loadCheckIns();
    } else {
      toast(`打卡失败: ${resp.message}`, 'error');
    }
  } catch (e) {
    toast(`打卡失败: ${e.message}`, 'error');
  } finally {
    btn.disabled = false;
    btn.textContent = '在地图中心打卡';
  }
}

/**
 * 加载当前用户的所有打卡记录
 */
async function loadCheckIns() {
  const btn = document.getElementById('btnRefreshCheckIns');
  btn.disabled = true;
  btn.innerHTML = '<span class="spinner"></span>';

  try {
    const resp = await api('GET', `/api/v1/checkins/user/${state.userId}`);
    if (resp.code === 0) {
      state.checkIns = resp.data || [];
      renderCheckInList();
      updateCheckInLayer();
    } else {
      toast(`加载打卡记录失败: ${resp.message}`, 'error');
    }
  } catch (e) {
    toast(`请求失败: ${e.message}`, 'error');
  } finally {
    btn.disabled = false;
    btn.textContent = '刷新';
  }
}

/**
 * 渲染侧边栏打卡列表
 */
function renderCheckInList() {
  const listEl = document.getElementById('checkInList');
  if (state.checkIns.length === 0) {
    listEl.innerHTML = '<div class="empty-list">暂无打卡记录</div>';
    return;
  }

  listEl.innerHTML = state.checkIns.map((ci) => {
    const timeStr = ci.checkedAt
      ? new Date(ci.checkedAt).toLocaleString('zh-CN', { month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' })
      : '—';
    return `
      <div class="checkin-item" onclick="flyToCheckIn(${ci.lng}, ${ci.lat})">
        <div class="checkin-name">📍 ${escapeHtml(ci.name || '打卡')}</div>
        <div class="checkin-meta">
          <span>${timeStr}</span>
          <span>${ci.lng.toFixed(4)}, ${ci.lat.toFixed(4)}</span>
          <span class="checkin-delete" onclick="deleteCheckIn(event, ${ci.id})">×</span>
        </div>
        ${ci.note ? `<div class="checkin-note">${escapeHtml(ci.note)}</div>` : ''}
      </div>
    `;
  }).join('');
}

/**
 * 将打卡数据渲染到地图图层
 */
async function updateCheckInLayer() {
  await mapReadyPromise;
  const fc = {
    type: 'FeatureCollection',
    features: state.checkIns.map((ci) => ({
      type: 'Feature',
      geometry: { type: 'Point', coordinates: [ci.lng, ci.lat] },
      properties: {
        id: ci.id,
        name: ci.name || '打卡',
        note: ci.note || '',
        checkedAt: ci.checkedAt,
      },
    })),
  };
  state.map.getSource('checkins').setData(fc);
}

/**
 * 飞到打卡点
 */
window.flyToCheckIn = function (lng, lat) {
  state.map.flyTo({ center: [lng, lat], zoom: 16, speed: 1.2 });
};

/**
 * 删除打卡记录
 */
window.deleteCheckIn = async function (e, id) {
  e.stopPropagation();
  try {
    const resp = await api('DELETE', `/api/v1/checkins/${id}`);
    if (resp.code === 0) {
      toast('已删除打卡记录', 'info');
      await loadCheckIns();
    } else {
      toast(`删除失败: ${resp.message}`, 'error');
    }
  } catch (err) {
    toast(`删除失败: ${err.message}`, 'error');
  }
};

/**
 * 地图打卡点点击弹窗
 */
function onCheckInClick(e) {
  if (!e.features || e.features.length === 0) return;
  const props = e.features[0].properties;
  const coords = e.features[0].geometry.coordinates;

  const timeStr = props.checkedAt
    ? new Date(props.checkedAt).toLocaleString('zh-CN')
    : '—';

  new maplibregl.Popup({ closeOnClick: true })
    .setLngLat(coords)
    .setHTML(`
      <div style="font-size:12px;line-height:1.8">
        <strong style="color:#f39c12">📍 ${escapeHtml(props.name)}</strong><br>
        ${props.note ? `<span style="color:#aaa">${escapeHtml(props.note)}</span><br>` : ''}
        <span style="color:#888">${timeStr}</span>
      </div>
    `)
    .addTo(state.map);
}

// ===== 状态栏 =====
function updateStatus(type, status) {
  if (type === 'backend') {
    const el = document.getElementById('statusBackend');
    const dot = el.querySelector('.status-dot');
    const text = el.querySelector('.status-text');

    dot.className = 'status-dot';
    if (status === 'connected') {
      dot.classList.add('connected');
      text.textContent = '后端已连接';
    } else if (status === 'checking') {
      dot.classList.add('connecting');
      text.textContent = '检查中...';
    } else {
      dot.classList.add('disconnected');
      text.textContent = '后端未连接';
    }
  }

  if (type === 'ws') {
    const el = document.getElementById('statusWs');
    const dot = el.querySelector('.status-dot');
    const text = el.querySelector('.status-text');

    dot.className = 'status-dot';
    if (status === 'connected') {
      dot.classList.add('connected');
      text.textContent = 'WebSocket 已连接';
    } else if (status === 'connecting') {
      dot.classList.add('connecting');
      text.textContent = '连接中...';
    } else {
      dot.classList.add('disconnected');
      text.textContent = 'WebSocket 未连接';
    }
  }

  // 更新最后更新时间
  document.getElementById('statusTime').textContent =
    `更新: ${new Date().toLocaleTimeString('zh-CN')}`;
}

// ===== Toast 通知 =====
function toast(message, type) {
  const container = document.getElementById('toastContainer');
  const el = document.createElement('div');
  el.className = `toast ${type || 'info'}`;
  el.textContent = message;
  container.appendChild(el);

  setTimeout(() => {
    el.style.opacity = '0';
    el.style.transition = 'opacity 0.3s';
    setTimeout(() => el.remove(), 300);
  }, 3000);
}

// ===== 工具函数 =====
function emptyFeatureCollection() {
  return { type: 'FeatureCollection', features: [] };
}

function escapeHtml(str) {
  const div = document.createElement('div');
  div.textContent = str;
  return div.innerHTML;
}
