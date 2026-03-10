package com.roadmap.repository;

import com.roadmap.entity.CheckIn;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CheckInRepository extends JpaRepository<CheckIn, Long> {

    List<CheckIn> findByUserIdOrderByCheckedAtDesc(Long userId);

    /**
     * 查询指定坐标附近 radiusMeters 米内的打卡记录
     */
    @Query(value = """
            SELECT * FROM check_ins
            WHERE user_id = :userId
              AND ST_DWithin(geom::geography,
                             ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)::geography,
                             :radiusMeters)
            ORDER BY checked_at DESC
            """, nativeQuery = true)
    List<CheckIn> findNearby(
            @Param("userId") Long userId,
            @Param("lng") double lng,
            @Param("lat") double lat,
            @Param("radiusMeters") double radiusMeters
    );
}
