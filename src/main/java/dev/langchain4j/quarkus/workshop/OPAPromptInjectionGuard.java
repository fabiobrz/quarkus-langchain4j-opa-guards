package dev.langchain4j.quarkus.workshop;

import com.fasterxml.jackson.databind.node.DoubleNode;
import com.styra.opa.wasm.OpaBuiltin;
import com.styra.opa.wasm.OpaPolicy;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.guardrail.InputGuardrail;
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

    @Override
    public InputGuardrailResult validate(UserMessage userMessage) {
        try {
            String userText = userMessage.singleText();

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
            throw new RuntimeException("Failed to evaluate OPA policy", e);
        }
    }
}
