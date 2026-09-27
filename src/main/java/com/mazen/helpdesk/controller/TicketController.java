package com.mazen.helpdesk.controller;

import com.mazen.helpdesk.dto.CreateTicketRequest;
import com.mazen.helpdesk.dto.TicketResponse;
import com.mazen.helpdesk.dto.UpdateTicketRequest;
import com.mazen.helpdesk.security.CurrentUser;
import com.mazen.helpdesk.service.TicketService;
import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.PutMapping;

@RestController
@RequestMapping("/api/tickets")
public class TicketController {

    private final TicketService ticketService;

    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TicketResponse create(@Valid @RequestBody CreateTicketRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        return ticketService.createTicket(request, UUID.fromString(jwt.getSubject()));
    }

    @GetMapping("/{id}")
    public TicketResponse getById(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        return ticketService.getTicket(id, CurrentUser.from(jwt));
    }

    @GetMapping
    public List<TicketResponse> list(@AuthenticationPrincipal Jwt jwt) {
        return ticketService.listTickets(CurrentUser.from(jwt));
    }

    @PutMapping ("/{id}")
    public TicketResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateTicketRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        return ticketService.updateTicket(id, request, CurrentUser.from(jwt));
    }

    @DeleteMapping ("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        ticketService.deleteTicket(id);
    }
}
