package com.example.report.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.report.dao.ReportDao;
import com.example.report.dto.CreateReportRequest;
import com.example.report.dto.GetResourceUsageSummaryRequest;
import com.example.report.dto.GetTopConsumersRequest;
import com.example.report.exception.NotFoundException;
import com.example.report.exception.ValidationException;
import com.example.report.model.Report;

@ExtendWith(MockitoExtension.class)
class ReportServiceImplValidationTest {

    @Mock
    private ReportDao reportDao;

    @InjectMocks
    private ReportServiceImpl reportService;

    // ==========================================
    // createReport
    // ==========================================

    @Test
    void createReport_HappyPath_ReturnsReportId() {
        UUID expectedId = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now();
        CreateReportRequest request = new CreateReportRequest(
            "container-abc", "billing-service", 72.5, 512, now
        );
        Report saved = new Report(expectedId, "container-abc", "billing-service", 72.5, 512, now);
        when(reportDao.save(any(Report.class))).thenReturn(saved);

        var response = reportService.createReport(request);

        assertEquals(expectedId.toString(), response.report_id());
        verify(reportDao, times(1)).save(any(Report.class));
    }

    @Test
    void createReport_NullTimestamp_ThrowsValidationException() {
        CreateReportRequest request = new CreateReportRequest(
            "container-abc", "billing-service", 72.5, 512, null
        );

        ValidationException ex = assertThrows(ValidationException.class,
            () -> reportService.createReport(request));

        assertEquals("Timestamp is required", ex.getMessage());
        verify(reportDao, never()).save(any(Report.class));
    }

    @Test
    void createReport_FutureTimestamp_ThrowsValidationException() {
        CreateReportRequest request = new CreateReportRequest(
            "container-abc", "billing-service", 72.5, 512, OffsetDateTime.now().plusSeconds(5)
        );

        ValidationException ex = assertThrows(ValidationException.class,
            () -> reportService.createReport(request));

        assertEquals("Timestamp cannot be in the future", ex.getMessage());
        verify(reportDao, never()).save(any(Report.class));
    }

    @Test
    void createReport_ExactlyNow_DoesNotThrow() {
        OffsetDateTime now = OffsetDateTime.now();
        CreateReportRequest request = new CreateReportRequest(
            "container-abc", "billing-service", 72.5, 512, now
        );
        when(reportDao.save(any(Report.class)))
            .thenReturn(new Report(UUID.randomUUID(), "container-abc", "billing-service", 72.5, 512, now));

        assertDoesNotThrow(() -> reportService.createReport(request));
    }

    // ==========================================
    // getReport
    // ==========================================

    @Test
    void getReport_HappyPath_ReturnsAllFields() {
        UUID id = UUID.randomUUID();
        OffsetDateTime reportedAt = OffsetDateTime.now().minusHours(1);
        Report report = new Report(id, "container-abc", "billing-service", 72.5, 512, reportedAt);
        when(reportDao.findReportById(id)).thenReturn(Optional.of(report));

        var response = reportService.getReport(id);

        assertEquals(id, response.report_id());
        assertEquals("container-abc", response.container_id());
        assertEquals("billing-service", response.app_name());
        assertEquals(72.5, response.cpu_usage_percent());
        assertEquals(512, response.memory_usage_mb());
        assertEquals(reportedAt, response.reported_at());
    }

    @Test
    void getReport_NotFound_ThrowsNotFoundException() {
        UUID id = UUID.randomUUID();
        when(reportDao.findReportById(id)).thenReturn(Optional.empty());

        NotFoundException ex = assertThrows(NotFoundException.class,
            () -> reportService.getReport(id));

        assertEquals("Report not found with ID: " + id, ex.getMessage());
    }

    // ==========================================
    // getResourceUsageSummary
    // ==========================================

