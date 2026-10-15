package com.wsdev.simplestock.domain.movement;

import com.wsdev.simplestock.common.ControllerTestSupport;
import com.wsdev.simplestock.common.exceptions.RecordNotFoundException;
import com.wsdev.simplestock.domain.movement.dto.request.MovementRequestDTO;
import com.wsdev.simplestock.domain.movement.dto.response.MovementResponseDTO;
import com.wsdev.simplestock.domain.movement.model.enums.MovementType;
import com.wsdev.simplestock.domain.user.model.enums.Permission;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest( MovementController.class )
class MovementControllerTest extends ControllerTestSupport
{
    private static final String INBOUND = "{\"productId\":1,\"userId\":2,\"movementType\":\"INBOUND\",\"quantity\":5}";
    private static final String OUTBOUND = "{\"productId\":1,\"userId\":2,\"movementType\":\"OUTBOUND\",\"quantity\":5}";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MovementService movementService;

    @Test
    void inbound_withoutCreatePermission_returns403() throws Exception
    {
        mockMvc.perform( post( "/api/v1/movement/inbound" )
                        .with( auth( Permission.MOVEMENT_READ ) )
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( INBOUND ) )
                .andExpect( status().isForbidden() );

        verifyNoInteractions( movementService );
    }

    @Test
    void list_withoutToken_returns403() throws Exception
    {
        mockMvc.perform( get( "/api/v1/movement" ) ).andExpect( status().isForbidden() );
    }

    @Test
    void list_returnsPage() throws Exception
    {
        MovementResponseDTO dto = new MovementResponseDTO();
        dto.setId( 1L );
        dto.setMovementType( MovementType.INBOUND );
        dto.setUserName( "john" );
        dto.setQuantity( 5 );
        when( movementService.getMovements( any() ) ).thenReturn( new PageImpl<>( List.of( dto ) ) );

        mockMvc.perform( get( "/api/v1/movement" ).with( auth() ) )
                .andExpect( status().isOk() )
                .andExpect( jsonPath( "$.content[0].userName" ).value( "john" ) )
                .andExpect( jsonPath( "$.content[0].movementType" ).value( "INBOUND" ) );
    }

    @Test
    void inbound_returns200AndParsesBody() throws Exception
    {
        mockMvc.perform( post( "/api/v1/movement/inbound" )
                        .with( auth() )
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( INBOUND ) )
                .andExpect( status().isOk() );

        ArgumentCaptor<MovementRequestDTO> captor = ArgumentCaptor.forClass( MovementRequestDTO.class );
        verify( movementService ).entryProduct( captor.capture() );
        assertEquals( MovementType.INBOUND, captor.getValue().getMovementType() );
        assertEquals( 5, captor.getValue().getQuantity() );
    }

    @Test
    void outbound_returns200() throws Exception
    {
        mockMvc.perform( post( "/api/v1/movement/outbound" )
                        .with( auth() )
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( OUTBOUND ) )
                .andExpect( status().isOk() );

        verify( movementService ).exitProduct( any( MovementRequestDTO.class ) );
    }

    @Test
    void add_returns201() throws Exception
    {
        mockMvc.perform( post( "/api/v1/movement/add" )
                        .with( auth() )
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( INBOUND ) )
                .andExpect( status().isCreated() );

        verify( movementService ).addMovement( any( MovementRequestDTO.class ) );
    }

    @Test
    void add_productNotFound_returns404() throws Exception
    {
        doThrow( new RecordNotFoundException( 1L ) ).when( movementService ).addMovement( any() );

        mockMvc.perform( post( "/api/v1/movement/add" )
                        .with( auth() )
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( INBOUND ) )
                .andExpect( status().isNotFound() );
    }

    @Test
    void update_returns204() throws Exception
    {
        mockMvc.perform( put( "/api/v1/movement/update/3" )
                        .with( auth() )
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( INBOUND ) )
                .andExpect( status().isNoContent() );

        verify( movementService ).updateMovement( eq( 3L ), any( MovementRequestDTO.class ) );
    }

    @Test
    void delete_returns204() throws Exception
    {
        mockMvc.perform( delete( "/api/v1/movement/delete/3" ).with( auth() ) )
                .andExpect( status().isNoContent() );

        verify( movementService ).deleteMovement( 3L );
    }
}
