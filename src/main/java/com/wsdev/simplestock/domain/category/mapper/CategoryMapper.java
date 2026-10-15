package com.wsdev.simplestock.domain.category.mapper;

import com.wsdev.simplestock.domain.category.dto.request.CategoryRequestDTO;
import com.wsdev.simplestock.domain.category.dto.response.CategoryResponseDTO;
import com.wsdev.simplestock.domain.category.model.Category;

public class CategoryMapper
{
    /**
     * Entity to dto
     *
     * @param category
     * @return
     */
    public static CategoryResponseDTO entityToDto( Category category )
    {
        CategoryResponseDTO categoryDTO = new CategoryResponseDTO();

        categoryDTO.setId( category.getId() );
        categoryDTO.setName( category.getName() );

        return categoryDTO;
    }

    /**
     * Entity to dto, including how many products are linked to the category
     *
     * @param category
     * @param productCount
     * @return
     */
    public static CategoryResponseDTO entityToDto( Category category, long productCount )
    {
        CategoryResponseDTO categoryDTO = entityToDto( category );

        categoryDTO.setProductCount( productCount );

        return categoryDTO;
    }

    /**
     * Dto to entity
     *
     * @param categoryRequestDTO
     * @return
     */
    public static Category dtoToEntity( CategoryRequestDTO categoryRequestDTO )
    {
        Category category = new Category();

        category.setName( categoryRequestDTO.getCategoryName() );

        return category;
    }
}