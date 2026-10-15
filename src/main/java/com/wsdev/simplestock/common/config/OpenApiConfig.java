package com.wsdev.simplestock.common.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig
{
    private static final String BEARER_SCHEME = "bearerAuth";

    @Bean
    public OpenAPI inventoryOpenAPIConfig()
    {
        return new OpenAPI()
                .info( new Info()
                        .title( "Inventory Control API" )
                        .version( "v1" )
                        .description( "API for inventory control.\n\n"
                                + "Authenticate with `POST /api/v1/auth/login` and use the returned token in the **Authorize** button. "
                                + "Each endpoint requires a permission (e.g. `CATEGORY_READ`); the super user and admins have all of them, "
                                + "regular users only the ones granted through `PUT /api/v1/users/{id}/permissions`. "
                                + "A request without the required permission is answered with 403." ) )
                .components( new Components().addSecuritySchemes( BEARER_SCHEME,
                        new SecurityScheme().type( SecurityScheme.Type.HTTP ).scheme( "bearer" ).bearerFormat( "JWT" ) ) )
                .addSecurityItem( new SecurityRequirement().addList( BEARER_SCHEME ) );
    }
}
