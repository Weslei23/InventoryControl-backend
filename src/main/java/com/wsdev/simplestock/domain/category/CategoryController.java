package com.wsdev.simplestock.domain.category;

import com.wsdev.simplestock.domain.category.dto.request.CategoryRequestDTO;
import com.wsdev.simplestock.domain.category.dto.response.CategoryResponseDTO;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController()
@CrossOrigin( origins = "http://localhost:5173" )
@RequestMapping( "/api/v1/categories" )
public class CategoryController
{
    @Autowired
    private CategoryService categoryService;

    @GetMapping()
    @ResponseStatus( HttpStatus.OK )
    @PreAuthorize( "hasAuthority('CATEGORY_READ')" )
    public Page<CategoryResponseDTO> getCategories( Pageable pageable )
    {
        return categoryService.getCategories( pageable );
    }

    @GetMapping( "/{name}" )
    @ResponseStatus( HttpStatus.OK )
    @PreAuthorize( "hasAuthority('CATEGORY_READ')" )
    public CategoryResponseDTO getCategoryByName( @PathVariable String name ) throws Exception
    {
        return categoryService.getCategoryByName( name );
    }

    @PostMapping( "/add" )
    @ResponseStatus( HttpStatus.CREATED )
    @PreAuthorize( "hasAuthority('CATEGORY_CREATE')" )
    public void addCategory( @Valid @RequestBody CategoryRequestDTO categoryRequestDTO ) throws Exception
    {
        categoryService.addCategory( categoryRequestDTO );
    }

    @PutMapping( "/update/{id}" )
    @ResponseStatus( HttpStatus.NO_CONTENT )
    @PreAuthorize( "hasAuthority('CATEGORY_UPDATE')" )
    public void updateCategory(@Valid @PathVariable Long id, @RequestBody CategoryRequestDTO categoryRequestDTO ) throws Exception
    {
        categoryService.updateCategory( id, categoryRequestDTO );
    }

    @DeleteMapping( "/delete/{id}" )
    @ResponseStatus( HttpStatus.NO_CONTENT )
    @PreAuthorize( "hasAuthority('CATEGORY_DELETE')" )
    public void deleteCategory( @PathVariable Long id )
    {
        categoryService.deleteCategory( id );
    }
}