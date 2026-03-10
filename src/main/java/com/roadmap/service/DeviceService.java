package com.roadmap.service;

import com.roadmap.dto.LocationDTO;
import com.roadmap.entity.Device;
import com.roadmap.repository.DeviceRepository;
import jakarta.transaction.Transactional;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class DeviceService {

    private final DeviceRepository deviceRepository;

    public DeviceService(DeviceRepository deviceRepository) {
        this.deviceRepository = deviceRepository;
    }

    @Transactional
    public Device registerDevice(Long userId, String deviceId, String name, String type) {
        Device device = deviceRepository.findByDeviceId(deviceId).orElseGet(Device::new);
        device.setUserId(userId);
        device.setDeviceId(deviceId);
        device.setDeviceName(name);
        device.setDeviceType(type);
        if (device.getLastSeen() == null) {
            device.setLastSeen(OffsetDateTime.now());
        }
        return deviceRepository.save(device);
    }

    public List<Device> getDevicesByUser(Long userId) {
        return deviceRepository.findByUserId(userId);
    }

    public LocationDTO getDeviceLocation(String deviceId) {
        Device device = deviceRepository.findByDeviceId(deviceId).orElse(null);
        if (device == null || device.getLastLocation() == null) {
            return null;
        }
        LocationDTO dto = new LocationDTO();
        dto.setUserId(device.getUserId());
        dto.setDeviceId(device.getDeviceId());
        dto.setLng(device.getLastLocation().getX());
        dto.setLat(device.getLastLocation().getY());
        dto.setRecordedAt(device.getLastSeen());
        dto.setCoordType("wgs84");
        return dto;
    }
}
