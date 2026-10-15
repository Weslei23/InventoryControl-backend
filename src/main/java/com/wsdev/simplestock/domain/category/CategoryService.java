package com.wsdev.simplestock.domain.category;

import com.wsdev.simplestock.common.exceptions.RecordNotFoundException;
import com.wsdev.simplestock.domain.category.dto.request.CategoryRequestDTO;
import com.wsdev.simplestock.domain.category.dto.response.CategoryResponseDTO;
import com.wsdev.simplestock.domain.category.mapper.CategoryMapper;
import com.wsdev.simplestock.domain.category.model.Category;
import com.wsdev.simplestock.common.utilities.Validator;
import jakarta.validation.Valid;
import com.wsdev.simplestock.domain.product.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class CategoryService
{
    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    /**
     * Get all categories
     * @return
     */
    public Page<CategoryResponseDTO> getCategories( Pageable pageable )
    {
        Page<Category> categories = categoryRepository.findAll( pageable );

        // one grouped query for the whole page instead of one count per category
        List<Long> ids = categories.getContent().stream().map( Category::getId ).toList();
        Map<Long, Long> productCounts = new HashMap<>();

        if ( !ids.isEmpty() )
        {
            productRepository.countByCategoryIds( ids )
                    .forEach( count -> productCounts.put( count.getCategoryId(), count.getTotal() ) );
        }

        return categories.map( category -> CategoryMapper.entityToDto( category, productCounts.getOrDefault( category.getId(), 0L ) ) );
    }

    /**
     * Get category by name
     * @param name
     * @return
     */
    public CategoryResponseDTO getCategoryByName( String name ) throws Exception
    {
        Validator.requiredNonNull( name, new IllegalArgumentException( "Argument 'Name' must not be null." ) );
        Category category = categoryRepository.getCategoryByName( name );

        return CategoryMapper.entityToDto( category, productRepository.countByCategoryId( category.getId() ) );
    }

    /**
     * Add category
     * @param categoryRequestDTO
     */
    public void addCategory( @Valid CategoryRequestDTO categoryRequestDTO ) throws Exception
    {
        if( categoryRepository.getCategoryByName( categoryRequestDTO.getCategoryName() ) != null )
        {
            throw new IllegalArgumentException( "Category already exists with name " + categoryRequestDTO.getCategoryName() );
        }

        categoryRepository.save( CategoryMapper.dtoToEntity( categoryRequestDTO ) );
    }

    /**
     * Update category
     * @param id
     * @param categoryRequestDTO
     */
    public void updateCategory( Long id, @Valid CategoryRequestDTO categoryRequestDTO ) throws Exception
    {
        Category category = categoryRepository.findById( id ).orElseThrow( () -> new RecordNotFoundException( id ) );

        category.setName( categoryRequestDTO.getCategoryName() );

        categoryRepository.save( category );
    }

    /**
     * Delete category
     * @param id
     */
    public void deleteCategory( Long id )
    {
        Category category = categoryRepository.findById( id ).orElseThrow( () -> new RecordNotFoundException( id ) );
        
        categoryRepository.delete( category );
    }
}