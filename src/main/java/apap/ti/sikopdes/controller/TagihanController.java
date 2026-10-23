package apap.ti.sikopdes.controller;

import apap.ti.sikopdes.dto.ApiResponse;
import apap.ti.sikopdes.dto.Requests.*;
import apap.ti.sikopdes.service.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tagihan")
public class TagihanController {
  private final TagihanService service;
  private final ReportService report;

  public TagihanController(TagihanService service, ReportService report) {
    this.service = service;
    this.report = report;
  }

  @GetMapping
  public ApiResponse<?> list() {
    return ApiResponse.ok(service.list(false));
  }

  @GetMapping("/jatuh-tempo")
  public ApiResponse<?> overdue() {
    return ApiResponse.ok(service.list(true));
  }

  @GetMapping("/statistik")
  public ApiResponse<?> statistics(
      @RequestParam(defaultValue = "bulanan") String period, @RequestParam int year) {
    return ApiResponse.ok(report.statistik(period, year));
  }

  @GetMapping("/{id}")
  public ApiResponse<?> detail(@PathVariable String id) {
    return ApiResponse.ok(service.detail(id));
  }

  @PostMapping("/create")
  @ResponseStatus(HttpStatus.CREATED)
  public ApiResponse<?> create(@Valid @RequestBody CreateTagihan body) {
    return ApiResponse.of(201, "Tagihan berhasil diterbitkan.", service.create(body));
  }

  @PutMapping("/update-klaim")
  public ApiResponse<?> update(@Valid @RequestBody UpdateTagihan body) {
    return ApiResponse.ok(service.updateClaims(body));
  }

  @PutMapping("/bayar")
  public ApiResponse<?> pay(@Valid @RequestBody Pembayaran body) {
    return ApiResponse.ok(service.pay(body));
  }

  @DeleteMapping("/{id}/delete")
  public ApiResponse<?> cancel(@PathVariable String id) {
    return ApiResponse.ok(service.cancel(id));
  }
}
