package com.roadmap.controller;

import com.roadmap.dto.ApiResponse;
import com.roadmap.dto.LocationBatchDTO;
import com.roadmap.dto.LocationDTO;
import com.roadmap.dto.TrajectoryImportDTO;
import com.roadmap.entity.Trip;
import com.roadmap.service.LocationService;
import com.roadmap.service.TrajectoryService;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/trajectories")
public class TrajectoryController {

    private final TrajectoryService trajectoryService;
    private final LocationService locationService;

    public TrajectoryController(TrajectoryService trajectoryService, LocationService locationService) {
        this.trajectoryService = trajectoryService;
        this.locationService = locationService;
    }

    @GetMapping("/{userId}")
    public ApiResponse<String> getTrajectory(
            @PathVariable Long userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime end,
            @RequestParam(defaultValue = "0.0001") double tolerance
    ) {
        return ApiResponse.success(trajectoryService.getTrajectoryGeoJson(userId, start, end, tolerance));
    }

    @GetMapping("/{userId}/points")
    public ApiResponse<String> getPoints(
            @PathVariable Long userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime end
    ) {
        return ApiResponse.success(trajectoryService.getPointsGeoJson(userId, start, end));
    }

    @PostMapping("/trips")
    public ApiResponse<Trip> startTrip(@RequestBody TripStartRequest request) {
        return ApiResponse.success(trajectoryService.startTrip(request.getUserId(), request.getDeviceId(), request.getName()));
    }

    @PutMapping("/trips/{tripId}/end")
    public ApiResponse<Trip> endTrip(@PathVariable Long tripId) {
        return ApiResponse.success(trajectoryService.endTrip(tripId));
    }

    @GetMapping("/trips/user/{userId}")
    public ApiResponse<List<Trip>> getTripsByUser(@PathVariable Long userId) {
        return ApiResponse.success(trajectoryService.getTripsByUser(userId));
    }

    @GetMapping("/trips/{tripId}/trajectory")
    public ApiResponse<String> getTripTrajectory(
            @PathVariable Long tripId,
            @RequestParam(defaultValue = "0.0001") double tolerance
    ) {
        return ApiResponse.success(trajectoryService.getTripTrajectory(tripId, tolerance));
    }

    /**
     * 导入轨迹 — 接收若干坐标点，创建 Trip 并聚合轨迹
     */
    @PostMapping("/import")
    public ApiResponse<Map<String, Object>> importTrajectory(@RequestBody TrajectoryImportDTO request) {
        if (request.getPoints() == null || request.getPoints().size() < 2) {
            return ApiResponse.error(400, "至少需要2个坐标点");
        }

        Long userId = request.getUserId() != null ? request.getUserId() : 1L;
        String deviceId = request.getDeviceId() != null ? request.getDeviceId() : "import-" + userId;
        String name = request.getName() != null ? request.getName() : "导入轨迹-" + OffsetDateTime.now();
        String coordType = request.getCoordType() != null ? request.getCoordType() : "wgs84";
        int intervalSeconds = 5;

        // 时间窗口：以当前时间为终点，往前推
        int pointCount = request.getPoints().size();
        OffsetDateTime start = OffsetDateTime.now().minusSeconds((long) pointCount * intervalSeconds);
        OffsetDateTime endTime = start.plusSeconds((long) (pointCount - 1) * intervalSeconds);

        // 创建 Trip（时间略微扩展以覆盖所有点）
        Trip trip = trajectoryService.startTrip(userId, deviceId, name, start.minusSeconds(1));

        // 构建 GPS 点批量数据
        LocationBatchDTO batch = new LocationBatchDTO();
        for (int i = 0; i < pointCount; i++) {
            TrajectoryImportDTO.PointDTO p = request.getPoints().get(i);
            LocationDTO loc = new LocationDTO();
            loc.setUserId(userId);
            loc.setDeviceId(deviceId);
            loc.setLng(p.getLng());
            loc.setLat(p.getLat());
            loc.setAltitude(p.getAltitude());
            loc.setSpeed(p.getSpeed());
            loc.setAccuracy(p.getAccuracy());
            loc.setRecordedAt(start.plusSeconds((long) i * intervalSeconds));
            loc.setCoordType(coordType);
            batch.getLocations().add(loc);
        }

        int saved = locationService.saveBatch(batch);
        trajectoryService.endTrip(trip.getId(), endTime.plusSeconds(1));

        Map<String, Object> result = new HashMap<>();
        result.put("tripId", trip.getId());
        result.put("pointCount", saved);
        return ApiResponse.success(result);
    }

    public static class TripStartRequest {
        private Long userId;
        private String deviceId;
        private String name;

        public Long getUserId() {
            return userId;
        }

        public void setUserId(Long userId) {
            this.userId = userId;
        }

        public String getDeviceId() {
            return deviceId;
        }

        public void setDeviceId(String deviceId) {
            this.deviceId = deviceId;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }
}
