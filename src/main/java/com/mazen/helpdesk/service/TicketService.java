package com.mazen.helpdesk.service;

 import com.mazen.helpdesk.dto.AssignTicketRequest;
import com.mazen.helpdesk.dto.ChangeStatusRequest;
import com.mazen.helpdesk.dto.CreateTicketRequest;
import com.mazen.helpdesk.dto.TicketResponse;
import com.mazen.helpdesk.dto.UpdateTicketRequest;
import com.mazen.helpdesk.entity.Role;
import com.mazen.helpdesk.entity.Ticket;
import com.mazen.helpdesk.entity.TicketCategory;
import com.mazen.helpdesk.entity.TicketPriority;
import com.mazen.helpdesk.entity.TicketStatus;
import com.mazen.helpdesk.entity.User;
import com.mazen.helpdesk.exception.InvalidAssigneeException;
import com.mazen.helpdesk.exception.InvalidCategoryException;
import com.mazen.helpdesk.exception.InvalidStatusTransitionException;
import com.mazen.helpdesk.exception.TicketActionNotAllowedException;
import com.mazen.helpdesk.exception.TicketAlreadyAssignedException;
import com.mazen.helpdesk.exception.TicketClosedException;
import com.mazen.helpdesk.exception.TicketNotEditableException;
import com.mazen.helpdesk.exception.TicketNotFoundException;
import com.mazen.helpdesk.exception.UserNotFoundException;
import com.mazen.helpdesk.repository.TicketCategoryRepository;
import com.mazen.helpdesk.repository.TicketRepository;
import com.mazen.helpdesk.repository.UserRepository;
import com.mazen.helpdesk.security.CurrentUser;
import com.mazen.helpdesk.security.TicketAccessPolicy;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.List;

@Service
public class TicketService {

    private final TicketRepository ticketRepository;
    private final TicketCategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final TicketAccessPolicy accessPolicy;

    public TicketService(TicketRepository ticketRepository,
            TicketCategoryRepository categoryRepository,
            UserRepository userRepository, TicketAccessPolicy accessPolicy) {
        this.ticketRepository = ticketRepository;
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
        this.accessPolicy = accessPolicy;
    }

    @Transactional
    public TicketResponse createTicket(CreateTicketRequest request, UUID customerId) {
        User customer = userRepository.findById(customerId)
                .orElseThrow(UserNotFoundException::new);

        TicketCategory category = categoryRepository.findById(request.categoryId())
                .orElseThrow(InvalidCategoryException::new);

        Ticket ticket = new Ticket();
        ticket.setTitle(request.title().trim());
        ticket.setDescription(request.description().trim());
        ticket.setCategory(category);
        ticket.setCustomer(customer);
        ticket.setStatus(TicketStatus.OPEN);
        ticket.setPriority(request.priority() != null ? request.priority() : TicketPriority.MEDIUM);
        ticket.setAssignedTo(null);

        return TicketResponse.from(ticketRepository.save(ticket));
    }

    @Transactional(readOnly = true)
    public TicketResponse getTicket(UUID ticketId, CurrentUser currentUser) {
        Ticket ticket = findViewableTicket(ticketId, currentUser);

        return TicketResponse.from(ticket);
    }

    @Transactional(readOnly = true)
    public List<TicketResponse> listTickets(CurrentUser currentUser) {
        // Must stay in sync with canView()
        List<Ticket> tickets = switch (currentUser.role()) {
            case ADMIN -> ticketRepository.findAllByOrderByCreatedAtDesc();
            case CUSTOMER -> ticketRepository.findByCustomerIdOrderByCreatedAtDesc(currentUser.id());
            case AGENT -> ticketRepository.findVisibleToAgent(currentUser.id());
        };

        return tickets.stream()
                .map(TicketResponse::from)
                .toList();
    }

    @Transactional
    public TicketResponse updateTicket(UUID ticketId, UpdateTicketRequest request, CurrentUser currentUser) {
        Ticket ticket = findViewableTicket(ticketId, currentUser);

        if (ticket.getStatus() != TicketStatus.OPEN) {
            throw new TicketNotEditableException();
        }

        TicketCategory category = categoryRepository.findById(request.categoryId())
                .orElseThrow(InvalidCategoryException::new);

        ticket.setTitle(request.title().trim());
        ticket.setDescription(request.description().trim());
        ticket.setCategory(category);
        ticket.setPriority(request.priority());

        // Dirty checking will UPDATE at commit; flush now so @PreUpdate sets updatedAt
        // before we map
        ticketRepository.flush();

        return TicketResponse.from(ticket);
    }

    @Transactional
    public void deleteTicket(UUID ticketId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(TicketNotFoundException::new);

        ticketRepository.delete(ticket);
    }

    @Transactional
    public TicketResponse changeStatus(UUID ticketId, ChangeStatusRequest request, CurrentUser currentUser) {
        Ticket ticket = findViewableTicket(ticketId, currentUser);

        if (!accessPolicy.canChangeStatus(ticket, currentUser)) {
            throw new TicketActionNotAllowedException();
        }

        TicketStatus current = ticket.getStatus();
        TicketStatus target = request.status();

        if (!current.canTransitionTo(target)) {
            throw new InvalidStatusTransitionException(current, target);
        }

        ticket.setStatus(target);

        ticketRepository.flush();

        return TicketResponse.from(ticket);
    }

    @Transactional
    public TicketResponse claimTicket(UUID ticketId, CurrentUser currentUser) {
        // Agents can view unassigned tickets and their own; another agent's ticket is a 404
        Ticket ticket = findViewableTicket(ticketId, currentUser);

        if (ticket.getStatus() == TicketStatus.CLOSED) {
            throw new TicketClosedException();
        }

        // Fast path for the common case; NOT safe on its own (two agents can both pass it)
        if (ticket.getAssignedTo() != null) {
            throw new TicketAlreadyAssignedException();
        }

        User agent = userRepository.findById(currentUser.id())
                .orElseThrow(UserNotFoundException::new);

        // The real guarantee: one atomic UPDATE ... WHERE assigned_to IS NULL
        int updated = ticketRepository.claimIfUnassigned(ticketId, agent, LocalDateTime.now());
        if (updated == 0) {
            throw new TicketAlreadyAssignedException();
        }

        // The bulk update cleared the persistence context, so reload the fresh state
        return TicketResponse.from(ticketRepository.findById(ticketId)
                .orElseThrow(TicketNotFoundException::new));
    }

    @Transactional
    public TicketResponse assignTicket(UUID ticketId, AssignTicketRequest request, CurrentUser currentUser) {
        Ticket ticket = findViewableTicket(ticketId, currentUser);

        // Defense in depth: SecurityConfig already limits this endpoint to ADMIN
        if (!accessPolicy.canAssign(currentUser)) {
            throw new TicketActionNotAllowedException();
        }

        if (ticket.getStatus() == TicketStatus.CLOSED) {
            throw new TicketClosedException();
        }

        // The FK only proves the user exists; the role rule is ours to enforce
        User agent = userRepository.findById(request.agentId())
                .filter(u -> u.getRole() == Role.AGENT)
                .orElseThrow(InvalidAssigneeException::new);

        // Admin assignment is authoritative: it overrides any current assignee (reassign)
        ticket.setAssignedTo(agent);

        ticketRepository.flush();

        return TicketResponse.from(ticket);
    }

    private Ticket findViewableTicket(UUID ticketId, CurrentUser currentUser) {
        return ticketRepository.findById(ticketId)
                .filter(t -> accessPolicy.canView(t, currentUser))
                .orElseThrow(TicketNotFoundException::new);
    }

}
