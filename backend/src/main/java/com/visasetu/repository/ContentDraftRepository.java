package com.visasetu.repository;

import com.visasetu.domain.entity.ContentDraft;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ContentDraftRepository extends JpaRepository<ContentDraft, UUID> {
    List<ContentDraft> findBySectionKey(String sectionKey);
    Optional<ContentDraft> findBySectionKeyAndContentHash(String sectionKey, String contentHash);
    List<ContentDraft> findByIsPublishedTrue();
}
