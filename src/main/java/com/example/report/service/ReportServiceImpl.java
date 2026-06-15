package com.example.report.service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.example.report.dao.ReportDao;
import com.example.report.dto.CreateReportRequest;
import com.example.report.dto.CreateReportResponse;
import com.example.report.dto.GetReportResponse;
import com.example.report.dto.GetResourceUsageSummaryRequest;
import com.example.report.dto.GetResourceUsageSummaryResponse;
import com.example.report.dto.GetTopConsumersRequest;
import com.example.report.dto.GetTopConsumersResponse;
import com.example.report.exception.ValidationException;
import com.example.report.model.Report;


@Service
public class ReportServiceImpl implements ReportService{
    
    private static final Logger log = LoggerFactory.getLogger(ReportServiceImpl.class);

    private final ReportDao reportDao;

    public ReportServiceImpl(ReportDao reportDao) {
        this.reportDao = reportDao;
    }

    /*
  "container_id": "web-1",
  "app_name": "billing-service",
  "cpu_usage_percent": 72.5,
  "memory_usage_mb": 512,
  "reported_at": "2026-06-15T10:00:00Z"
} */ 
    @Override
    public CreateReportResponse createReport(CreateReportRequest request) {
        // TODO Auto-generated method stub
        log.info("Creating report for request: {}", request);
        validateTimestamp(request.reportedAt());
        Report report = new Report(UUID.randomUUID(), request.containerId(), request.appName(), request.cpuUsagePercent(), request.memoryUsageMb(), request.reportedAt());
        Report savedReport = reportDao.save(report);
        log.atInfo().log("Report created with ID: {}", savedReport.getId());
        return new CreateReportResponse(savedReport.getId().toString());
    }

    @Override
    public GetReportResponse getReport(UUID reportId) {
        // TODO Auto-generated method stub
        log.atInfo().log("Retrieving report with ID: {}", reportId);
        Optional<Report> report = reportDao.findReportById(reportId);
        if (report.isPresent()) {
            return new GetReportResponse(
                report.get().getId(),
                report.get().getContainerId(),
                report.get().getAppName(),
                report.get().getCpuUsagePercent(),
                report.get().getMemoryUsageMb(),
                report.get().getReportedAt()
            );
        } else {
            log.atError().log("Report not found with ID: {}", reportId);
            throw new ValidationException("Report not found with ID: " + reportId);
        }
    }

    @Override
    public GetResourceUsageSummaryResponse getResourceUsageSummary(GetResourceUsageSummaryRequest request) {
        // TODO Auto-generated method stub
        /*    String app_name,
    Integer total_reports,
    Double avg_cpu_percent,
    Double peak_cpu_percent,
    Integer avg_memory_mb,
    Integer peak_memory_mb */
        log.atInfo().log("Retrieving resource usage summary for request: {}", request);
        List<Report> reports = reportDao.findReportByContainerIdAndAppNameAndReportedAtBetween(request.containerId(), request.appName(), request.start(), request.end());
        log.atInfo().log("Found {} reports for container: {}", reports.size(), request.containerId());
        // Implementation for calculating resource usage summary
        return new GetResourceUsageSummaryResponse(
            request.containerId(),
            request.appName(),
            reports.size(),
            reports.stream().mapToDouble(Report::getCpuUsagePercent).average().orElse(0.0),
            reports.stream().mapToDouble(Report::getCpuUsagePercent).max().orElse(0.0),
            (int) Math.round(reports.stream().mapToInt(Report::getMemoryUsageMb).average().orElse(0.0)),
            reports.stream().mapToInt(Report::getMemoryUsageMb).max().orElse(0)
        );
    }

    @Override
    public GetTopConsumersResponse getTopConsumers(GetTopConsumersRequest request) {
        // TODO Auto-generated method stub
        log.atInfo().log("Retrieving top consumers for request: {}", request);
        List<Report> reports = reportDao.findAll();
        log.atInfo().log("Found {} reports", reports.size());
        /*
        {
  "results": [
    { "container_id": "worker-3", "avg_cpu_percent": 91.2 },
    { "container_id": "web-1",    "avg_cpu_percent": 72.5 }
  ]
} */
        return new GetTopConsumersResponse(
            List.of()
        );


    }

    private void validateTimestamp(LocalDateTime timestamp) {
        if (timestamp == null) {
            throw new ValidationException("Timestamp is required");
        }
        if (timestamp.isAfter(LocalDateTime.now())) {
            throw new ValidationException("Timestamp cannot be in the future");
        }
    }



}