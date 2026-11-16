package io.github.anevadog.pendla;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import com.jayway.jsonpath.JsonPath;

import io.github.anevadog.pendla.application.JobApplicationRepository;
import io.github.anevadog.pendla.user.AppUserRepository;

/**
 * The whole application together: security, controllers, services, JPA and a real PostgreSQL
 * in a Docker container (Testcontainers). Nothing is mocked and the tokens are real: each test
 * registers its users and logs in through the API.
 *
 * The other tests check each layer on its own. This one checks that the layers fit together,
 * above all that the user in the token decides which job applications can be seen.
 *
 * MockMvc calls the application without a server or a network port, but the security filters run.
 * Skipped when Docker is not running. The JobTech search is not called here.
 */
// The whole application context, not only one layer. The properties make the test independent of .env
@SpringBootTest(properties = {
		"jwt.secret=test-secret-with-at-least-32-characters",
		"registration.enabled=true" })
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
class ApiIntegrationTest {

	private static final String PASSWORD = "secret-password";

	// static: one container for all the tests in this class
	@Container
	static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:18");

	// The container gets a random port, so the connection data is only known once it has started
	@DynamicPropertySource
	static void datasourceProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", postgres::getJdbcUrl);
		registry.add("spring.datasource.username", postgres::getUsername);
		registry.add("spring.datasource.password", postgres::getPassword);
	}

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JobApplicationRepository applicationRepository;

	@Autowired
	private AppUserRepository userRepository;

	// Every request commits its own transaction, as in production, so nothing is rolled back
	// at the end of a test: each test starts by emptying the tables. Applications first (foreign key).
	@BeforeEach
	void emptyTables() {
		applicationRepository.deleteAll();
		userRepository.deleteAll();
	}

	@Test
	void fullFlow_registerLoginCreateChangeStatusDelete_works() throws Exception {
		String token = registerAndLogin("alex");

		Integer id = createApplication(token, "31696992");

		mockMvc.perform(patch("/api/applications/{id}/status", id)
				.header(HttpHeaders.AUTHORIZATION, bearer(token))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"status": "SENT"}
						"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("SENT"));

		mockMvc.perform(get("/api/applications").header(HttpHeaders.AUTHORIZATION, bearer(token)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1))
				.andExpect(jsonPath("$[0].id").value(id))
				.andExpect(jsonPath("$[0].status").value("SENT"));

		mockMvc.perform(delete("/api/applications/{id}", id).header(HttpHeaders.AUTHORIZATION, bearer(token)))
				.andExpect(status().isNoContent());

		mockMvc.perform(get("/api/applications/{id}", id).header(HttpHeaders.AUTHORIZATION, bearer(token)))
				.andExpect(status().isNotFound());
	}

	@Test
	void getApplication_ofAnotherUser_returns404() throws Exception {
		String alexToken = registerAndLogin("alex");
		String mariaToken = registerAndLogin("maria");
		Integer alexApplicationId = createApplication(alexToken, "31696992");

		// Maria cannot read, change or delete the application of Alex
		mockMvc.perform(get("/api/applications/{id}", alexApplicationId)
				.header(HttpHeaders.AUTHORIZATION, bearer(mariaToken)))
				.andExpect(status().isNotFound());
		mockMvc.perform(patch("/api/applications/{id}/status", alexApplicationId)
				.header(HttpHeaders.AUTHORIZATION, bearer(mariaToken))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"status": "SENT"}
						"""))
				.andExpect(status().isNotFound());
		mockMvc.perform(delete("/api/applications/{id}", alexApplicationId)
				.header(HttpHeaders.AUTHORIZATION, bearer(mariaToken)))
				.andExpect(status().isNotFound());

		// Her list is empty, and the application of Alex is still there and unchanged
		mockMvc.perform(get("/api/applications").header(HttpHeaders.AUTHORIZATION, bearer(mariaToken)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(0));
		mockMvc.perform(get("/api/applications/{id}", alexApplicationId)
				.header(HttpHeaders.AUTHORIZATION, bearer(alexToken)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("SAVED"));
	}

	@Test
	void createApplication_sameJobtechIdTwice_returns409() throws Exception {
		String alexToken = registerAndLogin("alex");
		String mariaToken = registerAndLogin("maria");
		createApplication(alexToken, "31696992");

		mockMvc.perform(post("/api/applications")
				.header(HttpHeaders.AUTHORIZATION, bearer(alexToken))
				.contentType(MediaType.APPLICATION_JSON)
				.content(applicationJson("31696992")))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.status").value(409));

		// The same ad saved by another user is not a duplicate
		createApplication(mariaToken, "31696992");
	}

	@Test
	void listApplications_withoutToken_returns401() throws Exception {
		mockMvc.perform(get("/api/applications"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.status").value(401))
				.andExpect(jsonPath("$.title").value("Unauthorized"));
	}

	@Test
	void listApplications_withTokenOfDeletedUser_returns401() throws Exception {
		String token = registerAndLogin("alex");
		userRepository.deleteAll();

		// The token is still valid (signature and expiry), but its user no longer exists
		mockMvc.perform(get("/api/applications").header(HttpHeaders.AUTHORIZATION, bearer(token)))
				.andExpect(status().isUnauthorized());
	}

	// Creates the account and logs in through the API. Returns the token.
	private String registerAndLogin(String username) throws Exception {
		String credentials = """
				{"username": "%s", "password": "%s"}
				""".formatted(username, PASSWORD);

		mockMvc.perform(post("/api/auth/register")
				.contentType(MediaType.APPLICATION_JSON)
				.content(credentials))
				.andExpect(status().isCreated());

		String body = mockMvc.perform(post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(credentials))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		return JsonPath.read(body, "$.token");
	}

	// Saves an application through the API and checks it starts as SAVED. Returns its id.
	private Integer createApplication(String token, String jobtechId) throws Exception {
		String body = mockMvc.perform(post("/api/applications")
				.header(HttpHeaders.AUTHORIZATION, bearer(token))
				.contentType(MediaType.APPLICATION_JSON)
				.content(applicationJson(jobtechId)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.status").value("SAVED"))
				.andReturn().getResponse().getContentAsString();
		return JsonPath.read(body, "$.id");
	}

	private String applicationJson(String jobtechId) {
		return """
				{"jobtechId": "%s", "title": "Java developer", "employer": "Deploja AB"}
				""".formatted(jobtechId);
	}

	private String bearer(String token) {
		return "Bearer " + token;
	}

}