    @Test
    void getSummary_SingleReport_ReturnsAllFields() {
        String containerId = "container-abc";
        String appName = "billing-service";
        OffsetDateTime start = OffsetDateTime.now().minusDays(1);
        OffsetDateTime end = OffsetDateTime.now();
        GetResourceUsageSummaryRequest request = new GetResourceUsageSummaryRequest(
            containerId, appName, start, end
        );
        Report report = new Report(UUID.randomUUID(), containerId, appName, 60.0, 256,
            OffsetDateTime.now().minusHours(1));
        when(reportDao.findReportByContainerIdAndAppNameAndReportedAtBetween(
            eq(containerId), eq(appName), any(OffsetDateTime.class), any(OffsetDateTime.class)
        )).thenReturn(List.of(report));

        var response = reportService.getResourceUsageSummary(request);

        assertEquals(containerId, response.container_id());
        assertEquals(appName, response.app_name());
        assertEquals(1, response.total_reports());
        assertEquals(60.0, response.avg_cpu_percent(), 0.001);
        assertEquals(60.0, response.peak_cpu_percent(), 0.001);
        assertEquals(256, response.avg_memory_mb());
        assertEquals(256, response.peak_memory_mb());
    }

    @Test
    void getSummary_MultipleReports_CalculatesAvgAndPeakCorrectly() {
        String containerId = "container-abc";
        String appName = "billing-service";
        GetResourceUsageSummaryRequest request = new GetResourceUsageSummaryRequest(
            containerId, appName,
            OffsetDateTime.now().minusDays(1), OffsetDateTime.now()
        );
        Report r1 = new Report(UUID.randomUUID(), containerId, appName, 60.0, 256,
            OffsetDateTime.now().minusHours(2));
        Report r2 = new Report(UUID.randomUUID(), containerId, appName, 80.0, 512,
            OffsetDateTime.now().minusHours(1));
        when(reportDao.findReportByContainerIdAndAppNameAndReportedAtBetween(
            eq(containerId), eq(appName), any(OffsetDateTime.class), any(OffsetDateTime.class)
        )).thenReturn(List.of(r1, r2));

        var response = reportService.getResourceUsageSummary(request);

        assertEquals(2, response.total_reports());
        assertEquals(70.0, response.avg_cpu_percent(), 0.001);
        assertEquals(80.0, response.peak_cpu_percent(), 0.001);
        assertEquals(384, response.avg_memory_mb());
        assertEquals(512, response.peak_memory_mb());
    }

    @Test
    void getSummary_NoReports_ReturnsZeros() {
        GetResourceUsageSummaryRequest request = new GetResourceUsageSummaryRequest(
            "container-abc", "billing-service",
            OffsetDateTime.now().minusDays(1), OffsetDateTime.now()
        );
        when(reportDao.findReportByContainerIdAndAppNameAndReportedAtBetween(
            any(), any(), any(OffsetDateTime.class), any(OffsetDateTime.class)
        )).thenReturn(List.of());

        var response = reportService.getResourceUsageSummary(request);

        assertEquals(0, response.total_reports());
        assertEquals(0.0, response.avg_cpu_percent(), 0.001);
        assertEquals(0.0, response.peak_cpu_percent(), 0.001);
        assertEquals(0, response.avg_memory_mb());
        assertEquals(0, response.peak_memory_mb());
    }

    @Test
    void getSummary_ZeroCpu_HandlesAveragesCorrectly() {
        GetResourceUsageSummaryRequest request = new GetResourceUsageSummaryRequest(
            "container-abc", "billing-service",
            OffsetDateTime.now().minusDays(1), OffsetDateTime.now()
        );
        Report idleReport = new Report(UUID.randomUUID(), "container-abc", "billing-service",
            0.0, 0, OffsetDateTime.now().minusHours(1));
        when(reportDao.findReportByContainerIdAndAppNameAndReportedAtBetween(
            any(), any(), any(OffsetDateTime.class), any(OffsetDateTime.class)
        )).thenReturn(List.of(idleReport));

        var response = reportService.getResourceUsageSummary(request);

        assertEquals(1, response.total_reports());
        assertEquals(0.0, response.avg_cpu_percent(), 0.001);
        assertEquals(0.0, response.peak_cpu_percent(), 0.001);
        assertEquals(0, response.avg_memory_mb());
        assertEquals(0, response.peak_memory_mb());
    }

