package com.mazen.helpdesk.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mazen.helpdesk.dto.CreateCategoryRequest;
import com.mazen.helpdesk.dto.TicketCategoryResponse;
import com.mazen.helpdesk.dto.UpdateCategoryRequest;
import com.mazen.helpdesk.entity.TicketCategory;
import com.mazen.helpdesk.exception.CategoryAlreadyExistsException;
import com.mazen.helpdesk.exception.CategoryInUseException;
import com.mazen.helpdesk.exception.CategoryNotFoundException;
import com.mazen.helpdesk.repository.TicketCategoryRepository;
import com.mazen.helpdesk.repository.TicketRepository;
import java.util.List;
import java.util.UUID;


@Service
public class CategoryService {
    private final TicketCategoryRepository categoryRepository;
    private final TicketRepository ticketRepository;

    public CategoryService(TicketCategoryRepository categoryRepository, TicketRepository ticketRepository) {
        this.categoryRepository = categoryRepository;
        this.ticketRepository = ticketRepository;
    }

    @Transactional(readOnly = true)
    public List<TicketCategoryResponse> listCategories() {
        return categoryRepository.findAllByOrderByNameAsc()
                .stream()
                .map(TicketCategoryResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public TicketCategoryResponse getCategory(UUID categoryId) {
        return TicketCategoryResponse.from(findCategory(categoryId));
    }

    @Transactional
    public TicketCategoryResponse createCategory(CreateCategoryRequest request) {
        String name = normalizeName(request.name());

        if (categoryRepository.existsByNameIgnoreCase(name)) {
            throw new CategoryAlreadyExistsException();
        }

        TicketCategory category = new TicketCategory();
        category.setName(name);
        category = categoryRepository.save(category);
        return TicketCategoryResponse.from(category);
    }

    @Transactional
    public TicketCategoryResponse updateCategory(UUID categoryId, UpdateCategoryRequest request) {
        TicketCategory category = findCategory(categoryId);
        String name = normalizeName(request.name());

        // Excludes this category, so changing only the letter case ("billing" -> "Billing") is allowed
        if (categoryRepository.existsByNameIgnoreCaseAndIdNot(name, categoryId)) {
            throw new CategoryAlreadyExistsException();
        }

        category.setName(name);

        // Dirty checking will UPDATE at commit; flush now so @PreUpdate sets updatedAt before we map
        categoryRepository.flush();

        return TicketCategoryResponse.from(category);
    }

    @Transactional
    public void deleteCategory(UUID categoryId) {
        TicketCategory category = findCategory(categoryId);

        if (ticketRepository.existsByCategoryId(categoryId)) {
            throw new CategoryInUseException();
        }

        categoryRepository.delete(category);
    }

    private TicketCategory findCategory(UUID categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(CategoryNotFoundException::new);
    }

    private String normalizeName(String name) {
        return name.trim();
    }
}
