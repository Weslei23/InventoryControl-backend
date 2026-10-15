package com.wsdev.simplestock.domain.product;

import com.wsdev.simplestock.common.exceptions.RecordNotFoundException;
import com.wsdev.simplestock.domain.category.dto.request.CategoryRequestDTO;
import com.wsdev.simplestock.domain.product.dto.request.ProductRequestDTO;
import com.wsdev.simplestock.domain.product.dto.response.ProductResponseDTO;
import com.wsdev.simplestock.domain.category.mapper.CategoryMapper;
import com.wsdev.simplestock.domain.product.mapper.ProductMapper;
import com.wsdev.simplestock.domain.category.model.Category;
import com.wsdev.simplestock.domain.product.model.Product;
import com.wsdev.simplestock.domain.category.CategoryRepository;
import com.wsdev.simplestock.common.utilities.Validator;
import com.wsdev.simplestock.domain.supplier.SupplierRepository;
import com.wsdev.simplestock.domain.supplier.model.Supplier;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.PathResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
public class ProductService
{
    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private SupplierRepository supplierRepository;

    @Value( "${file.upload-dir}" )
    private String fileDirectory;

    /**
     * getProducts()
     * @return
     */
    public Page<ProductResponseDTO> getProducts( Pageable pageable )
    {
        Page<Product> productResponseDTOS = productRepository.findAll( pageable );

        return productResponseDTOS.map( ProductMapper::entityToDto );
    }

    /**
     *
     * @param id
     * @return
     */
    public ProductResponseDTO getProductById( Long id )
    {
        Product product = productRepository.findById( id ).orElseThrow( () -> new RecordNotFoundException( id ) );

        return ProductMapper.entityToDto( product );
    }

    /**
     *
     * @param categoryName
     * @return
     */
    public Page<ProductResponseDTO> getProductsByCategoryName( String categoryName, Pageable pageable )
    {
        Category category = categoryRepository.getCategoryByName( categoryName );

        Page<Product> products = productRepository.getProductsByCategory( category, pageable );

        return products.map( ProductMapper::entityToDto );
    }

    /**
     *
     * @param categoryRequestDTO
     * @return
     */
    public ProductResponseDTO getProductByCategory( CategoryRequestDTO categoryRequestDTO )
    {
        Product product = productRepository.getProductByCategory( CategoryMapper.dtoToEntity( categoryRequestDTO ) );

        return ProductMapper.entityToDto( product );
    }

    /**
     * addProduct
     * @param productRequestDTO
     */
    public void addProduct( ProductRequestDTO productRequestDTO, MultipartFile file ) throws Exception
    {
        Validator.requiredNonNull( productRequestDTO, new IllegalArgumentException( "Argument 'productDTO' must not be null" ) );

        Category category = categoryRepository.findById( productRequestDTO.getCategoryId() )
                .orElseThrow( () -> new RecordNotFoundException( productRequestDTO.getCategoryId() ) );

        Supplier supplier = supplierRepository.findById( productRequestDTO.getSupplierId() )
                .orElseThrow( () -> new RecordNotFoundException( productRequestDTO.getSupplierId() ) );

        Product product = ProductMapper.dtoToEntity( productRequestDTO );

        if ( file != null && !file.isEmpty() )
        {
            String originalName = Paths.get( file.getOriginalFilename() == null ? "image" : file.getOriginalFilename() ).getFileName().toString();
            String storedName = UUID.randomUUID() + "_" + originalName;

            Path filePath = Paths.get( fileDirectory, storedName );

            Files.createDirectories( filePath.getParent() );
            Files.write( filePath, file.getBytes() );

            product.setImage( storedName );
        }

        product.setCategory( category );
        product.setSupplier( supplier );

        productRepository.save( product );
    }

    /**
     *
     * @param id
     * @param productRequestDTO
     */
    public void updateProduct( Long id, ProductRequestDTO productRequestDTO )
    {
        Product product = productRepository.findById( id ).orElseThrow( () -> new RecordNotFoundException( id ) );

        Category category = categoryRepository.findById( productRequestDTO.getCategoryId() )
                .orElseThrow( () -> new RecordNotFoundException( productRequestDTO.getCategoryId() ) );

        Supplier supplier = supplierRepository.findById( productRequestDTO.getSupplierId() )
                .orElseThrow( () -> new RecordNotFoundException( productRequestDTO.getSupplierId() ) );


        product.setName( productRequestDTO.getName() );
        product.setDescription( productRequestDTO.getDescription() );
        product.setQuantity( productRequestDTO.getQuantity() );
        product.setImage( productRequestDTO.getImage() );
        product.setMaximumQuantity( productRequestDTO.getMaximumQuantity() );
        product.setMinimumQuantity( productRequestDTO.getMinimumQuantity() );
        product.setCategory( category );
        product.setPrice( productRequestDTO.getPrice() );
        product.setSupplier( supplier );

        productRepository.save( product );
    }

    /**
     * Product image (thumbnail) stored by addProduct. Only image files inside the upload directory are served.
     *
     * @param filename
     * @return
     */
    public Resource getProductImage( String filename )
    {
        Path baseDirectory = Paths.get( fileDirectory ).toAbsolutePath().normalize();
        Path file = baseDirectory.resolve( filename ).normalize();

        if ( !file.startsWith( baseDirectory ) || !Files.isRegularFile( file ) || !isImage( file ) )
        {
            throw new RecordNotFoundException( "Image not found: " + filename );
        }

        return new PathResource( file );
    }

    /**
     * Media type of a stored image
     *
     * @param resource
     * @return
     */
    public String getImageContentType( Resource resource ) throws Exception
    {
        String contentType = Files.probeContentType( resource.getFile().toPath() );

        return contentType == null ? "application/octet-stream" : contentType;
    }

    private boolean isImage( Path file )
    {
        try
        {
            String contentType = Files.probeContentType( file );

            return contentType != null && contentType.startsWith( "image/" );
        }
        catch ( java.io.IOException e )
        {
            return false;
        }
    }

    /**
     *
     * @param id
     */
    public void deleteProduct( Long id )
    {
        Product product = productRepository.findById( id ).orElseThrow( () -> new RecordNotFoundException( id ) );

        productRepository.delete( product );
    }


    public byte[] downloadSheet() throws Exception
    {
        HSSFWorkbook workbook = new HSSFWorkbook();

        HSSFSheet sheet = workbook.createSheet( "Products" );

        String[] coluns = { "Nome", "Descrição", "Quantidade Atual", "Quantidade Mínima", "Quantidade Máxima", "Categoria", "Preço", "Fornecedor" };

        HSSFRow headerRow = sheet.createRow( 0 );

        for ( int i = 0; i < coluns.length; i++ )
        {
            HSSFCell cell = headerRow.createCell( i );
            cell.setCellValue( coluns[i] );
        }

        int rowIdx = 1;
        for(  Product product : productRepository.findAll() )
        {
            HSSFRow row = sheet.createRow( rowIdx++ );

            row.createCell( 0 ).setCellValue( product.getName() );
            row.createCell( 1 ).setCellValue( product.getDescription() );
            row.createCell( 2 ).setCellValue( product.getQuantity() );
            row.createCell( 3 ).setCellValue( product.getMinimumQuantity() );
            row.createCell( 4 ).setCellValue( product.getMaximumQuantity() );
            row.createCell( 5 ).setCellValue( product.getCategory().getName() );
            row.createCell( 6 ).setCellValue( product.getPrice().toString() );
            row.createCell( 7 ).setCellValue( product.getSupplier().getName() );
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();

        workbook.write(out);
        workbook.close();

        return out.toByteArray();
    }
}