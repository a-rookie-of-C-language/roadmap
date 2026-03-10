package com.roadmap.controller;

import com.roadmap.dto.ApiResponse;
import com.roadmap.dto.LocationBatchDTO;
import com.roadmap.dto.LocationDTO;
import com.roadmap.service.LocationService;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/locations")
public class LocationController {

    private final LocationService locationService;

    public LocationController(LocationService locationService) {
        this.locationService = locationService;
    }

    @PostMapping("/batch")
    public ApiResponse<Map<String, Object>> saveBatch(@RequestBody LocationBatchDTO batchDTO) {
        int saved = locationService.saveBatch(batchDTO);
        return ApiResponse.success("saved", Map.of("saved", saved));
    }

    @GetMapping("/latest/{userId}")
    public ApiResponse<LocationDTO> latest(@PathVariable Long userId) {
        return ApiResponse.success(locationService.getLatestLocation(userId));
    }

    @PostMapping("/single")
    public ApiResponse<Map<String, Object>> saveSingle(@RequestBody LocationDTO locationDTO) {
        LocationBatchDTO batchDTO = new LocationBatchDTO();
        batchDTO.getLocations().add(locationDTO);
        int saved = locationService.saveBatch(batchDTO);
        return ApiResponse.success("saved", Map.of("saved", saved));
    }
}
