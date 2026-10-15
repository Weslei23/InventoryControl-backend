package com.wsdev.simplestock.domain.movement;

import com.wsdev.simplestock.domain.movement.dto.request.MovementRequestDTO;
import com.wsdev.simplestock.domain.movement.dto.response.MovementResponseDTO;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController()
@CrossOrigin( origins = "http://localhost:5173" )
@RequestMapping( "/api/v1/movement" )
public class MovementController
{
    @Autowired
    private MovementService movementService;

    @GetMapping()
    @ResponseStatus( HttpStatus.OK )
    @PreAuthorize( "hasAuthority('MOVEMENT_READ')" )
    public Page<MovementResponseDTO> getCategories( Pageable pageable ) throws Exception
    {
        return movementService.getMovements( pageable );
    }

    @PostMapping( "/inbound" )
    @ResponseStatus( HttpStatus.OK )
    @PreAuthorize( "hasAuthority('MOVEMENT_CREATE')" )
    public void entryProduct( @Valid @RequestBody MovementRequestDTO movementRequestDTO ) throws Exception
    {
        movementService.entryProduct( movementRequestDTO );
    }

    @PostMapping( "/outbound" )
    @ResponseStatus( HttpStatus.OK )
    @PreAuthorize( "hasAuthority('MOVEMENT_CREATE')" )
    public void exitProduct( @Valid @RequestBody MovementRequestDTO movementRequestDTO ) throws Exception
    {
        movementService.exitProduct( movementRequestDTO );
    }

    @PostMapping( "/add" )
    @ResponseStatus( HttpStatus.CREATED )
    @PreAuthorize( "hasAuthority('MOVEMENT_CREATE')" )
    public void addMovement( @Valid @RequestBody MovementRequestDTO movementRequestDTO ) throws Exception
    {
        movementService.addMovement( movementRequestDTO );
    }

    @PutMapping( "/update/{id}" )
    @ResponseStatus( HttpStatus.NO_CONTENT )
    @PreAuthorize( "hasAuthority('MOVEMENT_UPDATE')" )
    public void updateMovement( @Valid @PathVariable Long id, @RequestBody MovementRequestDTO movementRequestDTO ) throws Exception
    {
        movementService.updateMovement( id, movementRequestDTO );
    }

    @DeleteMapping( "/delete/{id}" )
    @ResponseStatus( HttpStatus.NO_CONTENT )
    @PreAuthorize( "hasAuthority('MOVEMENT_DELETE')" )
    public void deleteMovement( @PathVariable Long id ) throws Exception
    {
        movementService.deleteMovement( id );
    }
}