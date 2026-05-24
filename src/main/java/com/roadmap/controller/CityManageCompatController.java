package com.roadmap.controller;

import com.roadmap.dto.CheckInDTO;
import com.roadmap.dto.compat.CowMeta;
import com.roadmap.dto.compat.CowResponse;
import com.roadmap.dto.compat.FacilityListItemDTO;
import com.roadmap.dto.compat.PatrolAddRequest;
import com.roadmap.dto.compat.PatrolAddResultDTO;
import com.roadmap.service.CheckInService;
import com.roadmap.service.FacilityService;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/cow-ywgateway/citymanage")
public class CityManageCompatController {

    private final CheckInService checkInService;
    private final FacilityService facilityService;

    public CityManageCompatController(CheckInService checkInService, FacilityService facilityService) {
        this.checkInService = checkInService;
        this.facilityService = facilityService;
    }

    @GetMapping("/fac-management")
    public CowResponse<List<FacilityListItemDTO>> getFacilityManagement(
            @RequestParam(required = false) String typeid,
            @RequestParam(required = false) String dq,
            @RequestParam(defaultValue = "1") Integer pageNo,
            @RequestParam(defaultValue = "20") Integer pageSize,
            @RequestParam(required = false) String keyword
    ) {
        List<FacilityListItemDTO> data = facilityService.queryForCompat(typeid, dq, keyword, pageNo, pageSize);
        long total = facilityService.countForCompat(typeid, dq, keyword);
        CowMeta meta = new CowMeta(total, pageNo, pageSize);
        return CowResponse.of(data, meta);
    }

    @GetMapping("/fac-management/count")
    public CowResponse<Map<String, Object>> getFacilityManagementCount(
            @RequestParam(required = false) String dq,
            @RequestParam(required = false) String typeid,
            @RequestParam(required = false) String keyword
    ) {
        Map<String, Object> data = new HashMap<>();
        data.put("total", facilityService.countForCompat(typeid, dq, keyword));
        return CowResponse.of(data);
    }

    @GetMapping({"/bridge/{id}", "/tunnel/{id}", "/culver/{id}", "/footbridge/{id}",
            "/personsouterrain/{id}", "/carsouterrain/{id}", "/barricade/{id}", "/lighting/{id}"})
    public CowResponse<Map<String, Object>> getFacilityDetailByType(@PathVariable String id) {
        return CowResponse.of(facilityService.getCompatDetail(id));
    }

    @PostMapping("/dailypatrol/add")
    public CowResponse<PatrolAddResultDTO> addPatrol(@RequestBody PatrolAddRequest request) {
        if (request.getUserId() == null || request.getLng() == null || request.getLat() == null) {
            throw new IllegalArgumentException("userId/lng/lat 不能为空");
        }

        Long facilityId = request.getFacilityId() == null ? null : Long.parseLong(request.getFacilityId());
        double radiusMeters = facilityId == null ? 100D : facilityService.resolveRadiusMeters(facilityId);
        double distanceMeters = facilityId == null
                ? Double.MAX_VALUE
                : facilityService.distanceMeters(facilityId, request.getLng(), request.getLat(), request.getCoordType());

        CheckInDTO checkInDTO = new CheckInDTO();
        checkInDTO.setUserId(request.getUserId());
        checkInDTO.setDeviceId(request.getDeviceId());
        checkInDTO.setName("巡检打卡");
        checkInDTO.setNote(buildPatrolNote(request));
        checkInDTO.setLng(request.getLng());
        checkInDTO.setLat(request.getLat());
        checkInDTO.setCoordType(request.getCoordType() == null ? "wgs84" : request.getCoordType());
        checkInDTO.setCheckedAt(request.getTimestamp() == null ? OffsetDateTime.now() : request.getTimestamp());

        CheckInDTO saved = checkInService.checkIn(checkInDTO);

        PatrolAddResultDTO result = new PatrolAddResultDTO();
        result.setSuccess(distanceMeters <= radiusMeters);
        result.setDistanceMeters(distanceMeters);
        result.setRadiusMeters(radiusMeters);
        result.setCheckInId(saved.getId() == null ? null : String.valueOf(saved.getId()));
        return CowResponse.of(result);
    }

    @GetMapping("/dailypatrol")
    public CowResponse<List<CheckInDTO>> listPatrol(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String facilityId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime dateTo,
            @RequestParam(defaultValue = "1") Integer pageNo,
            @RequestParam(defaultValue = "20") Integer pageSize
    ) {
        List<CheckInDTO> all = userId == null ? List.of() : checkInService.getCheckInsByUser(userId);
        List<CheckInDTO> filtered = all.stream()
                .filter(c -> facilityId == null || facilityId.isBlank() || hasFacilityId(c.getNote(), facilityId))
                .filter(c -> dateFrom == null || (c.getCheckedAt() != null && !c.getCheckedAt().isBefore(dateFrom)))
                .filter(c -> dateTo == null || (c.getCheckedAt() != null && !c.getCheckedAt().isAfter(dateTo)))
                .collect(Collectors.toList());

        int safePageNo = Math.max(pageNo, 1);
        int safePageSize = Math.max(pageSize, 1);
        int from = (safePageNo - 1) * safePageSize;
        List<CheckInDTO> pageData = from >= filtered.size()
                ? List.of()
                : filtered.subList(from, Math.min(from + safePageSize, filtered.size()));

        CowMeta meta = new CowMeta(filtered.size(), pageNo, pageSize);
        return CowResponse.of(pageData, meta);
    }

    private String buildPatrolNote(PatrolAddRequest request) {
        StringBuilder note = new StringBuilder();
        if (request.getFacilityId() != null) {
            note.append("facilityId=").append(request.getFacilityId());
        }
        if (request.getTaskId() != null) {
            if (note.length() > 0) {
                note.append("; ");
            }
            note.append("taskId=").append(request.getTaskId());
        }
        return note.toString();
    }

    private boolean hasFacilityId(String note, String facilityId) {
        return note != null && note.contains("facilityId=" + facilityId);
    }
}
