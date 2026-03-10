package com.roadmap.service;

import com.roadmap.dto.LocationBatchDTO;
import com.roadmap.dto.LocationDTO;
import com.roadmap.entity.Device;
import com.roadmap.entity.GpsPoint;
import com.roadmap.repository.DeviceRepository;
import com.roadmap.repository.GpsPointRepository;
import com.roadmap.util.CoordTransformUtil;
import com.roadmap.websocket.LocationWebSocketHandler;
import jakarta.transaction.Transactional;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.stereotype.Service;

@Service
public class LocationService {

    private final GpsPointRepository gpsPointRepository;
    private final DeviceRepository deviceRepository;
    private final LocationWebSocketHandler locationWebSocketHandler;
    private final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);

    public LocationService(
            GpsPointRepository gpsPointRepository,
            DeviceRepository deviceRepository,
            LocationWebSocketHandler locationWebSocketHandler
    ) {
        this.gpsPointRepository = gpsPointRepository;
        this.deviceRepository = deviceRepository;
        this.locationWebSocketHandler = locationWebSocketHandler;
    }

    @Transactional
    public int saveBatch(LocationBatchDTO batch) {
        if (batch == null || batch.getLocations() == null || batch.getLocations().isEmpty()) {
            return 0;
        }

        List<GpsPoint> points = new ArrayList<>();
        for (LocationDTO location : batch.getLocations()) {
            if (location.getUserId() == null || location.getDeviceId() == null || location.getDeviceId().isBlank()) {
                continue;
            }

            double[] wgs84 = toWgs84(location.getLng(), location.getLat(), location.getCoordType());
            Point point = geometryFactory.createPoint(new Coordinate(wgs84[0], wgs84[1]));
            point.setSRID(4326);

            GpsPoint gpsPoint = new GpsPoint();
            gpsPoint.setUserId(location.getUserId());
            gpsPoint.setDeviceId(location.getDeviceId());
            gpsPoint.setGeom(point);
            gpsPoint.setAltitude(location.getAltitude());
            gpsPoint.setSpeed(location.getSpeed());
            gpsPoint.setAccuracy(location.getAccuracy());
            gpsPoint.setHeading(location.getHeading());
            gpsPoint.setRecordedAt(location.getRecordedAt() == null ? OffsetDateTime.now() : location.getRecordedAt());
            points.add(gpsPoint);

            upsertDevice(location, point, gpsPoint.getRecordedAt());

            LocationDTO pushPayload = new LocationDTO();
            pushPayload.setUserId(location.getUserId());
            pushPayload.setDeviceId(location.getDeviceId());
            pushPayload.setLng(wgs84[0]);
            pushPayload.setLat(wgs84[1]);
            pushPayload.setAltitude(location.getAltitude());
            pushPayload.setSpeed(location.getSpeed());
            pushPayload.setAccuracy(location.getAccuracy());
            pushPayload.setHeading(location.getHeading());
            pushPayload.setRecordedAt(gpsPoint.getRecordedAt());
            pushPayload.setCoordType("wgs84");
            locationWebSocketHandler.broadcastLocation(location.getUserId(), pushPayload);
        }

        gpsPointRepository.saveAll(points);
        return points.size();
    }

    public LocationDTO getLatestLocation(Long userId) {
        GpsPoint point = gpsPointRepository.getLatestPoint(userId);
        if (point == null) {
            return null;
        }
        LocationDTO dto = new LocationDTO();
        dto.setUserId(point.getUserId());
        dto.setDeviceId(point.getDeviceId());
        dto.setLng(point.getGeom().getX());
        dto.setLat(point.getGeom().getY());
        dto.setAltitude(point.getAltitude());
        dto.setSpeed(point.getSpeed());
        dto.setAccuracy(point.getAccuracy());
        dto.setHeading(point.getHeading());
        dto.setRecordedAt(point.getRecordedAt());
        dto.setCoordType("wgs84");
        return dto;
    }

    private double[] toWgs84(double lng, double lat, String coordType) {
        return CoordTransformUtil.toWgs84(lng, lat, coordType);
    }

    private void upsertDevice(LocationDTO location, Point point, OffsetDateTime lastSeen) {
        Optional<Device> existing = deviceRepository.findByDeviceId(location.getDeviceId());
        Device device = existing.orElseGet(Device::new);
        device.setUserId(location.getUserId());
        device.setDeviceId(location.getDeviceId());
        if (device.getDeviceName() == null) {
            device.setDeviceName(location.getDeviceId());
        }
        if (device.getDeviceType() == null) {
            device.setDeviceType("tracker");
        }
        device.setLastLocation(point);
        device.setLastSeen(lastSeen);
        deviceRepository.save(device);
    }
}
