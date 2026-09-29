package com.mazen.helpdesk.repository;

import com.mazen.helpdesk.entity.Ticket;
import com.mazen.helpdesk.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
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

        // Atomic check-and-set: the database only assigns if the ticket is STILL
        // unassigned.
        // Returns the number of updated rows: 1 = claimed, 0 = someone else got there
        // first.
        // Bulk updates skip @PreUpdate, so updatedAt is set explicitly.
        @Modifying(flushAutomatically = true, clearAutomatically = true)
        @Query("""
                        update Ticket t
                        set t.assignedTo = :agent, t.updatedAt = :now
                        where t.id = :ticketId and t.assignedTo is null
                        """)
        int claimIfUnassigned(@Param("ticketId") UUID ticketId,
                        @Param("agent") User agent,
                        @Param("now") LocalDateTime now);
}
