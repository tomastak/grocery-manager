package group.rohlik.grocerymanager.configuration;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for OpenAPI documentation of the Grocery Manager API.
 *
 * @author Tomas Kramec
 */
@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Grocery Manager API",
                version = "1.0",
                description = "RESTful API for managing products and orders with stock reservation",
                contact = @Contact(name = "Tomas Kramec", email = "tomas.kramec@gmail.com")
        )
)
public class OpenApiConfiguration {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .components(new Components()
                        .addSecuritySchemes("basic-key",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("basic"))
                )
                .addSecurityItem(new SecurityRequirement().addList("basic-key"));
    }
}