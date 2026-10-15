package com.wsdev.simplestock.domain.product;

import com.wsdev.simplestock.common.exceptions.RecordNotFoundException;
import com.wsdev.simplestock.domain.category.CategoryRepository;
import com.wsdev.simplestock.domain.category.model.Category;
import com.wsdev.simplestock.domain.product.dto.request.ProductRequestDTO;
import com.wsdev.simplestock.domain.product.dto.response.ProductResponseDTO;
import com.wsdev.simplestock.domain.product.model.Product;
import com.wsdev.simplestock.domain.product.model.enums.ProductState;
import com.wsdev.simplestock.domain.supplier.SupplierRepository;
import com.wsdev.simplestock.domain.supplier.model.Supplier;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith( MockitoExtension.class )
class ProductServiceTest
{
    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private SupplierRepository supplierRepository;

    @InjectMocks
    private ProductService productService;

    @TempDir
    Path uploadDir;

    private Category category;
    private Supplier supplier;

    @BeforeEach
    void setUp()
    {
        ReflectionTestUtils.setField( productService, "fileDirectory", uploadDir.toString() );

        category = new Category();
        category.setId( 1L );
        category.setName( "Food" );

        supplier = new Supplier();
        supplier.setId( 2L );
        supplier.setName( "Acme" );
    }

    private Product product( Long id )
    {
        Product product = new Product();
        product.setId( id );
        product.setName( "Rice" );
        product.setDescription( "5kg" );
        product.setPrice( new BigDecimal( "19.90" ) );
        product.setQuantity( 10 );
        product.setMinimumQuantity( 2 );
        product.setMaximumQuantity( 50 );
        product.setState( ProductState.ACTIVE );
        product.setCategory( category );
        product.setSupplier( supplier );
        return product;
    }

    private ProductRequestDTO request()
    {
        ProductRequestDTO dto = new ProductRequestDTO();
        dto.setName( "Beans" );
        dto.setDescription( "1kg" );
        dto.setState( ProductState.ACTIVE );
        dto.setPrice( new BigDecimal( "7.50" ) );
        dto.setQuantity( 5 );
        dto.setMinimumQuantity( 1 );
        dto.setMaximumQuantity( 20 );
        dto.setCategoryId( 1L );
        dto.setSupplierId( 2L );
        return dto;
    }

    @Test
    void getProducts_mapsPage()
    {
        PageRequest pageable = PageRequest.of( 0, 10 );
        when( productRepository.findAll( pageable ) ).thenReturn( new PageImpl<>( List.of( product( 1L ) ) ) );

        Page<ProductResponseDTO> result = productService.getProducts( pageable );

        ProductResponseDTO dto = result.getContent().get( 0 );
        assertEquals( "Rice", dto.getName() );
        assertEquals( "Food", dto.getCategoryDTO().getName() );
        assertEquals( "Acme", dto.getSupplierResponseDTO().getName() );
    }

    @Test
    void getProductById_returnsDto()
    {
        when( productRepository.findById( 1L ) ).thenReturn( Optional.of( product( 1L ) ) );

        ProductResponseDTO result = productService.getProductById( 1L );

        assertEquals( 1L, result.getId() );
        assertEquals( new BigDecimal( "19.90" ), result.getPrice() );
    }

    @Test
    void getProductById_notFound_throws()
    {
        when( productRepository.findById( 9L ) ).thenReturn( Optional.empty() );

        assertThrows( RecordNotFoundException.class, () -> productService.getProductById( 9L ) );
    }

    @Test
    void getProductsByCategoryName_usesCategoryLookup()
    {
        PageRequest pageable = PageRequest.of( 0, 10 );
        when( categoryRepository.getCategoryByName( "Food" ) ).thenReturn( category );
        when( productRepository.getProductsByCategory( category, pageable ) ).thenReturn( new PageImpl<>( List.of( product( 1L ) ) ) );

        Page<ProductResponseDTO> result = productService.getProductsByCategoryName( "Food", pageable );

        assertEquals( 1, result.getTotalElements() );
    }

