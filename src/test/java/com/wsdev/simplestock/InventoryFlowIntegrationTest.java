package com.wsdev.simplestock;

import com.jayway.jsonpath.JsonPath;
import com.wsdev.simplestock.domain.category.CategoryRepository;
import com.wsdev.simplestock.domain.movement.MovementRepository;
import com.wsdev.simplestock.domain.product.ProductRepository;
import com.wsdev.simplestock.domain.supplier.SupplierRepository;
import com.wsdev.simplestock.domain.user.UserRepository;
import com.wsdev.simplestock.domain.user.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles( "test" )
class InventoryFlowIntegrationTest
{
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private SupplierRepository supplierRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private MovementRepository movementRepository;

    private String token;
    private Long userId;

    @BeforeEach
    void authenticate() throws Exception
    {
        String body = mockMvc.perform( post( "/api/v1/auth/login" )
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( "{\"email\":\"super@test.local\",\"password\":\"SuperPass123\"}" ) )
                .andExpect( status().isOk() )
                .andReturn().getResponse().getContentAsString();

        token = "Bearer " + JsonPath.read( body, "$.token" );
        userId = userRepository.findByEmail( "super@test.local" ).orElseThrow().getId();
    }

    private Long createCategory( String name ) throws Exception
    {
        mockMvc.perform( post( "/api/v1/categories/add" )
                        .header( "Authorization", token )
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( "{\"categoryName\":\"" + name + "\"}" ) )
                .andExpect( status().isCreated() );

        return categoryRepository.getCategoryByName( name ).getId();
    }

    private Long createSupplier( String name ) throws Exception
    {
        mockMvc.perform( post( "/api/v1/supplier/add" )
                        .header( "Authorization", token )
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( "{\"name\":\"" + name + "\",\"contact\":\"555\",\"email\":\"s@mail.com\",\"address\":\"Street 1\"}" ) )
                .andExpect( status().isCreated() );

        return supplierRepository.findByName( name ).getId();
    }

    private Long createProduct( String name, int quantity, Long categoryId, Long supplierId ) throws Exception
    {
        String json = "{\"name\":\"" + name + "\",\"description\":\"desc\",\"state\":\"ACTIVE\",\"price\":9.99,\"quantity\":" + quantity
                + ",\"minimumQuantity\":1,\"maximumQuantity\":100,\"categoryId\":" + categoryId + ",\"supplierId\":" + supplierId + "}";

        mockMvc.perform( multipart( "/api/v1/product/add" )
                        .file( new MockMultipartFile( "product", "", MediaType.APPLICATION_JSON_VALUE, json.getBytes() ) )
                        .header( "Authorization", token ) )
                .andExpect( status().isCreated() );

        return productRepository.findAll().stream().filter( p -> name.equals( p.getName() ) ).findFirst().orElseThrow().getId();
    }

    private String movementJson( Long productId, String type, int quantity )
    {
        return "{\"productId\":" + productId + ",\"userId\":" + userId + ",\"movementType\":\"" + type + "\",\"quantity\":" + quantity + "}";
    }

    // ---- security ----

    @Test
    void protectedEndpoints_withoutToken_areRejected() throws Exception
    {
        mockMvc.perform( get( "/api/v1/product" ) ).andExpect( status().isForbidden() );
        mockMvc.perform( get( "/api/v1/categories" ) ).andExpect( status().isForbidden() );
        mockMvc.perform( get( "/api/v1/supplier" ) ).andExpect( status().isForbidden() );
        mockMvc.perform( get( "/api/v1/movement" ) ).andExpect( status().isForbidden() );
    }

    @Test
    void protectedEndpoints_withForgedToken_areRejected() throws Exception
    {
        mockMvc.perform( get( "/api/v1/product" ).header( "Authorization", "Bearer not.a.token" ) )
                .andExpect( status().isForbidden() );
    }

    @Test
    void login_wrongPassword_isRejected() throws Exception
    {
        String email = UUID.randomUUID() + "@mail.com";
        mockMvc.perform( post( "/api/v1/auth/register" )
                .contentType( MediaType.APPLICATION_JSON )
                .content( "{\"username\":\"u\",\"email\":\"" + email + "\",\"password\":\"right\"}" ) );

        mockMvc.perform( post( "/api/v1/auth/login" )
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( "{\"email\":\"" + email + "\",\"password\":\"wrong\"}" ) )
                .andExpect( status().is4xxClientError() );
    }

