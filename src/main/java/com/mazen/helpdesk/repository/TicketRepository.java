package com.mazen.helpdesk.repository;

import com.mazen.helpdesk.entity.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;


public interface TicketRepository extends JpaRepository<Ticket, UUID> {
    List<Ticket> findAllByOrderByCreatedAtDesc();

    List<Ticket> findByCustomerIdOrderByCreatedAtDesc(UUID customerId); 

    @Query("""
            select t from Ticket t
            where t.assignedTo is null or t.assignedTo.id = :agentId
            order by t.createdAt desc
            """)
    List<Ticket> findVisibleToAgent(@Param("agentId") UUID agentId);

    boolean existsByCategoryId(UUID categoryId);
}