    @Test
    void addProduct_withoutFile_savesWithCategoryAndSupplier() throws Exception
    {
        when( categoryRepository.findById( 1L ) ).thenReturn( Optional.of( category ) );
        when( supplierRepository.findById( 2L ) ).thenReturn( Optional.of( supplier ) );

        productService.addProduct( request(), null );

        ArgumentCaptor<Product> captor = ArgumentCaptor.forClass( Product.class );
        verify( productRepository ).save( captor.capture() );
        Product saved = captor.getValue();
        assertEquals( "Beans", saved.getName() );
        assertSame( category, saved.getCategory() );
        assertSame( supplier, saved.getSupplier() );
        assertNull( saved.getImage() );
    }

    @Test
    void addProduct_withFile_storesFileWithUniqueNameAndSetsImage() throws Exception
    {
        when( categoryRepository.findById( 1L ) ).thenReturn( Optional.of( category ) );
        when( supplierRepository.findById( 2L ) ).thenReturn( Optional.of( supplier ) );
        MockMultipartFile file = new MockMultipartFile( "file", "beans.png", "image/png", new byte[]{ 1, 2, 3 } );

        productService.addProduct( request(), file );

        ArgumentCaptor<Product> captor = ArgumentCaptor.forClass( Product.class );
        verify( productRepository ).save( captor.capture() );
        String stored = captor.getValue().getImage();
        assertTrue( stored.endsWith( "_beans.png" ) );
        assertArrayEquals( new byte[]{ 1, 2, 3 }, Files.readAllBytes( uploadDir.resolve( stored ) ) );
    }

    @Test
    void addProduct_sameFileNameTwice_doesNotOverwrite() throws Exception
    {
        when( categoryRepository.findById( 1L ) ).thenReturn( Optional.of( category ) );
        when( supplierRepository.findById( 2L ) ).thenReturn( Optional.of( supplier ) );

        productService.addProduct( request(), new MockMultipartFile( "file", "a.png", "image/png", new byte[]{ 1 } ) );
        productService.addProduct( request(), new MockMultipartFile( "file", "a.png", "image/png", new byte[]{ 2 } ) );

        try ( var files = Files.list( uploadDir ) )
        {
            assertEquals( 2, files.count() );
        }
    }

    @Test
    void addProduct_fileNameWithPathSegments_staysInsideUploadDirectory() throws Exception
    {
        when( categoryRepository.findById( 1L ) ).thenReturn( Optional.of( category ) );
        when( supplierRepository.findById( 2L ) ).thenReturn( Optional.of( supplier ) );

        productService.addProduct( request(), new MockMultipartFile( "file", "../../evil.png", "image/png", new byte[]{ 1 } ) );

        ArgumentCaptor<Product> captor = ArgumentCaptor.forClass( Product.class );
        verify( productRepository ).save( captor.capture() );
        assertFalse( captor.getValue().getImage().contains( ".." ) );
        assertTrue( Files.exists( uploadDir.resolve( captor.getValue().getImage() ) ) );
    }

    // ---- product image ----

    @Test
    void getProductImage_returnsStoredImage() throws Exception
    {
        Files.write( uploadDir.resolve( "rice.png" ), new byte[]{ 1, 2, 3 } );

        Resource image = productService.getProductImage( "rice.png" );

        assertArrayEquals( new byte[]{ 1, 2, 3 }, image.getContentAsByteArray() );
        assertEquals( "image/png", productService.getImageContentType( image ) );
    }

    @Test
    void getProductImage_missingFile_throwsNotFound()
    {
        assertThrows( RecordNotFoundException.class, () -> productService.getProductImage( "nope.png" ) );
    }

