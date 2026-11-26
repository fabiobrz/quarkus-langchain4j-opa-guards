package dev.langchain4j.quarkus.workshop;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.params.provider.Arguments.arguments;

/**
 * Pure unit tests for {@link PromptInjectionDetectionService#isInjection(String)} - no Quarkus, no Ollama.
 * <p>
 * The LLM is asked for a bare float but is under no obligation to comply, and a strict {@code double} binding
 * turns any deviation into a guardrail outage. These cases pin down what the lenient parse tolerates.
 */
public class PromptInjectionScoreParsingTest {

    /** Stubs the AI method so only the parsing in the default method is under test. */
    private static PromptInjectionDetectionService serviceReturning(String reply) {
        return userQuery -> reply;
    }

    static Stream<Arguments> replies() {
        return Stream.of(
                arguments("1.0", 1.0),
                arguments(" 1.0", 1.0),                       // what Ollama actually sends today
                arguments(" 0.95\n", 0.95),
                arguments("0", 0.0),
                arguments(".5", 0.5),
                arguments("Score: 0.8", 0.8),
                arguments("```\n0.9\n```", 0.9),
                // the reply that broke the demo
                arguments(" 0.25\n\nThe user query does not explicitly try to persuade me to take any new action.",
                        0.25));
    }

    @ParameterizedTest(name = "[{index}] {1} <- {0}")
    @MethodSource("replies")
    void extractsScoreFromModelReply(String reply, double expected) {
        assertEquals(expected, serviceReturning(reply).isInjection("any query"), 1e-9);
    }

    @Test
    void failsLoudlyWhenTheReplyHasNoNumber() {
        var service = serviceReturning("I cannot help with that request.");

        var e = assertThrows(IllegalStateException.class, () -> service.isInjection("any query"));
        assertTrue(e.getMessage().contains("I cannot help with that request."),
                "The unusable reply should be in the message: " + e.getMessage());
    }

    @Test
    void failsLoudlyOnANullReply() {
        assertThrows(IllegalStateException.class, () -> serviceReturning(null).isInjection("any query"));
    }
}
