package com.chitchat.app.configuration;

import com.chitchat.app.util.AppConstants;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@SecurityScheme(
        name = AppConstants.SECURITY_SCHEME_NAME,
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT"
)
public class OpenApiConfig {

    @Bean
    public OpenAPI chitchatOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Chitchat API")
                        .description("Online Chat Application REST API")
                        .version("1.0"))
                .addSecurityItem(new SecurityRequirement().addList(AppConstants.SECURITY_SCHEME_NAME));
    }
}
