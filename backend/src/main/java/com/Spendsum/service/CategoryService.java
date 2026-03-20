package com.Spendsum.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.Spendsum.model.Category;
import com.Spendsum.repository.CategoryRepository;

@Service
public class CategoryService {

    @Autowired
    private CategoryRepository categoryRepository;

    // ✅ Create Category (with duplicate check)
    public Category createCategory(Category category) {

        categoryRepository.findByNameAndUserId(
                category.getName(),
                category.getUser().getId()
        ).ifPresent(existing -> {
            throw new RuntimeException("Category already exists for this user");
        });

        return categoryRepository.save(category);
    }

    // ✅ Get all categories
    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }

    // ✅ Get categories by user
    public List<Category> getCategoriesByUser(Long userId) {
        return categoryRepository.findByUserId(userId);
    }

    // ✅ Get by ID
    public Category getCategoryById(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Category not found"));
    }

    // ✅ Update
    public Category updateCategory(Long id, Category updated) {
        Category existing = getCategoryById(id);

        existing.setName(updated.getName());
        existing.setType(updated.getType());

        return categoryRepository.save(existing);
    }

    // ✅ Delete
    public void deleteCategory(Long id) {
        categoryRepository.deleteById(id);
    }
}