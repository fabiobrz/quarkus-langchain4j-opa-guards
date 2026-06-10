package dev.langchain4j.quarkus.workshop;

import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.guardrail.InputGuardrail;
import dev.langchain4j.guardrail.InputGuardrailResult;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class LLMPromptInjectionGuard implements InputGuardrail {

    private final PromptInjectionDetectionService service;

    public LLMPromptInjectionGuard(PromptInjectionDetectionService service) {
        this.service = service;
    }

    @Override
    public InputGuardrailResult validate(UserMessage userMessage) {
        double result = service.isInjection(userMessage.singleText());
        if (result > 0.7) {
            Log.infof("LLM guardrail - BLOCKED: prompt injection detected by LLM");
            return failure("Prompt injection detected by LLM");
        }
        return success();
    }
}
