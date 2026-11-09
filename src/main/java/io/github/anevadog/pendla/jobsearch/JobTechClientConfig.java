package io.github.anevadog.pendla.jobsearch;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * Builds the RestClient used to call JobTech, with explicit timeouts.
 * Without timeouts, a JobTech that never answers would block the request thread forever.
 */
@Configuration
public class JobTechClientConfig {

	@Bean
	public RestClient jobTechRestClient(
			@Value("${jobtech.base-url}") String baseUrl,
			@Value("${jobtech.connect-timeout}") Duration connectTimeout,
			@Value("${jobtech.read-timeout}") Duration readTimeout) {

		// connect: time to open the connection; read: time to wait for the response once connected
		SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
		requestFactory.setConnectTimeout(connectTimeout);
		requestFactory.setReadTimeout(readTimeout);

		return RestClient.builder()
				.baseUrl(baseUrl)
				.requestFactory(requestFactory)
				.build();
	}

}
