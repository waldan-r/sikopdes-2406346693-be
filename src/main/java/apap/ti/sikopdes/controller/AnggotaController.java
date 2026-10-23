package apap.ti.sikopdes.controller;

import apap.ti.sikopdes.dto.ApiResponse;
import apap.ti.sikopdes.dto.Requests.*;
import apap.ti.sikopdes.service.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/anggota")
public class AnggotaController {
  private final AnggotaService service;

  public AnggotaController(AnggotaService service) {
    this.service = service;
  }

  @GetMapping
  public ApiResponse<?> list() {
    return ApiResponse.ok(service.list());
  }
}
