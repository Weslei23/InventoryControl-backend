package com.wsdev.simplestock.domain.supplier;

import com.wsdev.simplestock.common.ControllerTestSupport;
import com.wsdev.simplestock.common.exceptions.RecordNotFoundException;
import com.wsdev.simplestock.domain.supplier.dto.request.SupplierRequestDTO;
import com.wsdev.simplestock.domain.supplier.dto.response.SupplierResponseDTO;
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

@WebMvcTest( SupplierController.class )
class SupplierControllerTest extends ControllerTestSupport
{
    private static final String BODY = "{\"name\":\"Acme\",\"contact\":\"555\",\"email\":\"a@b.com\",\"address\":\"Street 1\"}";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SupplierService supplierService;

    private SupplierResponseDTO dto()
    {
        SupplierResponseDTO dto = new SupplierResponseDTO();
        dto.setId( 1L );
        dto.setName( "Acme" );
        dto.setEmail( "a@b.com" );
        return dto;
    }

    @Test
    void add_withoutPermission_returns403() throws Exception
    {
        mockMvc.perform( post( "/api/v1/supplier/add" )
                        .with( auth( Permission.SUPPLIER_READ ) )
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( BODY ) )
                .andExpect( status().isForbidden() );

        verifyNoInteractions( supplierService );
    }

    @Test
    void list_withoutToken_returns403() throws Exception
    {
        mockMvc.perform( get( "/api/v1/supplier" ) ).andExpect( status().isForbidden() );
    }

    @Test
    void list_returnsPage() throws Exception
    {
        when( supplierService.getSuppliers( any() ) ).thenReturn( new PageImpl<>( List.of( dto() ) ) );

        mockMvc.perform( get( "/api/v1/supplier" ).with( auth() ) )
                .andExpect( status().isOk() )
                .andExpect( jsonPath( "$.content[0].name" ).value( "Acme" ) );
    }

    @Test
    void add_returns201AndDelegates() throws Exception
    {
        mockMvc.perform( post( "/api/v1/supplier/add" )
                        .with( auth() )
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( BODY ) )
                .andExpect( status().isCreated() );

        verify( supplierService ).addSupplier( any( SupplierRequestDTO.class ) );
    }

    @Test
    void update_returns204() throws Exception
    {
        mockMvc.perform( put( "/api/v1/supplier/update/1" )
                        .with( auth() )
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( BODY ) )
                .andExpect( status().isNoContent() );

        verify( supplierService ).updateSupplier( eq( 1L ), any( SupplierRequestDTO.class ) );
    }

    @Test
    void delete_returns204() throws Exception
    {
        mockMvc.perform( delete( "/api/v1/supplier/delete/1" ).with( auth() ) )
                .andExpect( status().isNoContent() );

        verify( supplierService ).deleteSupplier( 1L );
    }

    @Test
    void delete_notFound_returns404() throws Exception
    {
        doThrow( new RecordNotFoundException( 9L ) ).when( supplierService ).deleteSupplier( 9L );

        mockMvc.perform( delete( "/api/v1/supplier/delete/9" ).with( auth() ) )
                .andExpect( status().isNotFound() );
    }
}
