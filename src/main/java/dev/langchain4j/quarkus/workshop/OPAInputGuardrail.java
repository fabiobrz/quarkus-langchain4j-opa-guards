package dev.langchain4j.quarkus.workshop;

import com.styra.opa.wasm.OpaPolicy;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.guardrail.InputGuardrail;
import dev.langchain4j.guardrail.InputGuardrailResult;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

@ApplicationScoped
public class OPAInputGuardrail implements InputGuardrail {

    public static final String POLICIES_PROMPT_INJECTION_WASM_PATH = "/policies/prompt-injection.wasm";

    @ConfigProperty(name = "opa.policy.path", defaultValue = POLICIES_PROMPT_INJECTION_WASM_PATH)
    String policyPath;

    private OpaPolicy policy;

    @PostConstruct
    public void init() {
        try (InputStream policyStream = getClass().getResourceAsStream(policyPath)) {
            if (policyStream == null) {
                throw new RuntimeException("Policy file not found: " + policyPath);
            }
            
            Path tempFile = Files.createTempFile("opa-policy", ".wasm");
            Files.copy(policyStream, tempFile, StandardCopyOption.REPLACE_EXISTING);
            
            policy = OpaPolicy.builder()
                .withPolicy(tempFile.toFile())
                .build();
                
            tempFile.toFile().deleteOnExit();
        } catch (IOException e) {
            throw new RuntimeException("Failed to load OPA policy", e);
        }
    }

    @Override
    public InputGuardrailResult validate(UserMessage userMessage) {
        try {
            String input = String.format("{\"text\": \"%s\"}", 
                userMessage.singleText().replace("\"", "\\\""));
            
            String resultJson = policy.evaluate(input);
            
            if (resultJson.contains("false")) {
                return failure("Prompt injection detected by OPA policy");
            }
            return success();
        } catch (Exception e) {
            throw new RuntimeException("Failed to evaluate OPA policy", e);
        }
    }
}
