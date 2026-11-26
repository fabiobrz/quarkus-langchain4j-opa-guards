package dev.langchain4j.quarkus.workshop;

import com.fasterxml.jackson.databind.node.DoubleNode;
import com.styra.opa.wasm.OpaBuiltin;
import com.styra.opa.wasm.OpaPolicy;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.guardrail.InputGuardrail;
import dev.langchain4j.guardrail.InputGuardrailRequest;
import dev.langchain4j.guardrail.InputGuardrailResult;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.quarkus.logging.Log;

import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;

@ApplicationScoped
public class OPAPromptInjectionGuard implements InputGuardrail {

    public static final String POLICIES_PROMPT_INJECTION_WASM_PATH = "/policies/prompt-injection.wasm";

    /** Name of the {@code @UserMessage} template variable on {@link CustomerSupportAgent#chat(String)}. */
    private static final String USER_MESSAGE_VARIABLE = "userMessage";

    @ConfigProperty(name = "opa.policy.path", defaultValue = POLICIES_PROMPT_INJECTION_WASM_PATH)
    String policyPath;
    private OpaPolicy policy;

    private final PromptInjectionDetectionService llmService;

    public OPAPromptInjectionGuard(PromptInjectionDetectionService llmService) {
        this.llmService = llmService;
    }

    @PostConstruct
    public void init() {
        InputStream policyStream = getClass().getResourceAsStream(policyPath);
        if (policyStream == null) {
            throw new RuntimeException("Policy file not found: " + policyPath);
        }

        policy = OpaPolicy.builder()
            .withPolicy(policyStream)
            .addBuiltins(
                OpaBuiltin.from("llm_score", (instance, textNode) -> {
                    String text = textNode.asText();
                    Log.infof("OPA guardrail - pattern inconclusive, consulting LLM for: %.50s...", text);
                    double score = llmService.isInjection(text);
                    Log.infof("OPA guardrail - LLM score: %f", score);
                    return new DoubleNode(score);
                })
            )
            .build();
    }

    /**
     * Quarkus runs input guardrails <em>after</em> RAG augmentation, so {@code request.userMessage()} already has the
     * retrieved terms-of-use chunks appended to it. The policy must see what the user actually typed, so we read the
     * raw {@code userMessage} template variable and only fall back to the augmented message if it is unavailable.
     */
    @Override
    public InputGuardrailResult validate(InputGuardrailRequest request) {
        Object rawUserMessage = request.requestParams().variables().get(USER_MESSAGE_VARIABLE);
        if (rawUserMessage instanceof String text && !text.isBlank()) {
            return validateText(text);
        }
        Log.warnf("OPA guardrail - no '%s' template variable found, falling back to the augmented user message",
                USER_MESSAGE_VARIABLE);
        return validateText(request.userMessage().singleText());
    }

    @Override
    public InputGuardrailResult validate(UserMessage userMessage) {
        return validateText(userMessage.singleText());
    }

    private InputGuardrailResult validateText(String userText) {
        try {
            JsonObject inputObj = Json.createObjectBuilder()
                    .add("text", userText)
                    .build();
            StringWriter sw = new StringWriter();
            Json.createWriter(sw).write(inputObj);
            String input = sw.toString();

            Log.debugf("OPA guardrail - OPA input JSON: %s", input);
            String resultJson = policy.evaluate(input);
            Log.debugf("OPA guardrail - OPA result: %s", resultJson);

            boolean allowed = Json.createReader(new StringReader(resultJson))
                    .readArray()
                    .getJsonObject(0)
                    .getBoolean("result", false);

            if (!allowed) {
                Log.infof("OPA guardrail - BLOCKED: prompt injection detected");
                return failure("Prompt injection detected by OPA policy");
            }
            return success();
        } catch (Exception e) {
            throw new Exceptions.OpaEvaluationException(e);
        }
    }
}
