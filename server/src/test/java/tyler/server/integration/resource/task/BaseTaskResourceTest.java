package tyler.server.integration.resource.task;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.parsing.Parser;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import tyler.server.dto.auth.AuthRequest;

import static io.restassured.RestAssured.given;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class BaseTaskResourceTest {
    protected static final String TASKS_ENDPOINT = "/tasks";
    protected static final String AUTH_ENDPOINT = "/auth";

    @ServiceConnection
    public static final PostgreSQLContainer<?> postgreSQLContainer = new PostgreSQLContainer<>("postgres:16.8-alpine3.20");

    static {
        postgreSQLContainer.start();
    }

    @Autowired
    protected PasswordEncoder passwordEncoder;

    @BeforeAll
    void setUp(@Value("${local.server.port}") int port) {
        RestAssured.port = port;
        RestAssured.registerParser("text/plain", Parser.TEXT);
    }

    protected String getAuthToken(String username, String password) {
        AuthRequest authRequest = new AuthRequest(username, password);
        return given()
                .contentType(ContentType.JSON)
                .body(authRequest)
                .when()
                .post(AUTH_ENDPOINT + "/login")
                .then()
                .statusCode(200)
                .extract()
                .path("token");
    }

    protected RequestSpecification givenToken(String token) {
        return given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + token);
    }
} 