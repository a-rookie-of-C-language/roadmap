package com.roadmap.service;

import com.roadmap.dto.CheckInDTO;
import com.roadmap.entity.CheckIn;
import com.roadmap.repository.CheckInRepository;
import com.roadmap.util.CoordTransformUtil;
import jakarta.transaction.Transactional;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.stereotype.Service;

@Service
public class CheckInService {

    private final CheckInRepository checkInRepository;
    private final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);

    public CheckInService(CheckInRepository checkInRepository) {
        this.checkInRepository = checkInRepository;
    }

    @Transactional
    public CheckInDTO checkIn(CheckInDTO dto) {
        if (dto.getUserId() == null) {
            throw new IllegalArgumentException("userId 不能为空");
        }

        // 坐标转换
        double[] wgs84 = toWgs84(dto.getLng(), dto.getLat(), dto.getCoordType());
        Point point = geometryFactory.createPoint(new Coordinate(wgs84[0], wgs84[1]));
        point.setSRID(4326);

        CheckIn entity = new CheckIn();
        entity.setUserId(dto.getUserId());
        entity.setDeviceId(dto.getDeviceId());
        entity.setName(dto.getName() == null || dto.getName().isBlank() ? "打卡" : dto.getName());
        entity.setAddress(dto.getAddress());
        entity.setNote(dto.getNote());
        entity.setGeom(point);
        entity.setCheckedAt(dto.getCheckedAt() != null ? dto.getCheckedAt() : OffsetDateTime.now());

        CheckIn saved = checkInRepository.save(entity);
        return toDTO(saved);
    }

    public List<CheckInDTO> getCheckInsByUser(Long userId) {
        return checkInRepository.findByUserIdOrderByCheckedAtDesc(userId)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public List<CheckInDTO> getNearby(Long userId, double lng, double lat, double radiusMeters) {
        double[] wgs84 = toWgs84(lng, lat, "wgs84");
        return checkInRepository.findNearby(userId, wgs84[0], wgs84[1], radiusMeters)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteCheckIn(Long id) {
        checkInRepository.deleteById(id);
    }

    // ===== 内部工具方法 =====

    private CheckInDTO toDTO(CheckIn entity) {
        CheckInDTO dto = new CheckInDTO();
        dto.setId(entity.getId());
        dto.setUserId(entity.getUserId());
        dto.setDeviceId(entity.getDeviceId());
        dto.setName(entity.getName());
        dto.setAddress(entity.getAddress());
        dto.setNote(entity.getNote());
        dto.setLng(entity.getGeom().getX());
        dto.setLat(entity.getGeom().getY());
        dto.setCheckedAt(entity.getCheckedAt());
        dto.setCoordType("wgs84");
        return dto;
    }

    private double[] toWgs84(double lng, double lat, String coordType) {
        return CoordTransformUtil.toWgs84(lng, lat, coordType);
    }
}
