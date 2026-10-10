package com.visasetu.repository;

import com.visasetu.domain.entity.Faq;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface FaqRepository extends JpaRepository<Faq, UUID> {
    List<Faq> findByTopicOrderByOrderNumAsc(String topic);
    List<Faq> findAllByOrderByOrderNumAsc();

    @Query("SELECT f FROM Faq f WHERE LOWER(f.question) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(f.answer) LIKE LOWER(CONCAT('%', :query, '%')) ORDER BY f.orderNum ASC")
    List<Faq> searchFaqs(String query);
}
