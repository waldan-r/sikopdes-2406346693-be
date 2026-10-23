package apap.ti.sikopdes.dto;

import java.time.*;

public record ApiResponse<T>(int status, String message, OffsetDateTime timestamp, T data) {
  public static <T> ApiResponse<T> of(int status, String message, T data) {
    return new ApiResponse<>(status, message, OffsetDateTime.now(ZoneId.of("Asia/Jakarta")), data);
  }

  public static <T> ApiResponse<T> ok(T data) {
    return of(200, "Success", data);
  }
}