    @Test
    void register_storesEncodedPassword() throws Exception
    {
        String email = UUID.randomUUID() + "@mail.com";
        mockMvc.perform( post( "/api/v1/auth/register" )
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( "{\"username\":\"u\",\"email\":\"" + email + "\",\"password\":\"plain\"}" ) )
                .andExpect( status().isCreated() )
                .andExpect( jsonPath( "$.email" ).value( email ) );

        User saved = (User) userRepository.findUserByEmail( email ).orElseThrow();
        assertNotEquals( "plain", saved.getPassword() );
        assertTrue( saved.getPassword().startsWith( "$2" ) );
    }

    // ---- categories ----

    @Test
    void category_crudFlow() throws Exception
    {
        String name = "cat-" + UUID.randomUUID();
        Long id = createCategory( name );

        mockMvc.perform( get( "/api/v1/categories/" + name ).header( "Authorization", token ) )
                .andExpect( status().isOk() )
                .andExpect( jsonPath( "$.id" ).value( id ) );

        mockMvc.perform( get( "/api/v1/categories" ).header( "Authorization", token ) )
                .andExpect( status().isOk() )
                .andExpect( jsonPath( "$.content" ).isArray() );

        String renamed = name + "-renamed";
        mockMvc.perform( put( "/api/v1/categories/update/" + id )
                        .header( "Authorization", token )
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( "{\"categoryName\":\"" + renamed + "\"}" ) )
                .andExpect( status().isNoContent() );
        assertEquals( renamed, categoryRepository.findById( id ).orElseThrow().getName() );

        mockMvc.perform( delete( "/api/v1/categories/delete/" + id ).header( "Authorization", token ) )
                .andExpect( status().isNoContent() );
        assertTrue( categoryRepository.findById( id ).isEmpty() );
    }

    @Test
    void category_reportsHowManyProductsAreLinked() throws Exception
    {
        String name = "cat-" + UUID.randomUUID();
        Long categoryId = createCategory( name );
        String emptyName = "cat-" + UUID.randomUUID();
        createCategory( emptyName );
        Long supplierId = createSupplier( "sup-" + UUID.randomUUID() );
        createProduct( "p-" + UUID.randomUUID(), 1, categoryId, supplierId );
        createProduct( "p-" + UUID.randomUUID(), 1, categoryId, supplierId );

        mockMvc.perform( get( "/api/v1/categories/" + name ).header( "Authorization", token ) )
                .andExpect( jsonPath( "$.productCount" ).value( 2 ) );
        mockMvc.perform( get( "/api/v1/categories/" + emptyName ).header( "Authorization", token ) )
                .andExpect( jsonPath( "$.productCount" ).value( 0 ) );

        mockMvc.perform( get( "/api/v1/categories?size=500" ).header( "Authorization", token ) )
                .andExpect( status().isOk() )
                .andExpect( jsonPath( "$.content[?(@.id == " + categoryId + ")].productCount" ).value( 2 ) );

        // nested inside a product the count is not repeated
        mockMvc.perform( get( "/api/v1/product?size=500" ).header( "Authorization", token ) )
                .andExpect( jsonPath( "$.content[0].categoryDTO.productCount" ).doesNotExist() );
    }

    @Test
    void category_updateUnknownId_returns404() throws Exception
    {
        mockMvc.perform( put( "/api/v1/categories/update/999999" )
                        .header( "Authorization", token )
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( "{\"categoryName\":\"x\"}" ) )
                .andExpect( status().isNotFound() )
                .andExpect( content().string( "Record not found with id: 999999" ) );
    }

    // ---- suppliers ----

    @Test
    void supplier_crudFlow() throws Exception
    {
        String name = "sup-" + UUID.randomUUID();
        Long id = createSupplier( name );

        mockMvc.perform( put( "/api/v1/supplier/update/" + id )
                        .header( "Authorization", token )
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( "{\"name\":\"" + name + "-v2\",\"contact\":\"777\",\"email\":\"v2@mail.com\",\"address\":\"Other\"}" ) )
                .andExpect( status().isNoContent() );
        assertEquals( "777", supplierRepository.findById( id ).orElseThrow().getContact() );

        mockMvc.perform( get( "/api/v1/supplier" ).header( "Authorization", token ) )
                .andExpect( status().isOk() )
                .andExpect( jsonPath( "$.content" ).isArray() );

        mockMvc.perform( delete( "/api/v1/supplier/delete/" + id ).header( "Authorization", token ) )
                .andExpect( status().isNoContent() );
        assertTrue( supplierRepository.findById( id ).isEmpty() );
    }

