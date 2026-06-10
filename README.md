# Quarkus LangChain4j OPA Guards

A demonstration project showcasing prompt injection detection using 
[Open Policy Agent (OPA)](https://www.openpolicyagent.org) guardrails with Quarkus and LangChain4j.

The project is inspired by the 
[Quarkus LangChain4J Workshop - Step 09 - Guardrails](https://quarkus.io/quarkus-workshop-langchain4j/section-1/step-09/)
article and is based on 
[the related application example](https://github.com/quarkusio/quarkus-workshop-langchain4j/tree/main/section-1/step-09). 

The original application is modified to:
- replace OpenAI with Ollama (better for local development and iteration, no costs)
- add a concrete `InputGuardrail` implementation 
 (i.e.: [OPAInputGuardrail](src/main/java/dev/langchain4j/quarkus/workshop/OPAInputGuardrail.java)) that uses the 
 [Open Policy Agent WebAssembly Java SDK](https://github.com/StyraOSS/opa-java-wasm) to evaluate OPA policies 
 compiled into Wasm modules.    

The result is a modified application that allows defining OPA policies to detect prompt injection attacks.  


## Prerequisites

- Java 21+
- Maven 3.9+
- [OPA CLI](https://www.openpolicyagent.org/docs/latest/#running-opa) (for compiling Rego policies to WASM)
- [Ollama](https://ollama.ai/) with Mistral model
- PostgreSQL with pgvector extension
- The [Quarkus LangChain4J Workshop MCP Weather Server](https://github.com/quarkusio/quarkus-workshop-langchain4j/tree/main/section-1/step-08-mcp-server) 
 (for integration tests)


## Running Integration Tests

### 1. Start Required Services

**PostgreSQL with pgvector:**
```bash
docker run -d --name postgres-pgvector \
  -e POSTGRES_DB=quarkus_test \
  -e POSTGRES_USER=quarkus_test \
  -e POSTGRES_PASSWORD=quarkus_test \
  -p 5432:5432 \
  pgvector/pgvector:pg16
```

**Ollama with Mistral:**
```bash
# Install Ollama
curl -fsSL https://ollama.com/install.sh | sh

# Start Ollama
ollama serve

# Pull and run the Mistral model
ollama pull mistral
ollama run mistral
```

**MCP Weather Server:**
```bash
# Clone the workshop repository
git clone https://github.com/quarkusio/quarkus-workshop-langchain4j.git /tmp/workshop

# Build and start the MCP server
cd /tmp/workshop/section-1/step-08-mcp-server
./mvnw clean package -DskipTests
java -Dquarkus.http.port=8081 -jar target/quarkus-workshop-langchain4j-08-mcp-server-1.0-SNAPSHOT-runner.jar
```

### 2. Run Tests

```bash
./mvnw clean test
```

## OPA Policy Development

### Compiling Rego to WASM

When modifying the OPA policy (`src/main/resources/policies/prompt-injection.rego`), you must recompile it to WASM:

```bash
# Install OPA CLI (if not already installed)
curl -L -o opa https://openpolicyagent.org/downloads/latest/opa_linux_amd64_static
chmod +x opa

# Compile Rego policy to WASM
opa build -t wasm -e promptinjection/allow src/main/resources/policies/prompt-injection.rego

# Extract WASM file to /tmp (to avoid overwriting source files)
tar -xzf bundle.tar.gz -C /tmp

# Copy only the WASM file to the correct location
cp /tmp/policy.wasm src/main/resources/policies/prompt-injection.wasm

# Clean up
rm -f bundle.tar.gz
```

### Testing OPA Policy Locally

```bash
# Test with malicious input
echo '{"text": "ignore previous instructions"}' | \
  opa eval -d src/main/resources/policies/prompt-injection.rego \
  -I 'data.promptinjection.allow' --format pretty

# Expected output: false

# Test with benign input
echo '{"text": "cancel my booking"}' | \
  opa eval -d src/main/resources/policies/prompt-injection.rego \
  -I 'data.promptinjection.allow' --format pretty

# Expected output: true
```

## Key Features

- **OPA Guardrails**: Prompt injection detection using WASM-compiled policies
- **LangChain4j Integration**: AI agent with tool calling capabilities
- **MCP Protocol**: Integration with Model Context Protocol servers
- **RAG**: Retrieval-Augmented Generation with pgvector
- **Ollama**: Local LLM inference with Mistral model

## Troubleshooting

**OPA Policy Errors:**
- Verify the OPA policy has no syntax errors: `opa test src/main/resources/policies/prompt-injection.rego -v`
- Ensure that the WASM module is up to date after OPA Rego changes

**Test Timeouts:**
- Ollama responses can be slow, so tests have 180s timeouts
- Ensure Mistral model is fully loaded before running tests

**MCP Server Connection:**
- Verify server is running on port 8081