    @Test
    void getSummary_StartAfterEnd_ThrowsValidationException() {
        GetResourceUsageSummaryRequest request = new GetResourceUsageSummaryRequest(
            "container-abc", "billing-service",
            OffsetDateTime.now(), OffsetDateTime.now().minusDays(1)
        );

        ValidationException ex = assertThrows(ValidationException.class,
            () -> reportService.getResourceUsageSummary(request));

        assertEquals("Start timestamp cannot be after end timestamp", ex.getMessage());
        verify(reportDao, never()).findReportByContainerIdAndAppNameAndReportedAtBetween(
            any(), any(), any(), any());
    }

    // ==========================================
    // getTopConsumers
    // ==========================================

    @Test
    void getTopConsumers_EmptyReports_ReturnsEmptyResults() {
        when(reportDao.findAll()).thenReturn(List.of());

        var response = reportService.getTopConsumers(new GetTopConsumersRequest(10));

        assertNotNull(response.results());
        assertEquals(0, response.results().size());
    }

    @Test
    void getTopConsumers_MultipleContainers_SortedByAvgCpuDesc() {
        OffsetDateTime now = OffsetDateTime.now().minusHours(1);
        List<Report> reports = List.of(
            new Report(UUID.randomUUID(), "worker-1", "app", 30.0, 256, now),
            new Report(UUID.randomUUID(), "worker-2", "app", 90.0, 256, now),
            new Report(UUID.randomUUID(), "worker-3", "app", 60.0, 256, now)
        );
        when(reportDao.findAll()).thenReturn(reports);

        var response = reportService.getTopConsumers(new GetTopConsumersRequest(3));

        assertEquals(3, response.results().size());
        assertEquals("worker-2", response.results().get(0).container_id());
        assertEquals(90.0, response.results().get(0).avg_cpu_percent(), 0.001);
        assertEquals("worker-3", response.results().get(1).container_id());
        assertEquals(60.0, response.results().get(1).avg_cpu_percent(), 0.001);
        assertEquals("worker-1", response.results().get(2).container_id());
        assertEquals(30.0, response.results().get(2).avg_cpu_percent(), 0.001);
    }

    @Test
    void getTopConsumers_MultipleReportsSameContainer_AvgCpuIsCorrect() {
        OffsetDateTime now = OffsetDateTime.now().minusHours(1);
        List<Report> reports = List.of(
            new Report(UUID.randomUUID(), "web-1", "app", 60.0, 256, now),
            new Report(UUID.randomUUID(), "web-1", "app", 80.0, 512, now),
            new Report(UUID.randomUUID(), "web-1", "app", 100.0, 1024, now)
        );
        when(reportDao.findAll()).thenReturn(reports);

        var response = reportService.getTopConsumers(new GetTopConsumersRequest(10));

        assertEquals(1, response.results().size());
        assertEquals("web-1", response.results().get(0).container_id());
        assertEquals(80.0, response.results().get(0).avg_cpu_percent(), 0.001);
    }

    @Test
    void getTopConsumers_LimitCapsResults() {
        OffsetDateTime now = OffsetDateTime.now().minusHours(1);
        List<Report> reports = List.of(
            new Report(UUID.randomUUID(), "worker-1", "app", 30.0, 256, now),
            new Report(UUID.randomUUID(), "worker-2", "app", 90.0, 256, now),
            new Report(UUID.randomUUID(), "worker-3", "app", 60.0, 256, now)
        );
        when(reportDao.findAll()).thenReturn(reports);

        var response = reportService.getTopConsumers(new GetTopConsumersRequest(2));

        assertEquals(2, response.results().size());
        assertEquals("worker-2", response.results().get(0).container_id());
        assertEquals("worker-3", response.results().get(1).container_id());
    }
}
