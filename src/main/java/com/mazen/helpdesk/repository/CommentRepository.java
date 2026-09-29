package com.mazen.helpdesk.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mazen.helpdesk.entity.Comment;

import java.util.List;
import java.util.UUID;

public interface CommentRepository extends JpaRepository<Comment, UUID> {
    
    List<Comment> findByTicketIdOrderByCreatedAtAsc(UUID ticketId);

}
