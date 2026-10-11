package com.visasetu.repository;

import com.visasetu.domain.entity.HelpTicket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface HelpTicketRepository extends JpaRepository<HelpTicket, UUID> {
    Optional<HelpTicket> findByTicketId(String ticketId);
    List<HelpTicket> findByEmailIgnoreCaseOrderByCreatedAtDesc(String email);
}
