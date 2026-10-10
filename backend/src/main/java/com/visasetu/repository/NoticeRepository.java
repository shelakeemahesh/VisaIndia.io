package com.visasetu.repository;

import com.visasetu.domain.entity.Notice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface NoticeRepository extends JpaRepository<Notice, UUID> {
    Optional<Notice> findByNoticeId(String noticeId);
    List<Notice> findByOrderByPublishedAtDesc();
    List<Notice> findByIsPinnedHomeTrueOrderByPublishedAtDesc();
    List<Notice> findByCategoryOrderByPublishedAtDesc(String category);
}
