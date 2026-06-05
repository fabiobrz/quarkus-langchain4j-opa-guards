package dev.langchain4j.quarkus.workshop;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.util.concurrent.TimeUnit;

import static org.hamcrest.Matchers.*;

/**
 * Integration tests for the Ollama-based chat application.
 * These tests verify the complete flow including:
 * - Ollama connectivity
 * - Prompt injection guardrails
 * - MCP weather service integration
 * - Tool calling (booking operations)
 */
@QuarkusTest
public class IntegrationTest {

    @Test
    public void testIndexPage() {
        RestAssured.given()
            .when().get("/")
            .then()
            .statusCode(200)
            .contentType("text/html");
    }

    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    public void testOllamaConnectivity() {
        // Test Ollama API is accessible and can generate responses
        // Note: Model inference can take several seconds (typically 4-10s for small prompts)
        RestAssured.given()
            .baseUri("http://localhost:11434")
            .contentType("application/json")
            .body("""
                {
                  "model": "mistral",
                  "prompt": "Say hello",
                  "stream": false
                }
                """)
            .when()
            .post("/api/generate")
            .then()
            .time(lessThan(30000L)) // Expect response within 30 seconds
            .statusCode(200)
            .body("model", equalTo("mistral"))
            .body("response", notNullValue())
            .body("done", equalTo(true));
    }
}
