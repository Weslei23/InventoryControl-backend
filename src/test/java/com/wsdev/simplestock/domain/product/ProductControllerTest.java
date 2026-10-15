package com.wsdev.simplestock.domain.product;

import com.wsdev.simplestock.common.ControllerTestSupport;
import com.wsdev.simplestock.common.exceptions.RecordNotFoundException;
import com.wsdev.simplestock.domain.product.dto.request.ProductRequestDTO;
import com.wsdev.simplestock.domain.product.dto.response.ProductResponseDTO;
import com.wsdev.simplestock.domain.user.model.enums.Permission;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest( ProductController.class )
class ProductControllerTest extends ControllerTestSupport
{
    private static final String BODY = """
            {"name":"Rice","description":"5kg","state":"ACTIVE","price":19.90,"quantity":10,
             "minimumQuantity":2,"maximumQuantity":50,"categoryId":1,"supplierId":2}""";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductService productService;

    private ProductResponseDTO dto()
    {
        ProductResponseDTO dto = new ProductResponseDTO();
        dto.setId( 1L );
        dto.setName( "Rice" );
        dto.setPrice( new BigDecimal( "19.90" ) );
        return dto;
    }

    @Test
    void download_withoutExportPermission_returns403() throws Exception
    {
        mockMvc.perform( get( "/api/v1/product/download" ).with( auth( Permission.PRODUCT_READ ) ) )
                .andExpect( status().isForbidden() );

        verifyNoInteractions( productService );
    }

    @Test
    void list_withoutPermission_returns403() throws Exception
    {
        mockMvc.perform( get( "/api/v1/product" ).with( auth( Permission.CATEGORY_READ ) ) )
                .andExpect( status().isForbidden() );

        verifyNoInteractions( productService );
    }

    @Test
    void list_withoutToken_returns403() throws Exception
    {
        mockMvc.perform( get( "/api/v1/product" ) ).andExpect( status().isForbidden() );
    }

    @Test
    void list_withInvalidToken_returns403() throws Exception
    {
        mockMvc.perform( get( "/api/v1/product" ).header( "Authorization", "Bearer garbage" ) )
                .andExpect( status().isForbidden() );
    }

    @Test
    void list_returnsPage() throws Exception
    {
        when( productService.getProducts( any() ) ).thenReturn( new PageImpl<>( List.of( dto() ) ) );

        mockMvc.perform( get( "/api/v1/product" ).with( auth() ) )
                .andExpect( status().isOk() )
                .andExpect( jsonPath( "$.content[0].name" ).value( "Rice" ) );
    }

    @Test
    void add_multipart_returns201AndPassesFile() throws Exception
    {
        MockMultipartFile product = new MockMultipartFile( "product", "", MediaType.APPLICATION_JSON_VALUE, BODY.getBytes() );
        MockMultipartFile file = new MockMultipartFile( "file", "rice.png", "image/png", new byte[]{ 1, 2 } );

        mockMvc.perform( multipart( "/api/v1/product/add" ).file( product ).file( file ).with( auth() ) )
                .andExpect( status().isCreated() );

        ArgumentCaptor<ProductRequestDTO> dtoCaptor = ArgumentCaptor.forClass( ProductRequestDTO.class );
        verify( productService ).addProduct( dtoCaptor.capture(), any() );
        assertEquals( "Rice", dtoCaptor.getValue().getName() );
        assertEquals( 2L, dtoCaptor.getValue().getSupplierId() );
    }

    @Test
    void add_multipartWithoutFile_returns201() throws Exception
    {
        MockMultipartFile product = new MockMultipartFile( "product", "", MediaType.APPLICATION_JSON_VALUE, BODY.getBytes() );

        mockMvc.perform( multipart( "/api/v1/product/add" ).file( product ).with( auth() ) )
                .andExpect( status().isCreated() );

        verify( productService ).addProduct( any( ProductRequestDTO.class ), eq( null ) );
    }

    @Test
    void add_jsonBody_returns415() throws Exception
    {
        mockMvc.perform( post( "/api/v1/product/add" )
                        .with( auth() )
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( BODY ) )
                .andExpect( status().isUnsupportedMediaType() );
    }

    @Test
    void update_returns204() throws Exception
    {
        mockMvc.perform( put( "/api/v1/product/update/1" )
                        .with( auth() )
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( BODY ) )
                .andExpect( status().isNoContent() );

        verify( productService ).updateProduct( eq( 1L ), any( ProductRequestDTO.class ) );
    }

    @Test
    void delete_returns204() throws Exception
    {
        mockMvc.perform( delete( "/api/v1/product/delete/1" ).with( auth() ) )
                .andExpect( status().isNoContent() );

        verify( productService ).deleteProduct( 1L );
    }

    @Test
    void delete_notFound_returns404() throws Exception
    {
        doThrow( new RecordNotFoundException( 9L ) ).when( productService ).deleteProduct( 9L );

        mockMvc.perform( delete( "/api/v1/product/delete/9" ).with( auth() ) )
                .andExpect( status().isNotFound() );
    }

    @Test
    void download_returnsExcelAttachment() throws Exception
    {
        when( productService.downloadSheet() ).thenReturn( new byte[]{ 1, 2, 3 } );

        mockMvc.perform( get( "/api/v1/product/download" ).with( auth() ) )
                .andExpect( status().isOk() )
                .andExpect( content().contentType( "application/vnd.ms-excel" ) )
                .andExpect( header().string( "Content-Disposition", org.hamcrest.Matchers.containsString( "Relatorio_Produtos.xls" ) ) )
                .andExpect( content().bytes( new byte[]{ 1, 2, 3 } ) );
    }

    @Test
    void image_returnsFileWithItsContentType() throws Exception
    {
        when( productService.getProductImage( "rice.png" ) ).thenReturn( new ByteArrayResource( new byte[]{ 1, 2, 3 } ) );
        when( productService.getImageContentType( any() ) ).thenReturn( "image/png" );

        mockMvc.perform( get( "/api/v1/product/image/rice.png" ).with( auth( Permission.PRODUCT_READ ) ) )
                .andExpect( status().isOk() )
                .andExpect( content().contentType( "image/png" ) )
                .andExpect( content().bytes( new byte[]{ 1, 2, 3 } ) );
    }

    @Test
    void image_withoutPermission_returns403() throws Exception
    {
        mockMvc.perform( get( "/api/v1/product/image/rice.png" ).with( auth( Permission.CATEGORY_READ ) ) )
                .andExpect( status().isForbidden() );
        mockMvc.perform( get( "/api/v1/product/image/rice.png" ) ).andExpect( status().isForbidden() );

        verifyNoInteractions( productService );
    }

    @Test
    void image_notFound_returns404() throws Exception
    {
        when( productService.getProductImage( "nope.png" ) ).thenThrow( new RecordNotFoundException( "Image not found: nope.png" ) );

        mockMvc.perform( get( "/api/v1/product/image/nope.png" ).with( auth( Permission.PRODUCT_READ ) ) )
                .andExpect( status().isNotFound() )
                .andExpect( content().string( "Image not found: nope.png" ) );
    }
}
