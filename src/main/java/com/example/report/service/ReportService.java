package com.example.report.service;

import java.util.UUID;

import com.example.report.dto.CreateReportRequest;
import com.example.report.dto.CreateReportResponse;
import com.example.report.dto.GetReportResponse;
import com.example.report.dto.GetResourceUsageSummaryRequest;
import com.example.report.dto.GetResourceUsageSummaryResponse;
import com.example.report.dto.GetTopConsumersRequest;
import com.example.report.dto.GetTopConsumersResponse;

public interface ReportService {
    
    public CreateReportResponse createReport(CreateReportRequest request);

    public GetReportResponse getReport(UUID reportId);

    public GetResourceUsageSummaryResponse getResourceUsageSummary(GetResourceUsageSummaryRequest request);

    public GetTopConsumersResponse getTopConsumers(GetTopConsumersRequest request);

}
