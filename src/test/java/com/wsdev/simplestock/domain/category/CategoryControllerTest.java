package com.wsdev.simplestock.domain.category;

import com.wsdev.simplestock.common.ControllerTestSupport;
import com.wsdev.simplestock.common.exceptions.RecordNotFoundException;
import com.wsdev.simplestock.domain.category.dto.request.CategoryRequestDTO;
import com.wsdev.simplestock.domain.category.dto.response.CategoryResponseDTO;
import com.wsdev.simplestock.domain.user.model.enums.Permission;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest( CategoryController.class )
class CategoryControllerTest extends ControllerTestSupport
{
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CategoryService categoryService;

    private CategoryResponseDTO dto( Long id, String name )
    {
        CategoryResponseDTO dto = new CategoryResponseDTO();
        dto.setId( id );
        dto.setName( name );
        return dto;
    }

    @Test
    void list_withoutPermission_returns403() throws Exception
    {
        mockMvc.perform( get( "/api/v1/categories" ).with( auth( Permission.CATEGORY_CREATE ) ) )
                .andExpect( status().isForbidden() );

        verifyNoInteractions( categoryService );
    }

    @Test
    void delete_withReadOnlyPermission_returns403() throws Exception
    {
        mockMvc.perform( delete( "/api/v1/categories/delete/1" ).with( auth( Permission.CATEGORY_READ ) ) )
                .andExpect( status().isForbidden() );

        verifyNoInteractions( categoryService );
    }

    @Test
    void list_withoutToken_returns403() throws Exception
    {
        mockMvc.perform( get( "/api/v1/categories" ) ).andExpect( status().isForbidden() );
    }

    @Test
    void list_returnsPage() throws Exception
    {
        when( categoryService.getCategories( any() ) ).thenReturn( new PageImpl<>( List.of( dto( 1L, "Food" ) ) ) );

        mockMvc.perform( get( "/api/v1/categories" ).with( auth() ) )
                .andExpect( status().isOk() )
                .andExpect( jsonPath( "$.content[0].id" ).value( 1 ) )
                .andExpect( jsonPath( "$.content[0].name" ).value( "Food" ) );
    }

    @Test
    void getByName_returnsCategory() throws Exception
    {
        when( categoryService.getCategoryByName( "Food" ) ).thenReturn( dto( 1L, "Food" ) );

        mockMvc.perform( get( "/api/v1/categories/Food" ).with( auth() ) )
                .andExpect( status().isOk() )
                .andExpect( jsonPath( "$.name" ).value( "Food" ) );
    }

    @Test
    void add_returns201AndDelegates() throws Exception
    {
        mockMvc.perform( post( "/api/v1/categories/add" )
                        .with( auth() )
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( "{\"categoryName\":\"Food\"}" ) )
                .andExpect( status().isCreated() );

        verify( categoryService ).addCategory( any( CategoryRequestDTO.class ) );
    }

    @Test
    void update_returns204() throws Exception
    {
        mockMvc.perform( put( "/api/v1/categories/update/1" )
                        .with( auth() )
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( "{\"categoryName\":\"Drinks\"}" ) )
                .andExpect( status().isNoContent() );

        verify( categoryService ).updateCategory( eq( 1L ), any( CategoryRequestDTO.class ) );
    }

    @Test
    void update_notFound_returns404WithMessage() throws Exception
    {
        doThrow( new RecordNotFoundException( 9L ) ).when( categoryService ).updateCategory( eq( 9L ), any() );

        mockMvc.perform( put( "/api/v1/categories/update/9" )
                        .with( auth() )
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( "{\"categoryName\":\"Drinks\"}" ) )
                .andExpect( status().isNotFound() )
                .andExpect( content().string( "Record not found with id: 9" ) );
    }

    @Test
    void delete_returns204() throws Exception
    {
        mockMvc.perform( delete( "/api/v1/categories/delete/1" ).with( auth() ) )
                .andExpect( status().isNoContent() );

        verify( categoryService ).deleteCategory( 1L );
    }
}