    @Test
    void supplier_deleteUnknownId_returns404() throws Exception
    {
        mockMvc.perform( delete( "/api/v1/supplier/delete/999999" ).header( "Authorization", token ) )
                .andExpect( status().isNotFound() );
    }

    // ---- products ----

    @Test
    void product_createListUpdateDelete() throws Exception
    {
        Long categoryId = createCategory( "cat-" + UUID.randomUUID() );
        Long supplierId = createSupplier( "sup-" + UUID.randomUUID() );
        String name = "prod-" + UUID.randomUUID();
        Long productId = createProduct( name, 10, categoryId, supplierId );

        mockMvc.perform( get( "/api/v1/product" ).header( "Authorization", token ) )
                .andExpect( status().isOk() )
                .andExpect( jsonPath( "$.content[?(@.id == " + productId + ")].name" ).value( name ) );

        mockMvc.perform( put( "/api/v1/product/update/" + productId )
                        .header( "Authorization", token )
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( "{\"name\":\"" + name + "-v2\",\"description\":\"d\",\"state\":\"ACTIVE\",\"price\":5.00,\"quantity\":3,"
                                + "\"minimumQuantity\":1,\"maximumQuantity\":10,\"categoryId\":" + categoryId + ",\"supplierId\":" + supplierId + "}" ) )
                .andExpect( status().isNoContent() );
        assertEquals( 3, productRepository.findById( productId ).orElseThrow().getQuantity() );

        mockMvc.perform( delete( "/api/v1/product/delete/" + productId ).header( "Authorization", token ) )
                .andExpect( status().isNoContent() );
        assertTrue( productRepository.findById( productId ).isEmpty() );
    }

    @Test
    void product_createWithUnknownCategory_returns404() throws Exception
    {
        Long supplierId = createSupplier( "sup-" + UUID.randomUUID() );
        String json = "{\"name\":\"x\",\"state\":\"ACTIVE\",\"price\":1.00,\"quantity\":1,\"minimumQuantity\":0,\"maximumQuantity\":5,"
                + "\"categoryId\":999999,\"supplierId\":" + supplierId + "}";

        mockMvc.perform( multipart( "/api/v1/product/add" )
                        .file( new MockMultipartFile( "product", "", MediaType.APPLICATION_JSON_VALUE, json.getBytes() ) )
                        .header( "Authorization", token ) )
                .andExpect( status().isNotFound() );
    }

    @Test
    void product_uploadsImage() throws Exception
    {
        Long categoryId = createCategory( "cat-" + UUID.randomUUID() );
        Long supplierId = createSupplier( "sup-" + UUID.randomUUID() );
        String name = "img-" + UUID.randomUUID();
        String json = "{\"name\":\"" + name + "\",\"state\":\"ACTIVE\",\"price\":1.00,\"quantity\":1,\"minimumQuantity\":0,\"maximumQuantity\":5,"
                + "\"categoryId\":" + categoryId + ",\"supplierId\":" + supplierId + "}";

        mockMvc.perform( multipart( "/api/v1/product/add" )
                        .file( new MockMultipartFile( "product", "", MediaType.APPLICATION_JSON_VALUE, json.getBytes() ) )
                        .file( new MockMultipartFile( "file", "it-image.png", "image/png", new byte[]{ 1, 2, 3 } ) )
                        .header( "Authorization", token ) )
                .andExpect( status().isCreated() );

        String stored = productRepository.findAll().stream()
                .filter( p -> name.equals( p.getName() ) ).findFirst().orElseThrow().getImage();
        assertTrue( stored.endsWith( "_it-image.png" ) );

        // the thumbnail can be fetched back through the API
        byte[] served = mockMvc.perform( get( "/api/v1/product/image/" + stored ).header( "Authorization", token ) )
                .andExpect( status().isOk() )
                .andExpect( content().contentType( "image/png" ) )
                .andReturn().getResponse().getContentAsByteArray();
        assertArrayEquals( new byte[]{ 1, 2, 3 }, served );

        // ...only by authenticated users, and never outside the upload directory
        mockMvc.perform( get( "/api/v1/product/image/" + stored ) ).andExpect( status().isForbidden() );
        mockMvc.perform( get( "/api/v1/product/image/..%2F..%2Fpom.xml" ).header( "Authorization", token ) )
                .andExpect( status().is4xxClientError() );
        mockMvc.perform( get( "/api/v1/product/image/missing.png" ).header( "Authorization", token ) )
                .andExpect( status().isNotFound() );
    }

