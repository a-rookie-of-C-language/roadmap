package com.roadmap.controller;

import com.roadmap.dto.ApiResponse;
import com.roadmap.dto.LocationDTO;
import com.roadmap.entity.Device;
import com.roadmap.service.DeviceService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/devices")
public class DeviceController {

    private final DeviceService deviceService;

    public DeviceController(DeviceService deviceService) {
        this.deviceService = deviceService;
    }

    @PostMapping("/register")
    public ApiResponse<Device> register(@RequestBody RegisterDeviceRequest request) {
        Device device = deviceService.registerDevice(
                request.getUserId(),
                request.getDeviceId(),
                request.getName(),
                request.getType()
        );
        return ApiResponse.success(device);
    }

    @GetMapping("/user/{userId}")
    public ApiResponse<List<Device>> getByUser(@PathVariable Long userId) {
        return ApiResponse.success(deviceService.getDevicesByUser(userId));
    }

    @GetMapping("/{deviceId}/location")
    public ApiResponse<LocationDTO> getDeviceLocation(@PathVariable String deviceId) {
        return ApiResponse.success(deviceService.getDeviceLocation(deviceId));
    }

    public static class RegisterDeviceRequest {
        private Long userId;
        private String deviceId;
        private String name;
        private String type;

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

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }
    }
}
