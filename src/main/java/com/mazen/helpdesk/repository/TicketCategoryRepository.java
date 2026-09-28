package com.mazen.helpdesk.repository;

import com.mazen.helpdesk.entity.TicketCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;
import java.util.List;

public interface TicketCategoryRepository extends JpaRepository<TicketCategory, UUID> {
    List<TicketCategory> findAllByOrderByNameAsc();

    boolean existsByNameIgnoreCase(String name);

    // Rename check: is the name taken by a *different* category?
    boolean existsByNameIgnoreCaseAndIdNot(String name, UUID id);
}