    @Test
    void product_downloadSheet_returnsExcelFile() throws Exception
    {
        Long categoryId = createCategory( "cat-" + UUID.randomUUID() );
        Long supplierId = createSupplier( "sup-" + UUID.randomUUID() );
        createProduct( "sheet-" + UUID.randomUUID(), 4, categoryId, supplierId );

        byte[] bytes = mockMvc.perform( get( "/api/v1/product/download" ).header( "Authorization", token ) )
                .andExpect( status().isOk() )
                .andExpect( header().string( "Content-Disposition", org.hamcrest.Matchers.containsString( "Relatorio_Produtos.xls" ) ) )
                .andReturn().getResponse().getContentAsByteArray();

        assertTrue( bytes.length > 0 );
    }

    // ---- movements ----

    @Test
    void movement_inboundAndOutbound_updateStockAndAreListed() throws Exception
    {
        Long productId = createProduct( "mov-" + UUID.randomUUID(), 10,
                createCategory( "cat-" + UUID.randomUUID() ), createSupplier( "sup-" + UUID.randomUUID() ) );

        mockMvc.perform( post( "/api/v1/movement/inbound" )
                        .header( "Authorization", token )
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( movementJson( productId, "INBOUND", 5 ) ) )
                .andExpect( status().isOk() );
        assertEquals( 15, productRepository.findById( productId ).orElseThrow().getQuantity() );

        mockMvc.perform( post( "/api/v1/movement/outbound" )
                        .header( "Authorization", token )
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( movementJson( productId, "OUTBOUND", 6 ) ) )
                .andExpect( status().isOk() );
        assertEquals( 9, productRepository.findById( productId ).orElseThrow().getQuantity() );

        mockMvc.perform( post( "/api/v1/movement/add" )
                        .header( "Authorization", token )
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( movementJson( productId, "INBOUND", 1 ) ) )
                .andExpect( status().isCreated() );
        assertEquals( 10, productRepository.findById( productId ).orElseThrow().getQuantity() );

        mockMvc.perform( get( "/api/v1/movement" ).header( "Authorization", token ) )
                .andExpect( status().isOk() )
                .andExpect( jsonPath( "$.content[0].userName" ).value( "superuser" ) );
    }

    @Test
    void movement_outboundBeyondStock_isRejectedAndStockUnchanged() throws Exception
    {
        Long productId = createProduct( "low-" + UUID.randomUUID(), 2,
                createCategory( "cat-" + UUID.randomUUID() ), createSupplier( "sup-" + UUID.randomUUID() ) );
        long movementsBefore = movementRepository.count();

        try
        {
            mockMvc.perform( post( "/api/v1/movement/outbound" )
                    .header( "Authorization", token )
                    .contentType( MediaType.APPLICATION_JSON )
                    .content( movementJson( productId, "OUTBOUND", 3 ) ) );
            fail( "expected the request to fail with insufficient stock" );
        }
        catch ( Exception expected )
        {
            // IllegalArgumentException is not mapped by the ControllerAdvice, so it surfaces from MockMvc
            assertTrue( expected.getMessage().contains( "Insufficient stock" ) );
        }

        assertEquals( 2, productRepository.findById( productId ).orElseThrow().getQuantity() );
        assertEquals( movementsBefore, movementRepository.count() );
    }

    @Test
    void movement_updateAndDelete() throws Exception
    {
        Long productId = createProduct( "upd-" + UUID.randomUUID(), 10,
                createCategory( "cat-" + UUID.randomUUID() ), createSupplier( "sup-" + UUID.randomUUID() ) );
        mockMvc.perform( post( "/api/v1/movement/inbound" )
                .header( "Authorization", token )
                .contentType( MediaType.APPLICATION_JSON )
                .content( movementJson( productId, "INBOUND", 2 ) ) );
        Long movementId = movementRepository.findAll().stream()
                .filter( m -> m.getProduct().getId().equals( productId ) ).findFirst().orElseThrow().getId();

        mockMvc.perform( put( "/api/v1/movement/update/" + movementId )
                        .header( "Authorization", token )
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( movementJson( productId, "ADJUSTMENT", 7 ) ) )
                .andExpect( status().isNoContent() );

        mockMvc.perform( delete( "/api/v1/movement/delete/" + movementId ).header( "Authorization", token ) )
                .andExpect( status().isNoContent() );
        assertTrue( movementRepository.findById( movementId ).isEmpty() );
    }

    @Test
    void movement_unknownProduct_returns404() throws Exception
    {
        mockMvc.perform( post( "/api/v1/movement/add" )
                        .header( "Authorization", token )
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( movementJson( 999999L, "INBOUND", 1 ) ) )
                .andExpect( status().isNotFound() );
    }
}
