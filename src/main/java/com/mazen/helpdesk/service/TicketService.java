package com.mazen.helpdesk.service;

import com.mazen.helpdesk.dto.CreateTicketRequest;
import com.mazen.helpdesk.dto.TicketResponse;
import com.mazen.helpdesk.entity.Ticket;
import com.mazen.helpdesk.entity.TicketCategory;
import com.mazen.helpdesk.entity.TicketPriority;
import com.mazen.helpdesk.entity.TicketStatus;
import com.mazen.helpdesk.entity.User;
import com.mazen.helpdesk.exception.InvalidCategoryException;
import com.mazen.helpdesk.exception.UserNotFoundException;
import com.mazen.helpdesk.repository.TicketCategoryRepository;
import com.mazen.helpdesk.repository.TicketRepository;
import com.mazen.helpdesk.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

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
}
