package com.wsdev.simplestock.domain.product;

import com.wsdev.simplestock.domain.category.dto.request.CategoryRequestDTO;
import com.wsdev.simplestock.domain.product.dto.request.ProductRequestDTO;
import com.wsdev.simplestock.domain.product.dto.response.ProductResponseDTO;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;

@RestController()
@CrossOrigin( origins = "http://localhost:5173" )
@RequestMapping( "/api/v1/product" )
public class ProductController
{
    @Autowired
    private ProductService productService;

    @GetMapping()
    @ResponseStatus( HttpStatus.OK )
    @PreAuthorize( "hasAuthority('PRODUCT_READ')" )
    public Page<ProductResponseDTO> getProducts( Pageable pageable )
    {
        return productService.getProducts( pageable );
    }

    @GetMapping( "/{id}" )
    @ResponseStatus( HttpStatus.OK )
    @PreAuthorize( "hasAuthority('PRODUCT_READ')" )
    public ProductResponseDTO getProductById( @PathVariable Long id )
    {
        return productService.getProductById( id );
    }

    @GetMapping( "/{category}" )
    @ResponseStatus( HttpStatus.OK )
    @PreAuthorize( "hasAuthority('PRODUCT_READ')" )
    public ProductResponseDTO getProductByCategory( @PathVariable CategoryRequestDTO categoryRequestDTO )
    {
        return  productService.getProductByCategory( categoryRequestDTO );
    }

    @PostMapping( value = "/add", consumes = MediaType.MULTIPART_FORM_DATA_VALUE )
    @ResponseStatus( HttpStatus.CREATED )
    @PreAuthorize( "hasAuthority('PRODUCT_CREATE')" )
    public void addProduct( @Valid @RequestPart( "product" ) ProductRequestDTO productRequestDTO,
                            @RequestPart( value = "file", required = false ) MultipartFile file ) throws Exception
    {
        productService.addProduct( productRequestDTO, file );
    }

    @GetMapping( "/image/{filename:.+}" )
    @PreAuthorize( "hasAuthority('PRODUCT_READ')" )
    public ResponseEntity<Resource> getProductImage( @PathVariable String filename ) throws Exception
    {
        Resource image = productService.getProductImage( filename );

        return ResponseEntity.ok()
                .contentType( MediaType.parseMediaType( productService.getImageContentType( image ) ) )
                .cacheControl( CacheControl.maxAge( Duration.ofHours( 1 ) ).cachePrivate() )
                .body( image );
    }

    @PutMapping( "/update/{id}" )
    @ResponseStatus( HttpStatus.NO_CONTENT )
    @PreAuthorize( "hasAuthority('PRODUCT_UPDATE')" )
    public void updateProduct( @Valid @PathVariable Long id, @RequestBody ProductRequestDTO productRequestDTO )
    {
        productService.updateProduct( id, productRequestDTO );
    }

    @GetMapping( "/download" )
    @PreAuthorize( "hasAuthority('PRODUCT_EXPORT')" )
    public ResponseEntity<byte[]> downloadSheet() throws Exception
    {
        byte[] file = productService.downloadSheet();

        HttpHeaders headers = new HttpHeaders();

        headers.setContentType( MediaType.parseMediaType( "application/vnd.ms-excel" ) );

        headers.setContentDisposition( ContentDisposition.attachment().filename( "Relatorio_Produtos.xls" ).build() );

        return ResponseEntity.ok().headers( headers ).body( file );
    }

    @DeleteMapping( "/delete/{id}" )
    @ResponseStatus( HttpStatus.NO_CONTENT )
    @PreAuthorize( "hasAuthority('PRODUCT_DELETE')" )
    public void deleteProduct( @PathVariable Long id )
    {
        productService.deleteProduct( id );
    }
}
