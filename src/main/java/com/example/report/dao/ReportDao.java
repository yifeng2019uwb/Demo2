package com.example.report.dao;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.example.report.model.Report;

@Repository
public interface ReportDao extends JpaRepository<Report, UUID>{
    
    Optional<Report> findReportById(UUID id);

    List<Report> findReportByContainerIdAndAppNameAndReportedAtBetween(String containerId, String appName, LocalDateTime start, LocalDateTime end);

    List<Report> findReportsByContainerId(String containerId);
    
}
