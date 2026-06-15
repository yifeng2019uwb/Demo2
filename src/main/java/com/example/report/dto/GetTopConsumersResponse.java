package com.example.report.dto;

import java.util.List;

/*
{
  "results": [
    { "container_id": "worker-3", "avg_cpu_percent": 91.2 },
    { "container_id": "web-1",    "cpu_percent": 72.5 }
  ]
}
   */
public record GetTopConsumersResponse (
    List<ContainerUsage> results
) {
    public record ContainerUsage(
        String container_id ,
        Double avg_cpu_percent
    ) {
        public ContainerUsage(){
            this("", 0.0);
        }
    }

    public GetTopConsumersResponse(){
        this(List.of());
    }
}
