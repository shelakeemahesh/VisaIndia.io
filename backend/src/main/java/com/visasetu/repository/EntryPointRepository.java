package com.visasetu.repository;

import com.visasetu.domain.entity.EntryPoint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface EntryPointRepository extends JpaRepository<EntryPoint, UUID> {
    List<EntryPoint> findByTypeOrderByStateAscNameAsc(String type);
    List<EntryPoint> findByStateIgnoreCase(String state);
}
