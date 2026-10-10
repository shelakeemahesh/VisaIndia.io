package com.visasetu.controller;

import com.visasetu.domain.entity.DemoApplication;
import com.visasetu.domain.entity.EntryPoint;
import com.visasetu.domain.entity.VisaCategoryDetail;
import com.visasetu.domain.entity.VisaRule;
import com.visasetu.domain.enums.RouteType;
import com.visasetu.domain.enums.VisaPurpose;
import com.visasetu.repository.DemoApplicationRepository;
import com.visasetu.repository.EntryPointRepository;
import com.visasetu.repository.VisaCategoryDetailRepository;
import com.visasetu.repository.VisaRuleRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

@RestController
@RequiredArgsConstructor
@Slf4j
public class McpServerController {

    private final VisaRuleRepository visaRuleRepository;
    private final VisaCategoryDetailRepository visaCategoryDetailRepository;
    private final EntryPointRepository entryPointRepository;
    private final DemoApplicationRepository demoApplicationRepository;

    @Data
    public static class McpRequest {
        private String jsonrpc = "2.0";
        private String method;
        private Map<String, Object> params;
        private Object id;
    }

    @GetMapping(value = "/llms.txt", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> getLlmsTxt() {
        String content = """
# VisaSetu Model Context Protocol & API Index
> Concept Project — Not affiliated with the Government of India. Official data sourced from indianvisaonline.gov.in, boi.gov.in, mha.gov.in.

## Available Agent Capabilities
- `check_eligibility`: Search all viable visa routes (e-Visa, VoA, Visa-Free, Regular) for any nationality and visit purpose.
- `get_category_info`: Retrieve official specifications, fee schedules, validities, and document requirements for e-Visa categories.
- `list_entry_points`: Access verified list of 37 designated airports, 5 seaports, and land border check posts.
- `track_application`: Check real-time progress and verification milestones for an existing application reference.
- `prepare_draft`: Construct a pre-validated traveler draft application.

## Strict Safety Guardrails & Non-Executables
- SAFE MODE ENFORCED: AI Agents and automated callers CANNOT execute final visa submissions or trigger credit card / Razorpay payments.
- All financial transactions and legal declarations MUST be authorized by the human traveler in the UI.

## Endpoints
- MCP JSON-RPC: POST /api/v1/mcp
- Eligibility API: GET /api/v1/eligibility?nationality={ISO3}&purpose={PURPOSE}
- Categories: GET /api/v1/content/categories
- Entry Points: GET /api/v1/content/entry-points
- Notices: GET /api/v1/content/notices
- Status Tracking: POST /api/v1/applications/track
""";
        return ResponseEntity.ok(content);
    }

    @GetMapping("/api/v1/mcp")
    public ResponseEntity<Map<String, Object>> getMcpManifest() {
        return ResponseEntity.ok(Map.of(
                "protocolVersion", "2024-11-05",
                "serverInfo", Map.of(
                        "name", "visasetu-mcp-server",
                        "version", "1.0.0",
                        "description", "Model Context Protocol interface for VisaSetu travel assistance"
                ),
                "capabilities", Map.of("tools", true),
                "guardrails", Map.of(
                        "safeMode", true,
                        "paymentAutonomousExecution", false,
                        "humanInTheLoopRequired", true
                ),
                "tools", getAvailableTools()
        ));
    }

    @PostMapping("/api/v1/mcp")
    public ResponseEntity<Map<String, Object>> handleMcpJsonRpc(@RequestBody McpRequest request) {
        String method = request.getMethod();
        Object reqId = request.getId();

        if ("tools/list".equals(method)) {
            return ResponseEntity.ok(Map.of(
                    "jsonrpc", "2.0",
                    "id", reqId,
                    "result", Map.of("tools", getAvailableTools())
            ));
        }

        if ("tools/call".equals(method)) {
            Map<String, Object> params = request.getParams() != null ? request.getParams() : Map.of();
            String toolName = (String) params.get("name");
            @SuppressWarnings("unchecked")
            Map<String, Object> arguments = (Map<String, Object>) params.getOrDefault("arguments", Map.of());

            Map<String, Object> result = executeTool(toolName, arguments);
            return ResponseEntity.ok(Map.of(
                    "jsonrpc", "2.0",
                    "id", reqId,
                    "result", result
            ));
        }

        return ResponseEntity.ok(Map.of(
                "jsonrpc", "2.0",
                "id", reqId,
                "error", Map.of("code", -32601, "message", "Method not found: " + method)
        ));
    }

    private List<Map<String, Object>> getAvailableTools() {
        return List.of(
                Map.of(
                        "name", "check_eligibility",
                        "description", "Check all applicable Indian visa routes (e-Visa, VoA, Visa-Free, Regular) for a nationality and purpose.",
                        "inputSchema", Map.of(
                                "type", "object",
                                "properties", Map.of(
                                        "nationality", Map.of("type", "string", "description", "3-letter ISO code e.g. JPN, USA, GBR"),
                                        "purpose", Map.of("type", "string", "description", "TOURISM, BUSINESS, MEDICAL, CONFERENCE")
                                ),
                                "required", List.of("nationality", "purpose")
                        )
                ),
                Map.of(
                        "name", "get_category_info",
                        "description", "Retrieve official fees, processing times, validity, and document checklists for an e-Visa category.",
                        "inputSchema", Map.of(
                                "type", "object",
                                "properties", Map.of("slug", Map.of("type", "string", "description", "Category slug e.g. e-tourist-30-days, e-business-visa")),
                                "required", List.of("slug")
                        )
                ),
                Map.of(
                        "name", "list_entry_points",
                        "description", "List official designated airports, seaports, and land border checkpoints.",
                        "inputSchema", Map.of(
                                "type", "object",
                                "properties", Map.of("type", Map.of("type", "string", "description", "AIRPORT, SEAPORT, or LAND_ICP"))
                        )
                ),
                Map.of(
                        "name", "prepare_draft",
                        "description", "Prepare a pre-filled draft application for a traveler (Safe mode: Human must finalize and pay in UI).",
                        "inputSchema", Map.of(
                                "type", "object",
                                "properties", Map.of(
                                        "nationality", Map.of("type", "string"),
                                        "categoryName", Map.of("type", "string"),
                                        "applicantName", Map.of("type", "string"),
                                        "applicantEmail", Map.of("type", "string")
                                ),
                                "required", List.of("nationality", "applicantName", "applicantEmail")
                        )
                )
        );
    }

    private Map<String, Object> executeTool(String toolName, Map<String, Object> args) {
        switch (toolName) {
            case "check_eligibility": {
                String nat = (String) args.getOrDefault("nationality", "USA");
                String purStr = (String) args.getOrDefault("purpose", "TOURISM");
                try {
                    VisaPurpose purpose = VisaPurpose.valueOf(purStr.toUpperCase());
                    List<VisaRule> rules = visaRuleRepository.findActiveRules(nat.toUpperCase(), purpose);
                    return Map.of("content", List.of(Map.of("type", "text", "text", "Found " + rules.size() + " route(s) for " + nat + " (" + purpose + ")", "routes", rules)));
                } catch (Exception e) {
                    return Map.of("content", List.of(Map.of("type", "text", "text", "Invalid purpose or nationality: " + e.getMessage())));
                }
            }
            case "get_category_info": {
                String slug = (String) args.getOrDefault("slug", "e-tourist-30-days");
                Optional<VisaCategoryDetail> cat = visaCategoryDetailRepository.findBySlug(slug);
                if (cat.isPresent()) {
                    return Map.of("content", List.of(Map.of("type", "text", "text", cat.get().getName() + " details retrieved.", "category", cat.get())));
                }
                return Map.of("content", List.of(Map.of("type", "text", "text", "Category not found for slug: " + slug)));
            }
            case "list_entry_points": {
                String type = (String) args.get("type");
                List<EntryPoint> pts = type != null ? entryPointRepository.findByTypeOrderByStateAscNameAsc(type.toUpperCase()) : entryPointRepository.findAll();
                return Map.of("content", List.of(Map.of("type", "text", "text", "Found " + pts.size() + " entry points.", "entryPoints", pts)));
            }
            case "prepare_draft": {
                String ref = "VS-" + LocalDate.now().getYear() + "-" + (1000 + new Random().nextInt(9000));
                String email = (String) args.get("applicantEmail");
                String name = (String) args.get("applicantName");
                String nat = (String) args.getOrDefault("nationality", "USA");
                String catName = (String) args.getOrDefault("categoryName", "e-Tourist Visa (30 Days)");

                DemoApplication draft = DemoApplication.builder()
                        .referenceId(ref)
                        .applicantEmail(email)
                        .applicantName(name)
                        .nationality(nat)
                        .categoryName(catName)
                        .status("DRAFT")
                        .amount(new BigDecimal("25.00"))
                        .arrivalDate(LocalDate.now().plusDays(30))
                        .portOfArrival("Delhi (DEL)")
                        .createdAt(Instant.now())
                        .updatedAt(Instant.now())
                        .build();
                demoApplicationRepository.save(draft);

                return Map.of(
                        "content", List.of(Map.of(
                                "type", "text",
                                "text", "Draft prepared successfully with Reference ID: " + ref + ". GUARD-RAIL NOTICE: Automated agents cannot submit payment. The traveler must review and confirm at https://visasetu.demo/existing-applications?ref=" + ref,
                                "referenceId", ref,
                                "resumeUrl", "https://visasetu.demo/existing-applications?ref=" + ref
                        ))
                );
            }
            default:
                return Map.of("content", List.of(Map.of("type", "text", "text", "Tool not found: " + toolName)));
        }
    }
}
