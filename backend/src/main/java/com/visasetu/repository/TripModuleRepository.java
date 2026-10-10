package com.visasetu.repository;

import com.visasetu.domain.entity.TripModule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TripModuleRepository extends JpaRepository<TripModule, UUID> {
    List<TripModule> findByTripId(UUID tripId);
}
