package com.roadmap.controller;

import com.roadmap.dto.ApiResponse;
import com.roadmap.dto.LocationBatchDTO;
import com.roadmap.dto.LocationDTO;
import com.roadmap.entity.Trip;
import com.roadmap.service.LocationService;
import com.roadmap.service.TrajectoryService;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/simulator")
public class SimulatorController {

    private final LocationService locationService;
    private final TrajectoryService trajectoryService;

    public SimulatorController(LocationService locationService, TrajectoryService trajectoryService) {
        this.locationService = locationService;
        this.trajectoryService = trajectoryService;
    }

    @PostMapping("/generate")
    public ApiResponse<Map<String, Object>> generate(
            @RequestParam Long userId,
            @RequestParam(defaultValue = "116.4") double centerLng,
            @RequestParam(defaultValue = "39.9") double centerLat,
            @RequestParam(defaultValue = "100") int points,
            @RequestParam(defaultValue = "5") int intervalSeconds
    ) {
        OffsetDateTime start = OffsetDateTime.now().minusSeconds((long) points * intervalSeconds);
        OffsetDateTime endTime = start.plusSeconds((long) (points - 1) * intervalSeconds);

        String deviceId = "sim-" + userId;
        // Start trip with time BEFORE the first simulated point so aggregate_trip can find them
        Trip trip = trajectoryService.startTrip(userId, deviceId, "simulation-" + OffsetDateTime.now(), start.minusSeconds(1));

        LocationBatchDTO batch = new LocationBatchDTO();
        double currentLng = centerLng;
        double currentLat = centerLat;

        for (int i = 0; i < points; i++) {
            currentLng += randomOffset();
            currentLat += randomOffset();

            LocationDTO location = new LocationDTO();
            location.setUserId(userId);
            location.setDeviceId(deviceId);
            location.setLng(currentLng);
            location.setLat(currentLat);
            location.setSpeed(ThreadLocalRandom.current().nextDouble(1.0, 20.0));
            location.setAccuracy(ThreadLocalRandom.current().nextDouble(5.0, 30.0));
            location.setHeading(ThreadLocalRandom.current().nextDouble(0.0, 360.0));
            location.setAltitude(ThreadLocalRandom.current().nextDouble(10.0, 120.0));
            location.setRecordedAt(start.plusSeconds((long) i * intervalSeconds));
            location.setCoordType("wgs84");
            batch.getLocations().add(location);
        }

        int saved = locationService.saveBatch(batch);
        trajectoryService.endTrip(trip.getId(), endTime.plusSeconds(1));

        Map<String, Object> result = new HashMap<>();
        result.put("tripId", trip.getId());
        result.put("pointCount", saved);
        return ApiResponse.success(result);
    }

    private double randomOffset() {
        double sign = ThreadLocalRandom.current().nextBoolean() ? 1.0 : -1.0;
        return sign * ThreadLocalRandom.current().nextDouble(0.0001, 0.0005);
    }
}
