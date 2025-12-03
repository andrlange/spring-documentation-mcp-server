# Capability Planning Guide (Enriched with Examples)

This document describes the workflow for creating an AI‑driven capability planning document before starting feature development.

---

## 1. Define the Feature & Context

### What to do
- Provide a short 3–5 sentence feature description.
- List the systems involved (services, DBs, APIs, queues).
- Capture constraints such as SLAs, compliance policies, or deadlines.
- Ask AI to rewrite the description into an executive summary and problem statement.

### Example
**Feature:**  
A new “Flavor Management” module in a Spring Boot backend allowing admins to create, edit, and categorize flavor guidelines. The system must support search, versioning, and audit logging.

**Executive Summary (AI-generated):**  
“Add an admin‑only service for storing and retrieving flavor guidelines, including categorization, markdown content, version history, and audit trails.”

---

## 2. Analyse LLM Weaknesses & Knowledge Gaps

### What to do
- Identify where the AI model may be outdated compared to your stack.
- Note breaking changes or new APIs introduced after the model cutoff.
- List internal company constraints or architecture rules the AI cannot know.

### Example
- The LLM assumes Spring Boot 2.x defaults, but the system uses **Spring Boot 3.2** with Jakarta EE namespace migration.
- Internal authentication uses a custom `CompanyJwtFilter` not known to the LLM.
- The model still gives advice about `WebSecurityConfigurerAdapter`, which is obsolete.

**AI Prompt Example:**  
“Given Spring Boot 3.2, which of your assumptions or code suggestions may be outdated?”

---

## 3. Estimate Impact (Speed, Error Rate, Token Usage)

### What to do
- Estimate developer time saved.
- Estimate reduced error rate in reviews.
- Estimate token cost before vs after adding agentic capability.

### Example
| Metric | Baseline | After Capability | Improvement |
|--------|----------|------------------|-------------|
| Developer setup time | 45 min | 10 min | 78% |
| Review correction cycles | 3 rounds | 1 round | 66% |
| Tokens per flow | 12k | 7k | ~40% reduction |

**Reasoning:**  
Letting the AI pre‑generate entity classes, controllers, and validation logic reduces cognitive overhead and rework.

---

## 4. Analyse Feature Parameters

### What to do
Document:
- Inputs (schemas, code, existing APIs)
- Outputs (code files, tests, configs)
- Non-functional requirements (latency, security)
- Agentic needs (MCP tools, context readers)
- Failure modes

### Example
**Inputs:**  
- Flavor schema: `id`, `name`, `description`, `categories`, `version`, `content`

**Outputs:**  
- Spring Boot REST controller  
- JPA entity + repository  
- Validation + error handling logic  
- Unit + integration tests  

**Failure Modes Identified by AI:**  
- Wrong use of deprecated APIs  
- Improper handling of version conflicts  
- Missing audit fields  
- Incorrect transaction boundaries  

---

## 5. Create the Detailed Capability Plan

### What to do
Define:
- The abilities the AI must have
- What documents or code it needs to read
- Tools it will use
- Guardrails and policies
- Success metrics

### Example Capability Plan

**Capabilities Needed:**  
1. Generate CRUD controllers tailored to internal company guidelines.  
2. Validate entity design against security and compliance rules.  
3. Propose migration scripts for new DB schemas.  
4. Produce integration tests with Testcontainers.  

**Knowledge Sources:**  
- `architecture-guidelines.md`  
- `naming-conventions.md`  
- `error-handling-standards.md`  

**Tools:**  
- MCP tool: `searchFlavors`  
- MCP tool: `writeFile`  
- DB schema introspection tool  

**Guardrails:**  
- Must not generate endpoints without RBAC annotations.  
- Must never propose publicly exposed admin APIs.  

**Success Metrics:**  
- 70% reduction in review comments related to architecture.  
- At least one complete test suite generated automatically.

---

## 6. Write the Detailed Implementation Plan

### What to do
Break into phases: backend, frontend, rollout, testing.

### Example Implementation Plan

**Phase 1 — Backend Foundations**
- Create `flavor` entity and migration + Flyway script.
- Implement `FlavorService` with versioning logic.
- Add search + pagination support.

**Phase 2 — AI Agent Integration**
- Add MCP tool to retrieve flavor categories.
- Register context loader for guidelines.
- Add AI validation step in PR workflow.

**Phase 3 — UI & Developer Experience**
- Add admin UI page using React.  
- Provide `/generate-flavor-template` action for developers.

**Phase 4 — Rollout**
- Introduce feature flag: `flavor.mgmt.enabled`.  
- Internal beta testing and supervised AI-driven code generation.

---

## 7. Decision & Next Steps

### What to do
Write the final evaluation and actions.

### Example
**Decision:** Proceed.  
**Reasons:** Well‑defined scope, high productivity gain, minimal risk.  

**Next Actions:**  
1. Create repository module: `flavors-core`.  
2. Write initial Flyway migration.  
3. Implement `searchFlavors` MCP tool.  
4. Seed 3 sample flavor files for testing.

---

## Minimal Template (Ready to Copy)

1. **Executive Summary & Decision**  
2. **Current Problem & LLM Limitations**  
3. **Impact Estimate**  
4. **Feature Parameters & Constraints**  
5. **Capability Plan**  
6. **Implementation Plan**  
7. **Risks & Success Metrics**

