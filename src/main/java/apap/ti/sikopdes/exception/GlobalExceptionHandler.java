package apap.ti.sikopdes.exception;

import apap.ti.sikopdes.dto.ApiResponse;
import org.slf4j.*;
import org.springframework.dao.*;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {
  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  @ExceptionHandler(BusinessException.class)
  public ResponseEntity<?> business(BusinessException e) {
    return error(e.getStatus(), e.getMessage());
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<?> validation(MethodArgumentNotValidException e) {
    var errors =
        e.getBindingResult().getFieldErrors().stream()
            .map(f -> f.getField() + ": " + f.getDefaultMessage())
            .toList();
    return ResponseEntity.badRequest().body(ApiResponse.of(400, "Data belum valid.", errors));
  }

  @ExceptionHandler({
    HttpMessageNotReadableException.class,
    MethodArgumentTypeMismatchException.class,
    MissingServletRequestParameterException.class
  })
  public ResponseEntity<?> format(Exception e) {
    return error(HttpStatus.BAD_REQUEST, "Format data atau parameter tidak sesuai.");
  }

  @ExceptionHandler({
    DataIntegrityViolationException.class,
    OptimisticLockingFailureException.class,
    PessimisticLockingFailureException.class
  })
  public ResponseEntity<?> conflict(Exception e) {
    return error(
        HttpStatus.CONFLICT,
        "Data berubah atau bertentangan dengan data lain. Muat ulang lalu coba kembali.");
  }

  @ExceptionHandler(NoResourceFoundException.class)
  public ResponseEntity<?> missing(Exception e) {
    return error(HttpStatus.NOT_FOUND, "Endpoint tidak ditemukan.");
  }

  @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
  public ResponseEntity<?> method(Exception e) {
    return error(HttpStatus.METHOD_NOT_ALLOWED, "HTTP method tidak didukung untuk endpoint ini.");
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<?> unexpected(Exception e) {
    log.error("Permintaan gagal diproses", e);
    return error(HttpStatus.INTERNAL_SERVER_ERROR, "Terjadi gangguan saat memproses data.");
  }

  private ResponseEntity<?> error(HttpStatus status, String message) {
    return ResponseEntity.status(status).body(ApiResponse.of(status.value(), message, null));
  }
}
