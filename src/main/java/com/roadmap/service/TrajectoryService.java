package com.roadmap.service;

import com.roadmap.entity.Trip;
import com.roadmap.repository.GpsPointRepository;
import com.roadmap.repository.TripRepository;
import jakarta.transaction.Transactional;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.OffsetDateTime;
import java.util.List;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;
import org.springframework.stereotype.Service;

@Service
public class TrajectoryService {

    private final GpsPointRepository gpsPointRepository;
    private final TripRepository tripRepository;

    @PersistenceContext
    private EntityManager entityManager;

    public TrajectoryService(GpsPointRepository gpsPointRepository, TripRepository tripRepository) {
        this.gpsPointRepository = gpsPointRepository;
        this.tripRepository = tripRepository;
    }

    public String getTrajectoryGeoJson(Long userId, OffsetDateTime start, OffsetDateTime end, double tolerance) {
        String geojson = gpsPointRepository.getTrajectoryGeoJson(userId, start, end, tolerance);
        return geojson == null ? "{\"type\":\"LineString\",\"coordinates\":[]}" : geojson;
    }

    public String getPointsGeoJson(Long userId, OffsetDateTime start, OffsetDateTime end) {
        String geojson = gpsPointRepository.getPointsAsGeoJson(userId, start, end);
        return geojson == null
                ? "{\"type\":\"FeatureCollection\",\"features\":[]}"
                : geojson;
    }

    @Transactional
    public Trip startTrip(Long userId, String deviceId, String name) {
        return startTrip(userId, deviceId, name, OffsetDateTime.now());
    }

    @Transactional
    public Trip startTrip(Long userId, String deviceId, String name, OffsetDateTime startTime) {
        Trip trip = new Trip();
        trip.setUserId(userId);
        trip.setDeviceId(deviceId);
        trip.setName(name);
        trip.setStatus("recording");
        trip.setStartTime(startTime);
        return tripRepository.save(trip);
    }

    @Transactional
    public Trip endTrip(Long tripId) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new IllegalArgumentException("Trip not found: " + tripId));
        trip.setStatus("completed");
        trip.setEndTime(OffsetDateTime.now());
        Trip saved = tripRepository.save(trip);
        entityManager.createNativeQuery("SELECT aggregate_trip(:tripId)")
                .setParameter("tripId", tripId)
                .getSingleResult();
        return saved;
    }

    @Transactional
    public Trip endTrip(Long tripId, OffsetDateTime endTime) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new IllegalArgumentException("Trip not found: " + tripId));
        trip.setStatus("completed");
        trip.setEndTime(endTime);
        Trip saved = tripRepository.save(trip);
        entityManager.createNativeQuery("SELECT aggregate_trip(:tripId)")
                .setParameter("tripId", tripId)
                .getSingleResult();
        return saved;
    }

    public List<Trip> getTripsByUser(Long userId) {
        return tripRepository.findByUserIdOrderByStartTimeDesc(userId);
    }

    public String getTripTrajectory(Long tripId, double tolerance) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new IllegalArgumentException("Trip not found: " + tripId));
        LineString lineString = trip.getTrajectory() != null ? trip.getTrajectory() : trip.getSimplifiedGeom();
        if (lineString == null) {
            return "{\"type\":\"LineString\",\"coordinates\":[]}";
        }
        return lineStringToGeoJson(lineString, tolerance);
    }

    private String lineStringToGeoJson(LineString lineString, double tolerance) {
        Coordinate[] coordinates = lineString.getCoordinates();
        StringBuilder builder = new StringBuilder();
        builder.append("{\"type\":\"LineString\",\"coordinates\":[");
        for (int i = 0; i < coordinates.length; i++) {
            Coordinate coordinate = coordinates[i];
            if (i > 0) {
                builder.append(',');
            }
            builder.append('[')
                    .append(trimDecimal(coordinate.getX(), tolerance))
                    .append(',')
                    .append(trimDecimal(coordinate.getY(), tolerance))
                    .append(']');
        }
        builder.append("]}");
        return builder.toString();
    }

    private String trimDecimal(double value, double tolerance) {
        int decimals = tolerance <= 0 ? 6 : Math.max(1, Math.min(8, (int) Math.ceil(-Math.log10(tolerance))));
        return String.format("%1$." + decimals + "f", value);
    }
}
