package com.wsdev.simplestock.domain.category;

import com.wsdev.simplestock.common.exceptions.RecordNotFoundException;
import com.wsdev.simplestock.domain.category.dto.request.CategoryRequestDTO;
import com.wsdev.simplestock.domain.category.dto.response.CategoryResponseDTO;
import com.wsdev.simplestock.domain.category.model.Category;
import com.wsdev.simplestock.domain.product.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith( MockitoExtension.class )
class CategoryServiceTest
{
    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private CategoryService categoryService;

    private Category category( Long id, String name )
    {
        Category category = new Category();
        category.setId( id );
        category.setName( name );
        return category;
    }

    private CategoryRequestDTO request( String name )
    {
        CategoryRequestDTO dto = new CategoryRequestDTO();
        dto.setCategoryName( name );
        return dto;
    }

    @Test
    void getCategories_mapsPageToDtos()
    {
        PageRequest pageable = PageRequest.of( 0, 10 );
        when( categoryRepository.findAll( pageable ) ).thenReturn( new PageImpl<>( List.of( category( 1L, "Food" ) ) ) );

        Page<CategoryResponseDTO> result = categoryService.getCategories( pageable );

        assertEquals( 1, result.getTotalElements() );
        assertEquals( "Food", result.getContent().get( 0 ).getName() );
    }

    @Test
    void getCategories_includesProductCountWithASingleGroupedQuery()
    {
        PageRequest pageable = PageRequest.of( 0, 10 );
        when( categoryRepository.findAll( pageable ) )
                .thenReturn( new PageImpl<>( List.of( category( 1L, "Food" ), category( 2L, "Drinks" ), category( 3L, "Empty" ) ) ) );
        when( productRepository.countByCategoryIds( List.of( 1L, 2L, 3L ) ) )
                .thenReturn( List.of( count( 1L, 5L ), count( 2L, 1L ) ) );

        List<CategoryResponseDTO> result = categoryService.getCategories( pageable ).getContent();

        assertEquals( 5L, result.get( 0 ).getProductCount() );
        assertEquals( 1L, result.get( 1 ).getProductCount() );
        assertEquals( 0L, result.get( 2 ).getProductCount() );
        verify( productRepository, times( 1 ) ).countByCategoryIds( any() );
        verify( productRepository, never() ).countByCategoryId( any() );
    }

    @Test
    void getCategories_emptyPage_doesNotQueryProducts()
    {
        PageRequest pageable = PageRequest.of( 0, 10 );
        when( categoryRepository.findAll( pageable ) ).thenReturn( new PageImpl<>( List.of() ) );

        assertEquals( 0, categoryService.getCategories( pageable ).getTotalElements() );
        verifyNoInteractions( productRepository );
    }

    private ProductRepository.CategoryProductCount count( Long categoryId, Long total )
    {
        return new ProductRepository.CategoryProductCount()
        {
            public Long getCategoryId() { return categoryId; }

            public Long getTotal() { return total; }
        };
    }

    @Test
    void getCategoryByName_returnsDto() throws Exception
    {
        when( categoryRepository.getCategoryByName( "Food" ) ).thenReturn( category( 1L, "Food" ) );
        when( productRepository.countByCategoryId( 1L ) ).thenReturn( 4L );

        CategoryResponseDTO result = categoryService.getCategoryByName( "Food" );

        assertEquals( 4L, result.getProductCount() );

        assertEquals( 1L, result.getId() );
        assertEquals( "Food", result.getName() );
    }

    @Test
    void addCategory_savesNewCategory() throws Exception
    {
        when( categoryRepository.getCategoryByName( "Food" ) ).thenReturn( null );

        categoryService.addCategory( request( "Food" ) );

        ArgumentCaptor<Category> captor = ArgumentCaptor.forClass( Category.class );
        verify( categoryRepository ).save( captor.capture() );
        assertEquals( "Food", captor.getValue().getName() );
    }

    @Test
    void addCategory_duplicateName_throws()
    {
        when( categoryRepository.getCategoryByName( "Food" ) ).thenReturn( category( 1L, "Food" ) );

        IllegalArgumentException ex = assertThrows( IllegalArgumentException.class, () -> categoryService.addCategory( request( "Food" ) ) );

        assertTrue( ex.getMessage().contains( "Food" ) );
        verify( categoryRepository, never() ).save( any() );
    }

    @Test
    void updateCategory_updatesName() throws Exception
    {
        Category existing = category( 1L, "Old" );
        when( categoryRepository.findById( 1L ) ).thenReturn( Optional.of( existing ) );

        categoryService.updateCategory( 1L, request( "New" ) );

        assertEquals( "New", existing.getName() );
        verify( categoryRepository ).save( existing );
    }

    @Test
    void updateCategory_notFound_throws()
    {
        when( categoryRepository.findById( 99L ) ).thenReturn( Optional.empty() );

        assertThrows( RecordNotFoundException.class, () -> categoryService.updateCategory( 99L, request( "New" ) ) );
        verify( categoryRepository, never() ).save( any() );
    }

    @Test
    void deleteCategory_deletesExisting()
    {
        Category existing = category( 1L, "Food" );
        when( categoryRepository.findById( 1L ) ).thenReturn( Optional.of( existing ) );

        categoryService.deleteCategory( 1L );

        verify( categoryRepository ).delete( existing );
    }

    @Test
    void deleteCategory_notFound_throws()
    {
        when( categoryRepository.findById( 99L ) ).thenReturn( Optional.empty() );

        assertThrows( RecordNotFoundException.class, () -> categoryService.deleteCategory( 99L ) );
        verify( categoryRepository, never() ).delete( any() );
    }
}
