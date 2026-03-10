package com.roadmap.repository;

import com.roadmap.entity.GpsPoint;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GpsPointRepository extends JpaRepository<GpsPoint, Long> {

    List<GpsPoint> findByUserIdAndRecordedAtBetweenOrderByRecordedAtAsc(
            Long userId,
            OffsetDateTime start,
            OffsetDateTime end
    );

    @Query(value = """
            SELECT ST_AsGeoJSON(ST_SimplifyPreserveTopology(ST_MakeLine(geom ORDER BY recorded_at), :tolerance))
            FROM gps_points WHERE user_id = :userId AND recorded_at BETWEEN :startTime AND :endTime
            """, nativeQuery = true)
    String getTrajectoryGeoJson(
            @Param("userId") Long userId,
            @Param("startTime") OffsetDateTime startTime,
            @Param("endTime") OffsetDateTime endTime,
            @Param("tolerance") double tolerance
    );

    @Query(value = "SELECT * FROM gps_points WHERE user_id = :userId ORDER BY recorded_at DESC LIMIT 1", nativeQuery = true)
    GpsPoint getLatestPoint(@Param("userId") Long userId);

    @Query(value = """
            SELECT json_build_object('type','FeatureCollection','features',
              json_agg(json_build_object('type','Feature','geometry',CAST(ST_AsGeoJSON(geom) AS json),
              'properties',json_build_object('speed',speed,'accuracy',accuracy,'altitude',altitude,
              'heading',heading,'recorded_at',recorded_at))))
            FROM gps_points WHERE user_id = :userId AND recorded_at BETWEEN :startTime AND :endTime
            """, nativeQuery = true)
    String getPointsAsGeoJson(
            @Param("userId") Long userId,
            @Param("startTime") OffsetDateTime startTime,
            @Param("endTime") OffsetDateTime endTime
    );
}
