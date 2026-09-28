package com.mazen.helpdesk.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mazen.helpdesk.dto.TicketCategoryResponse;
import com.mazen.helpdesk.service.CategoryService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;


@RestController 
@RequestMapping("/api/categories")
public class CategoryController {

    private final CategoryService categoryService;
    
    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    public List<TicketCategoryResponse> listCategories() {
        return categoryService.listCategories();
    }

}
