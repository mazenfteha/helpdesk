package com.mazen.helpdesk.service;


import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mazen.helpdesk.entity.TicketStatus;
import com.mazen.helpdesk.entity.User;
import com.mazen.helpdesk.dto.CommentResponse;
import com.mazen.helpdesk.dto.CreateCommentRequest;
import com.mazen.helpdesk.entity.Comment;
import com.mazen.helpdesk.entity.Ticket;
import com.mazen.helpdesk.exception.CommentNotAllowedException;
import com.mazen.helpdesk.exception.TicketClosedException;
import com.mazen.helpdesk.exception.TicketNotFoundException;
import com.mazen.helpdesk.repository.CommentRepository;
import com.mazen.helpdesk.repository.TicketRepository;
import com.mazen.helpdesk.repository.UserRepository;
import com.mazen.helpdesk.security.CurrentUser;
import com.mazen.helpdesk.security.TicketAccessPolicy;

import java.util.List;
import java.util.UUID;

@Service
public class CommentService {
    private final CommentRepository commentRepository;
    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;
    private final TicketAccessPolicy accessPolicy;

    public CommentService(CommentRepository commentRepository,
            TicketRepository ticketRepository,
            UserRepository userRepository,
            TicketAccessPolicy accessPolicy) {
        this.commentRepository = commentRepository;
        this.ticketRepository = ticketRepository;
        this.userRepository = userRepository;
        this.accessPolicy = accessPolicy;
    }

    @Transactional 
    public CommentResponse addComment(UUID ticketId, CreateCommentRequest request, CurrentUser currentUser) {
        Ticket ticket = findViewableTicket(ticketId, currentUser);

        if (!accessPolicy.canComment(ticket, currentUser)) {
            throw new CommentNotAllowedException();
        }

        if (ticket.getStatus() == TicketStatus.CLOSED) {
            throw new TicketClosedException();
        }

        User author = userRepository.findById(currentUser.id())
                .orElseThrow(IllegalStateException::new);

        Comment comment = new Comment();
        comment.setContent(request.content().trim());
        comment.setTicket(ticket);
        comment.setUser(author);

        return CommentResponse.from(commentRepository.save(comment));

    }

    @Transactional(readOnly = true)
        public List<CommentResponse> listComments(UUID ticketId, CurrentUser currentUser) {
        Ticket ticket = findViewableTicket(ticketId, currentUser);

        return commentRepository.findByTicketIdOrderByCreatedAtAsc(ticket.getId())
                .stream()
                .map(CommentResponse::from)
                .toList();
    }



    private Ticket findViewableTicket(UUID ticketId, CurrentUser currentUser) {
        return ticketRepository.findById(ticketId)
                .filter(t -> accessPolicy.canView(t, currentUser))
                .orElseThrow(TicketNotFoundException::new);
    }

}
