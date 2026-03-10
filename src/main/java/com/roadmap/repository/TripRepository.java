package com.roadmap.repository;

import com.roadmap.entity.Trip;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TripRepository extends JpaRepository<Trip, Long> {

    List<Trip> findByUserIdOrderByStartTimeDesc(Long userId);

    List<Trip> findByUserIdAndStatus(Long userId, String status);
}
