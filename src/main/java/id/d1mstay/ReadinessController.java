package id.d1mstay;

import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ReadinessController {
    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("application", "D1MSTAY", "status", "ready");
    }
}
