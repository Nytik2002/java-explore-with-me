package ru.practicum.ewm.category.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.category.CategoryMapper;
import ru.practicum.ewm.category.dto.CategoryDto;
import ru.practicum.ewm.category.dto.NewCategoryDto;
import ru.practicum.ewm.category.model.Category;
import ru.practicum.ewm.category.repository.CategoryRepository;
import ru.practicum.ewm.exception.NotFoundException;
import ru.practicum.ewm.util.OffsetPageRequest;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;

    @Transactional
    public CategoryDto createCategory(NewCategoryDto dto) {
        Category category = CategoryMapper.toCategory(dto);
        Category savedCategory = categoryRepository.save(category);

        return CategoryMapper.toCategoryDto(savedCategory);
    }

    @Transactional
    public CategoryDto updateCategory(long categoryId, CategoryDto dto) {
        Category category = getCategoryEntity(categoryId);

        category.setName(dto.getName());

        Category savedCategory = categoryRepository.save(category);

        return CategoryMapper.toCategoryDto(savedCategory);
    }

    @Transactional
    public void deleteCategory(long categoryId) {
        Category category = getCategoryEntity(categoryId);

        categoryRepository.delete(category);
    }

    @Transactional(readOnly = true)
    public List<CategoryDto> getCategories(int from, int size) {
        OffsetPageRequest pageable = new OffsetPageRequest(
                from,
                size,
                Sort.by("id").ascending()
        );

        return categoryRepository.findAllBy(pageable)
                .stream()
                .map(CategoryMapper::toCategoryDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public CategoryDto getCategory(long categoryId) {
        return CategoryMapper.toCategoryDto(
                getCategoryEntity(categoryId)
        );
    }

    @Transactional(readOnly = true)
    public Category getCategoryEntity(long categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new NotFoundException(
                        "Category with id=" + categoryId + " was not found"
                ));
    }
}