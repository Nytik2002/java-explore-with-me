package ru.practicum.ewm.category;

import ru.practicum.ewm.category.dto.CategoryDto;
import ru.practicum.ewm.category.dto.NewCategoryDto;
import ru.practicum.ewm.category.model.Category;

public final class CategoryMapper {

    private CategoryMapper() {
    }

    public static Category toCategory(NewCategoryDto dto) {
        return Category.builder().name(dto.getName()).build();
    }

    public static CategoryDto toCategoryDto(Category category) {
        return CategoryDto.builder().id(category.getId()).name(category.getName()).build();
    }
}