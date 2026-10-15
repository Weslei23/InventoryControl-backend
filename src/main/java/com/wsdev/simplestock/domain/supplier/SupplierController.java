package com.wsdev.simplestock.domain.supplier;

import com.wsdev.simplestock.domain.supplier.dto.request.SupplierRequestDTO;
import com.wsdev.simplestock.domain.supplier.dto.response.SupplierResponseDTO;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController()
@CrossOrigin( origins = "http://localhost:5173" )
@RequestMapping( "/api/v1/supplier" )
public class SupplierController
{
    @Autowired
    private SupplierService supplierService;

    @GetMapping()
    @ResponseStatus( HttpStatus.OK )
    @PreAuthorize( "hasAuthority('SUPPLIER_READ')" )
    public Page<SupplierResponseDTO> getSuppliers( Pageable pageable ) throws Exception
    {
        return supplierService.getSuppliers( pageable );
    }

    @GetMapping( "/{id}" )
    @ResponseStatus( HttpStatus.OK )
    @PreAuthorize( "hasAuthority('SUPPLIER_READ')" )
    public SupplierResponseDTO getSupplierById( @PathVariable Long id ) throws Exception
    {
        return supplierService.getSupplierById( id );
    }

    @GetMapping( "/{name}" )
    @ResponseStatus( HttpStatus.OK )
    @PreAuthorize( "hasAuthority('SUPPLIER_READ')" )
    public SupplierResponseDTO getSupplierByName( @PathVariable String name ) throws Exception
    {
        return  supplierService.getSupplierByName( name );
    }

    @PostMapping( "/add" )
    @ResponseStatus( HttpStatus.CREATED )
    @PreAuthorize( "hasAuthority('SUPPLIER_CREATE')" )
    public void addSupplier( @Valid @RequestBody SupplierRequestDTO supplierRequestDTO ) throws Exception
    {
        supplierService.addSupplier( supplierRequestDTO );
    }

    @PutMapping( "/update/{id}" )
    @ResponseStatus( HttpStatus.NO_CONTENT )
    @PreAuthorize( "hasAuthority('SUPPLIER_UPDATE')" )
    public void updateSupplier( @Valid @PathVariable Long id, @RequestBody SupplierRequestDTO supplierRequestDTO ) throws Exception
    {
        supplierService.updateSupplier( id, supplierRequestDTO );
    }

    @DeleteMapping( "/delete/{id}" )
    @ResponseStatus( HttpStatus.NO_CONTENT )
    @PreAuthorize( "hasAuthority('SUPPLIER_DELETE')" )
    public void deleteSupplier( @PathVariable Long id ) throws Exception
    {
        supplierService.deleteSupplier( id );
    }
}
