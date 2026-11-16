package io.github.anevadog.pendla.common;

import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;

/**
 * General information shown at the top of Swagger UI. The endpoints themselves are documented in each controller.
 *
 * The security scheme adds the "Authorize" button: paste the token from /api/auth/login there
 * and Swagger sends it in the Authorization header of every request.
 */
@Configuration
@OpenAPIDefinition(
		info = @Info(
				title = "Pendla API",
				version = "0.0.1",
				description = "Search real job ads in Sweden (JobTech JobSearch API) and track the status of my applications. "
						+ "Register and log in at /api/auth to get a token. Endpoints with a lock return 401 without a valid token. "
						+ "Errors are returned as ProblemDetail (RFC 9457)."),
		security = @SecurityRequirement(name = "bearerAuth"))
@SecurityScheme(name = "bearerAuth", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT")
public class OpenApiConfig {
}
