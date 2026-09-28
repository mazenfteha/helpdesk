package com.mazen.helpdesk.repository;

import com.mazen.helpdesk.entity.TicketCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;
import java.util.List;

public interface TicketCategoryRepository extends JpaRepository<TicketCategory, UUID> {
    List<TicketCategory> findAllByOrderByNameAsc();
}