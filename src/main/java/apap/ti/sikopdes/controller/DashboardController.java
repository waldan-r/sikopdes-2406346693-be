package apap.ti.sikopdes.controller;

import apap.ti.sikopdes.dto.ApiResponse;
import apap.ti.sikopdes.dto.Requests.*;
import apap.ti.sikopdes.service.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {
  private final ReportService service;

  public DashboardController(ReportService service) {
    this.service = service;
  }

  @GetMapping
  public ApiResponse<?> dashboard() {
    return ApiResponse.ok(service.dashboard());
  }
}
