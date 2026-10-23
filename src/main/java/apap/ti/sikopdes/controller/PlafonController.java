package apap.ti.sikopdes.controller;

import apap.ti.sikopdes.dto.ApiResponse;
import apap.ti.sikopdes.dto.Requests.*;
import apap.ti.sikopdes.service.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/plafon")
public class PlafonController {
  private final PlafonService service;

  public PlafonController(PlafonService service) {
    this.service = service;
  }

  @GetMapping
  public ApiResponse<?> list() {
    return ApiResponse.ok(service.list());
  }

  @GetMapping("/{id}")
  public ApiResponse<?> detail(@PathVariable String id) {
    return ApiResponse.ok(service.detail(id));
  }

  @PostMapping("/create")
  @ResponseStatus(HttpStatus.CREATED)
  public ApiResponse<?> create(@Valid @RequestBody CreatePlafon body) {
    return ApiResponse.of(201, "Plafon berhasil dibuat.", service.create(body));
  }

  @PutMapping("/update")
  public ApiResponse<?> update(@Valid @RequestBody UpdatePlafon body) {
    return ApiResponse.ok(service.update(body));
  }

  @DeleteMapping("/{id}/close")
  public ApiResponse<?> close(@PathVariable String id) {
    return ApiResponse.ok(service.close(id));
  }
}
