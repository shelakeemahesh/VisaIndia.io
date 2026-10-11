package com.visasetu.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.visasetu.repository.DemoApplicationRepository;
import com.visasetu.repository.EntryPointRepository;
import com.visasetu.repository.VisaCategoryDetailRepository;
import com.visasetu.repository.VisaRuleRepository;
import com.visasetu.service.RateLimitingService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(McpServerController.class)
@AutoConfigureMockMvc(addFilters = false)
class McpServerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private VisaRuleRepository visaRuleRepository;

    @MockBean
    private VisaCategoryDetailRepository visaCategoryDetailRepository;

    @MockBean
    private EntryPointRepository entryPointRepository;

    @MockBean
    private DemoApplicationRepository demoApplicationRepository;

    @MockBean
    private RateLimitingService rateLimitingService;

    @MockBean
    private com.visasetu.service.JwtTokenService jwtTokenService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("GET /api/v1/mcp returns manifest with safe mode guardrails")
    void getMcpManifest_returnsManifest() throws Exception {
        mockMvc.perform(get("/api/v1/mcp"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.protocolVersion").value("2024-11-05"))
                .andExpect(jsonPath("$.guardrails.safeMode").value(true))
                .andExpect(jsonPath("$.guardrails.paymentAutonomousExecution").value(false));
    }

    @Test
    @DisplayName("POST /api/v1/mcp handles initialize method cleanly")
    void handleMcp_initialize_succeeds() throws Exception {
        Map<String, Object> req = Map.of(
                "jsonrpc", "2.0",
                "method", "initialize",
                "id", "client-req-1"
        );

        mockMvc.perform(post("/api/v1/mcp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jsonrpc").value("2.0"))
                .andExpect(jsonPath("$.id").value("client-req-1"))
                .andExpect(jsonPath("$.result.protocolVersion").value("2024-11-05"));
    }

    @Test
    @DisplayName("POST /api/v1/mcp handles null id safely without NullPointerException")
    void handleMcp_nullId_handledSafely() throws Exception {
        String jsonWithoutId = "{\"jsonrpc\":\"2.0\",\"method\":\"tools/list\"}";

        mockMvc.perform(post("/api/v1/mcp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonWithoutId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jsonrpc").value("2.0"))
                .andExpect(jsonPath("$.result.tools").isArray());
    }

    @Test
    @DisplayName("POST /api/v1/mcp handles missing tool name gracefully")
    void handleMcp_missingToolName_returnsError() throws Exception {
        Map<String, Object> req = Map.of(
                "jsonrpc", "2.0",
                "method", "tools/call",
                "params", Map.of()
        );

        mockMvc.perform(post("/api/v1/mcp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.error.code").value(-32602));
    }
}
