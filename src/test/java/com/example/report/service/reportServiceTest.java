package com.example.report.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.report.dao.ReportDao;
import com.example.report.dto.CreateReportRequest;
import com.example.report.dto.GetResourceUsageSummaryRequest;
import com.example.report.exception.ValidationException;
import com.example.report.model.Report;

@ExtendWith(MockitoExtension.class)
class ReportServiceImplValidationTest {

    @Mock
    private ReportDao reportDao;

    @InjectMocks
    private ReportServiceImpl reportService;

    // ==========================================
    // 1. TIMESTAMP VALIDATION TESTS (Business Logic)
    // ==========================================

    @Test
    void validateTimestamp_ExactlyNow_ShouldPass() {
        // Boundary check: LocalDateTime.now() should be valid
        LocalDateTime now = LocalDateTime.now();
        CreateReportRequest request = new CreateReportRequest(
            "container-12345", "billing-service", 0.5, 256, now
        );
        
        Report mockSavedReport = new Report(UUID.randomUUID(), "container-12345", "billing-service", 0.5, 256, now);
        when(reportDao.save(any(Report.class))).thenReturn(mockSavedReport);

        assertDoesNotThrow(() -> reportService.createReport(request));
    }

    @Test
    void validateTimestamp_Null_ThrowsValidationException() {
        CreateReportRequest request = new CreateReportRequest(
            "container-12345", "billing-service", 0.5, 256, null
        );

        ValidationException exception = assertThrows(ValidationException.class, () -> {
            reportService.createReport(request);
        });

        assertEquals("Timestamp is required", exception.getMessage());
        verify(reportDao, never()).save(any(Report.class));
    }

    @Test
    void validateTimestamp_FutureDate_ThrowsValidationException() {
        // Boundary check: 1 second into the future should fail
        LocalDateTime future = LocalDateTime.now().plusSeconds(5);
        CreateReportRequest request = new CreateReportRequest(
            "container-12345", "billing-service", 0.5, 256, future
        );

        ValidationException exception = assertThrows(ValidationException.class, () -> {
            reportService.createReport(request);
        });

        assertEquals("Timestamp cannot be in the future", exception.getMessage());
        verify(reportDao, never()).save(any(Report.class));
    }

    // ==========================================
    // 2. DATA MISSING / NOT FOUND VALIDATION TESTS
    // ==========================================

    @Test
    void getReport_WithInvalidOrMissingId_ThrowsValidationException() {
        UUID nonExistentId = UUID.randomUUID();
        when(reportDao.findReportById(nonExistentId)).thenReturn(java.util.Optional.empty());

        ValidationException exception = assertThrows(ValidationException.class, () -> {
            reportService.getReport(nonExistentId);
        });

        assertEquals("Report not found with ID: " + nonExistentId, exception.getMessage());
    }

@Test
void getResourceUsageSummary_WhenCpuUsageIsExactlyZero_HandlesAveragesCorrectly() {
    String containerId = "container-12345";
    String appName = "billing-service";
    GetResourceUsageSummaryRequest request = new GetResourceUsageSummaryRequest(
        containerId, appName, LocalDateTime.now().minusDays(1), LocalDateTime.now()
    );

    // FIX: Remove the UUID argument so the parameters align correctly with your constructor
    Report idleReport = new Report(
        UUID.randomUUID(),
        containerId, 
        appName, 
        0.0, 
        0, 
        LocalDateTime.now().minusHours(1)
    );
    
    when(reportDao.findReportByContainerIdAndAppNameAndReportedAtBetween(
        eq(containerId),      // Wrapper matcher for Container ID
        eq(appName),     // Wrapper matcher for App Name
        any(LocalDateTime.class),   // Matcher for Start time
    any(LocalDateTime.class)    // Matcher for End time
    )).thenReturn(java.util.List.of(idleReport));

    var response = reportService.getResourceUsageSummary(request);

    assertEquals(1, response.total_reports());
    assertEquals(0.0, response.avg_cpu_percent(), 0.001);
    assertEquals(0.0, response.peak_cpu_percent(), 0.001);
    assertEquals(0, response.avg_memory_mb());
    assertEquals(0, response.peak_memory_mb());
}
}