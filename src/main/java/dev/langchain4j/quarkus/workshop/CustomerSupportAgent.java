package dev.langchain4j.quarkus.workshop;

import dev.langchain4j.service.guardrail.InputGuardrails;
import io.quarkiverse.langchain4j.mcp.runtime.McpToolBox;
import jakarta.enterprise.context.SessionScoped;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import io.quarkiverse.langchain4j.RegisterAiService;
import io.quarkiverse.langchain4j.ToolBox;

@SessionScoped
@RegisterAiService
public interface CustomerSupportAgent {

    @SystemMessage("""
            You are a customer support agent of a car rental company 'Miles of Smiles'.
            You are friendly, polite and concise.
            If the question is unrelated to car rental, you should politely redirect the customer to the right department.
            
            When calling tools or functions, strictly use JSON objects,
            do not wrap in quotes or use plain strings.
            
            Today is {current_date}.
            """)
    // The explicit @UserMessage template is what lets OPAPromptInjectionGuard recover the raw user input:
    // Quarkus exposes the template variables to guardrails, and input guardrails run *after* RAG augmentation.
    @UserMessage("{userMessage}")
    @InputGuardrails(OPAPromptInjectionGuard.class)
    @ToolBox(BookingRepository.class)
    @McpToolBox("weather")
    String chat(String userMessage);
}
