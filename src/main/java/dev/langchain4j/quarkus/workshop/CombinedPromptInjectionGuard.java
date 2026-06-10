package dev.langchain4j.quarkus.workshop;

import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.guardrail.InputGuardrail;
import dev.langchain4j.guardrail.InputGuardrailResult;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class CombinedPromptInjectionGuard implements InputGuardrail {

    private final OPAPromptInjectionGuard opaGuard;
    private final LLMPromptInjectionGuard llmGuard;

    public CombinedPromptInjectionGuard(OPAPromptInjectionGuard opaGuard, LLMPromptInjectionGuard llmGuard) {
        this.opaGuard = opaGuard;
        this.llmGuard = llmGuard;
    }

    @Override
    public InputGuardrailResult validate(UserMessage userMessage) {
        // OPA policy first...
        InputGuardrailResult result = opaGuard.validate(userMessage);
        // if success, trigger LLM guard
        if (result.failures().isEmpty()) {
            result = llmGuard.validate(userMessage);
        }
        return result;
    }
}
