package com.mazen.helpdesk.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mazen.helpdesk.dto.TicketCategoryResponse;
import com.mazen.helpdesk.repository.TicketCategoryRepository;
import java.util.List;


@Service 
public class CategoryService {
    private final TicketCategoryRepository categoryRepository;

    public CategoryService(TicketCategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Transactional(readOnly = true)
    public List<TicketCategoryResponse> listCategories() {
        return categoryRepository.findAllByOrderByNameAsc()
                .stream()
                .map(TicketCategoryResponse::from)
                .toList();
    } 
}
