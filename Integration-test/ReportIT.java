import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;
import java.util.Random;


public class ReportIT {
    static final String BASE_URL = "https://report-app-88qqj.ondigitalocean.app";
    static final String BASE_PATH = "/api/v1/reports";
    static final HttpClient client = HttpClient.newHttpClient();

    private static String container_id = "container-1234";
    private static String app_name = "billing-service";

     public static void main(String[] args) throws Exception {
        String reportId = createReport_201();
        System.out.println("Report ID: " + reportId);
        getReport(reportId);
        System.out.println("\n==> All tests passed!");
     }

     /*
     {
{
  "container_id": "web-1",
  "app_name": "billing-service",
  "cpu_usage_percent": 72.5,
  "memory_usage_mb": 512,
  "reported_at": "2026-06-15T10:00:00Z"
}
}
   */
     private static String createReport_201() throws Exception {
        System.out.println("==> POST " + BASE_PATH);
        String body = """
            {
                "container_id": "%s",
                "app_name": "%s",
                "cpu_usage_percent": 0.725,
                "memory_usage_mb": 512,
                "reported_at": "2026-06-14T10:00:00Z"
            }
            """.formatted(container_id, app_name);

        var request = HttpRequest.newBuilder()
            .uri(URI.create(BASE_URL + BASE_PATH))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build();
        var response = client.send(request, HttpResponse.BodyHandlers.ofString());
        System.out.println("Status: " + response.statusCode() + "  Body: " + response.body());
        assert response.statusCode() == 201 : "Expected 201, got " + response.statusCode();

        String r = response.body();
        int start = r.indexOf("\"report_id\":\"") + 13;
        return r.substring(start, r.indexOf("\"", start));
        // return response.body();  

     }

     private static void getReport(String reportId) throws Exception {
        String url = BASE_URL + BASE_PATH + "/" + reportId;
        System.out.println("==> GET " + url);


        var request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Content-Type", "application/json")
            .GET()
            .build();
        var response = client.send(request, HttpResponse.BodyHandlers.ofString());
        System.out.println("Status: " + response.statusCode() + "  Body: " + response.body());
        assert response.statusCode() == 200 : "Expected 200, got " + response.statusCode();
     }
}
