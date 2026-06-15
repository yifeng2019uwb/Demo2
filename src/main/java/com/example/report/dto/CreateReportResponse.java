package com.example.report.dto;


/*
Response `201`:
```json
{ "report_id": "uuid" }
```
 */
public record CreateReportResponse (
    String report_id
) {
    public CreateReportResponse(){
        this("uuid");
    }
}
