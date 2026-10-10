package com.visasetu.repository;

import com.visasetu.domain.entity.VisaCategoryDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface VisaCategoryDetailRepository extends JpaRepository<VisaCategoryDetail, UUID> {
    Optional<VisaCategoryDetail> findBySlug(String slug);
    List<VisaCategoryDetail> findByPurposeCode(String purposeCode);
}