    @Test
    void getProductImage_pathTraversal_throwsNotFound() throws Exception
    {
        Path outside = uploadDir.getParent().resolve( "secret.png" );
        Files.write( outside, new byte[]{ 9 } );

        try
        {
            assertThrows( RecordNotFoundException.class, () -> productService.getProductImage( "../secret.png" ) );
        }
        finally
        {
            Files.deleteIfExists( outside );
        }
    }

    @Test
    void getProductImage_nonImageFile_throwsNotFound() throws Exception
    {
        Files.writeString( uploadDir.resolve( "page.html" ), "<script>alert(1)</script>" );

        assertThrows( RecordNotFoundException.class, () -> productService.getProductImage( "page.html" ) );
    }

    @Test
    void addProduct_categoryNotFound_throws()
    {
        when( categoryRepository.findById( 1L ) ).thenReturn( Optional.empty() );

        assertThrows( RecordNotFoundException.class, () -> productService.addProduct( request(), null ) );
        verify( productRepository, never() ).save( any() );
    }

    @Test
    void addProduct_supplierNotFound_throws()
    {
        when( categoryRepository.findById( 1L ) ).thenReturn( Optional.of( category ) );
        when( supplierRepository.findById( 2L ) ).thenReturn( Optional.empty() );

        assertThrows( RecordNotFoundException.class, () -> productService.addProduct( request(), null ) );
        verify( productRepository, never() ).save( any() );
    }

    @Test
    void updateProduct_updatesAllFields()
    {
        Product existing = product( 1L );
        when( productRepository.findById( 1L ) ).thenReturn( Optional.of( existing ) );
        when( categoryRepository.findById( 1L ) ).thenReturn( Optional.of( category ) );
        when( supplierRepository.findById( 2L ) ).thenReturn( Optional.of( supplier ) );

        productService.updateProduct( 1L, request() );

        assertEquals( "Beans", existing.getName() );
        assertEquals( "1kg", existing.getDescription() );
        assertEquals( 5, existing.getQuantity() );
        assertEquals( 1, existing.getMinimumQuantity() );
        assertEquals( 20, existing.getMaximumQuantity() );
        assertEquals( new BigDecimal( "7.50" ), existing.getPrice() );
        verify( productRepository ).save( existing );
    }

    @Test
    void updateProduct_productNotFound_throws()
    {
        when( productRepository.findById( 9L ) ).thenReturn( Optional.empty() );

        assertThrows( RecordNotFoundException.class, () -> productService.updateProduct( 9L, request() ) );
    }

    @Test
    void deleteProduct_deletesExisting()
    {
        Product existing = product( 1L );
        when( productRepository.findById( 1L ) ).thenReturn( Optional.of( existing ) );

        productService.deleteProduct( 1L );

        verify( productRepository ).delete( existing );
    }

    @Test
    void deleteProduct_notFound_throws()
    {
        when( productRepository.findById( 9L ) ).thenReturn( Optional.empty() );

        assertThrows( RecordNotFoundException.class, () -> productService.deleteProduct( 9L ) );
    }

    @Test
    void downloadSheet_generatesWorkbookWithHeaderAndRows() throws Exception
    {
        when( productRepository.findAll() ).thenReturn( List.of( product( 1L ) ) );

        byte[] bytes = productService.downloadSheet();

        try ( HSSFWorkbook workbook = new HSSFWorkbook( new ByteArrayInputStream( bytes ) ) )
        {
            HSSFSheet sheet = workbook.getSheet( "Products" );
            assertNotNull( sheet );
            assertEquals( "Nome", sheet.getRow( 0 ).getCell( 0 ).getStringCellValue() );
            assertEquals( "Rice", sheet.getRow( 1 ).getCell( 0 ).getStringCellValue() );
            assertEquals( 10, (int) sheet.getRow( 1 ).getCell( 2 ).getNumericCellValue() );
            assertEquals( "Food", sheet.getRow( 1 ).getCell( 5 ).getStringCellValue() );
            assertEquals( "Acme", sheet.getRow( 1 ).getCell( 7 ).getStringCellValue() );
        }
    }
}
