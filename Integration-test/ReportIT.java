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
    static final String BASE_URL = "https://shark-app-kt3v6.ondigitalocean.app";
    static final String BASE_PATH = "/api/v1/events";
    static final HttpClient client = HttpClient.newHttpClient();
}
