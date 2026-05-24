package com.roadmap.repository;

import com.roadmap.entity.Facility;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface FacilityRepository extends JpaRepository<Facility, Long> {

    @Query(value = """
            SELECT ST_DistanceSphere(
                geom,
                ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)
            )
            FROM facilities
            WHERE id = :facilityId
            """, nativeQuery = true)
    Double distanceMeters(
            @Param("facilityId") Long facilityId,
            @Param("lng") double lng,
            @Param("lat") double lat
    );

    Optional<Facility> findById(Long id);
}
