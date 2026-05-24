package com.roadmap.service;

import com.roadmap.dto.compat.FacilityListItemDTO;
import com.roadmap.entity.Facility;
import com.roadmap.repository.FacilityRepository;
import com.roadmap.util.CoordTransformUtil;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class FacilityService {

    private final FacilityRepository facilityRepository;

    public FacilityService(FacilityRepository facilityRepository) {
        this.facilityRepository = facilityRepository;
    }

    public List<FacilityListItemDTO> queryForCompat(String typeid, String dq, String keyword, int pageNo, int pageSize) {
        List<Facility> filtered = facilityRepository.findAll().stream()
                .filter(f -> typeid == null || typeid.isBlank() || typeid.equals(f.getTypeid()))
                .filter(f -> dq == null || dq.isBlank() || (f.getAdminAreaName() != null && f.getAdminAreaName().contains(dq)))
                .filter(f -> keyword == null || keyword.isBlank() || matchKeyword(f, keyword))
                .collect(Collectors.toList());

        int safePageNo = Math.max(pageNo, 1);
        int safePageSize = Math.max(pageSize, 1);
        int from = (safePageNo - 1) * safePageSize;
        if (from >= filtered.size()) {
            return List.of();
        }
        int to = Math.min(from + safePageSize, filtered.size());
        return filtered.subList(from, to).stream().map(this::toCompatItem).collect(Collectors.toList());
    }

    public long countForCompat(String typeid, String dq, String keyword) {
        return facilityRepository.findAll().stream()
                .filter(f -> typeid == null || typeid.isBlank() || typeid.equals(f.getTypeid()))
                .filter(f -> dq == null || dq.isBlank() || (f.getAdminAreaName() != null && f.getAdminAreaName().contains(dq)))
                .filter(f -> keyword == null || keyword.isBlank() || matchKeyword(f, keyword))
                .count();
    }

    public Map<String, Object> getCompatDetail(String id) {
        Facility facility = facilityRepository.findById(Long.parseLong(id))
                .orElseThrow(() -> new NoSuchElementException("facility not found: " + id));
        Map<String, Object> data = new HashMap<>();
        data.put("id", String.valueOf(facility.getId()));
        data.put("mc", Optional.ofNullable(facility.getMc()).orElse(""));
        data.put("typeid", Optional.ofNullable(facility.getTypeid()).orElse(""));
        data.put("adminAreaName", Optional.ofNullable(facility.getAdminAreaName()).orElse(""));
        data.put("szwz", Optional.ofNullable(facility.getSzwz()).orElse(""));
        data.put("gldw", Optional.ofNullable(facility.getGldw()).orElse(""));
        data.put("gldwname", Optional.ofNullable(facility.getGldwname()).orElse(""));
        data.put("yhdw", Optional.ofNullable(facility.getYhdw()).orElse(""));
        data.put("yhdwname", Optional.ofNullable(facility.getYhdwname()).orElse(""));
        data.put("zt", Optional.ofNullable(facility.getZt()).orElse(""));
        data.put("dzdt", toDzdt(facility));
        return data;
    }

    public double distanceMeters(Long facilityId, double lng, double lat, String coordType) {
        double[] wgs84 = CoordTransformUtil.toWgs84(lng, lat, coordType);
        Double dist = facilityRepository.distanceMeters(facilityId, wgs84[0], wgs84[1]);
        return dist == null ? Double.MAX_VALUE : dist;
    }

    public double resolveRadiusMeters(Long facilityId) {
        Facility facility = facilityRepository.findById(facilityId)
                .orElseThrow(() -> new NoSuchElementException("facility not found: " + facilityId));
        return facility.getRadiusMeters() == null ? 100D : facility.getRadiusMeters();
    }

    private FacilityListItemDTO toCompatItem(Facility facility) {
        FacilityListItemDTO item = new FacilityListItemDTO();
        item.setId(String.valueOf(facility.getId()));
        item.setMc(facility.getMc());
        item.setTypeid(facility.getTypeid());
        item.setAdminAreaName(facility.getAdminAreaName());
        item.setSzwz(facility.getSzwz());
        item.setGldw(facility.getGldw());
        item.setGldwname(facility.getGldwname());
        item.setYhdw(facility.getYhdw());
        item.setYhdwname(facility.getYhdwname());
        item.setZt(facility.getZt());
        item.setDzdt(toDzdt(facility));
        return item;
    }

    private String toDzdt(Facility facility) {
        if (facility.getGeom() == null) {
            return "";
        }
        return facility.getGeom().getX() + "," + facility.getGeom().getY();
    }

    private boolean matchKeyword(Facility f, String keyword) {
        String key = keyword.trim();
        return (f.getMc() != null && f.getMc().contains(key))
                || (f.getSzwz() != null && f.getSzwz().contains(key))
                || (f.getAdminAreaName() != null && f.getAdminAreaName().contains(key));
    }
}
