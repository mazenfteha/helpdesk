package com.mazen.helpdesk.service;

import com.mazen.helpdesk.dto.CreateTicketRequest;
import com.mazen.helpdesk.dto.TicketResponse;
import com.mazen.helpdesk.dto.UpdateTicketRequest;
import com.mazen.helpdesk.entity.Ticket;
import com.mazen.helpdesk.entity.TicketCategory;
import com.mazen.helpdesk.entity.TicketPriority;
import com.mazen.helpdesk.entity.TicketStatus;
import com.mazen.helpdesk.entity.User;
import com.mazen.helpdesk.exception.InvalidCategoryException;
import com.mazen.helpdesk.exception.TicketNotEditableException;
import com.mazen.helpdesk.exception.TicketNotFoundException;
import com.mazen.helpdesk.exception.UserNotFoundException;
import com.mazen.helpdesk.repository.TicketCategoryRepository;
import com.mazen.helpdesk.repository.TicketRepository;
import com.mazen.helpdesk.repository.UserRepository;
import com.mazen.helpdesk.security.CurrentUser;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.List;

@Service
public class TicketService {

    private final TicketRepository ticketRepository;
    private final TicketCategoryRepository categoryRepository;
    private final UserRepository userRepository;

    public TicketService(TicketRepository ticketRepository,
            TicketCategoryRepository categoryRepository,
            UserRepository userRepository) {
        this.ticketRepository = ticketRepository;
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
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
        Ticket ticket = ticketRepository.findById(ticketId)
                .filter(t -> canView(t, currentUser))
                .orElseThrow(TicketNotFoundException::new);

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
        Ticket ticket = ticketRepository.findById(ticketId)
                .filter(t -> canView(t, currentUser))
                .orElseThrow(TicketNotFoundException::new);

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

    private boolean canView(Ticket ticket, CurrentUser currentUser) {
        return switch (currentUser.role()) {
            case ADMIN -> true;
            case CUSTOMER -> ticket.getCustomer().getId().equals(currentUser.id());
            case AGENT -> ticket.getAssignedTo() == null
                    || ticket.getAssignedTo().getId().equals(currentUser.id());
        };
    }
}
