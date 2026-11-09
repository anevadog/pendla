package io.github.anevadog.pendla.common;

import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;

/**
 * General information shown at the top of Swagger UI. The endpoints themselves are documented in each controller.
 */
@Configuration
@OpenAPIDefinition(info = @Info(
		title = "Pendla API",
		version = "0.0.1",
		description = "Search real job ads in Sweden (JobTech JobSearch API) and track the status of my applications. "
				+ "Errors are returned as ProblemDetail (RFC 9457)."))
public class OpenApiConfig {
}
