package com.example.report.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.example.report.dto.CreateReportResponse;
import com.example.report.dto.GetReportResponse;
import com.example.report.dto.GetResourceUsageSummaryResponse;
import com.example.report.dto.GetTopConsumersResponse;
import com.example.report.exception.GlobalExceptionHandler;
import com.example.report.exception.NotFoundException;
import com.example.report.exception.ValidationException;
import com.example.report.service.ReportService;

@ExtendWith(MockitoExtension.class)
class ContainerReportControllerTest {

    @Mock
    private ReportService reportService;

    @InjectMocks
    private ContainerReportController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    // ==========================================
    // POST /api/v1/reports
    // ==========================================

    @Test
    void createReport_ValidRequest_Returns201() throws Exception {
        when(reportService.createReport(any())).thenReturn(new CreateReportResponse("abc-uuid"));

        mockMvc.perform(post("/api/v1/reports")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "container_id": "web-1",
                            "app_name": "billing-service",
                            "cpu_usage_percent": 72.5,
                            "memory_usage_mb": 512,
                            "reported_at": "2026-06-14T10:00:00Z"
                        }
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.report_id").value("abc-uuid"));
    }

    @Test
    void createReport_BlankContainerId_Returns400() throws Exception {
        mockMvc.perform(post("/api/v1/reports")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "container_id": "",
                            "app_name": "billing-service",
                            "cpu_usage_percent": 72.5,
                            "memory_usage_mb": 512,
                            "reported_at": "2026-06-14T10:00:00Z"
                        }
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void createReport_CpuExceeds100_Returns400() throws Exception {
        mockMvc.perform(post("/api/v1/reports")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "container_id": "web-1",
                            "app_name": "billing-service",
                            "cpu_usage_percent": 150.0,
                            "memory_usage_mb": 512,
                            "reported_at": "2026-06-14T10:00:00Z"
                        }
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void createReport_NegativeMemory_Returns400() throws Exception {
        mockMvc.perform(post("/api/v1/reports")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "container_id": "web-1",
                            "app_name": "billing-service",
                            "cpu_usage_percent": 72.5,
                            "memory_usage_mb": -1,
                            "reported_at": "2026-06-14T10:00:00Z"
                        }
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    // GlobalExceptionHandler: ValidationException → 400
    @Test
    void createReport_ServiceThrowsValidation_Returns400WithMessage() throws Exception {
        when(reportService.createReport(any()))
                .thenThrow(new ValidationException("Timestamp cannot be in the future"));

        mockMvc.perform(post("/api/v1/reports")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "container_id": "web-1",
                            "app_name": "billing-service",
                            "cpu_usage_percent": 72.5,
                            "memory_usage_mb": 512,
                            "reported_at": "2026-06-14T10:00:00Z"
                        }
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Timestamp cannot be in the future"));
    }

    // ==========================================
    // GET /api/v1/reports/{report_id}
    // ==========================================

    @Test
    void getReport_ValidId_Returns200WithAllFields() throws Exception {
        UUID id = UUID.randomUUID();
        OffsetDateTime reportedAt = OffsetDateTime.parse("2026-06-14T10:00:00Z");
        when(reportService.getReport(id)).thenReturn(new GetReportResponse(
                id, "web-1", "billing-service", 72.5, 512, reportedAt));

        mockMvc.perform(get("/api/v1/reports/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.report_id").value(id.toString()))
                .andExpect(jsonPath("$.container_id").value("web-1"))
                .andExpect(jsonPath("$.app_name").value("billing-service"))
                .andExpect(jsonPath("$.cpu_usage_percent").value(72.5))
                .andExpect(jsonPath("$.memory_usage_mb").value(512))
                .andExpect(jsonPath("$.reported_at").exists());
    }

    // GlobalExceptionHandler: NotFoundException → 404
    @Test
    void getReport_NotFound_Returns404WithMessage() throws Exception {
        UUID id = UUID.randomUUID();
        when(reportService.getReport(id))
                .thenThrow(new NotFoundException("Report not found with ID: " + id));

        mockMvc.perform(get("/api/v1/reports/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Report not found with ID: " + id));
    }

    // ==========================================
    // GET /api/v1/reports/summary
    // ==========================================

    @Test
    void getSummary_ValidParams_Returns200WithAllFields() throws Exception {
        when(reportService.getResourceUsageSummary(any()))
                .thenReturn(new GetResourceUsageSummaryResponse(
                        "web-1", "billing-service", 5, 70.0, 90.0, 400, 512));

        mockMvc.perform(get("/api/v1/reports/summary")
                .param("container_id", "web-1")
                .param("app_name", "billing-service")
                .param("start", "2026-06-14T00:00:00Z")
                .param("end", "2026-06-15T00:00:00Z"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.container_id").value("web-1"))
                .andExpect(jsonPath("$.app_name").value("billing-service"))
                .andExpect(jsonPath("$.total_reports").value(5))
                .andExpect(jsonPath("$.avg_cpu_percent").value(70.0))
                .andExpect(jsonPath("$.peak_cpu_percent").value(90.0))
                .andExpect(jsonPath("$.avg_memory_mb").value(400))
                .andExpect(jsonPath("$.peak_memory_mb").value(512));
    }

    // GlobalExceptionHandler: BindException → 400 (missing @NotBlank param on @ModelAttribute)
    @Test
    void getSummary_MissingContainerId_Returns400() throws Exception {
        mockMvc.perform(get("/api/v1/reports/summary")
                .param("app_name", "billing-service")
                .param("start", "2026-06-14T00:00:00Z")
                .param("end", "2026-06-15T00:00:00Z"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    // ==========================================
    // GET /api/v1/reports/top-consumers
    // ==========================================

    @Test
    void getTopConsumers_ValidLimit_Returns200WithResultsArray() throws Exception {
        when(reportService.getTopConsumers(any()))
                .thenReturn(new GetTopConsumersResponse(List.of()));

        mockMvc.perform(get("/api/v1/reports/top-consumers")
                .param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results").isArray());
    }

    @Test
    void getTopConsumers_LimitExceeds100_Returns400() throws Exception {
        mockMvc.perform(get("/api/v1/reports/top-consumers")
                .param("limit", "200"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }
}
