package apap.ti.sikopdes.exception;

import org.springframework.http.HttpStatus;

public class BusinessException extends RuntimeException {
  private final HttpStatus status;

  public BusinessException(String message) {
    this(HttpStatus.BAD_REQUEST, message);
  }

  public BusinessException(HttpStatus status, String message) {
    super(message);
    this.status = status;
  }

  public HttpStatus getStatus() {
    return status;
  }

  public static BusinessException notFound(String type, String id) {
    return new BusinessException(HttpStatus.NOT_FOUND, type + " " + id + " tidak ditemukan.");
  }

  public static void require(boolean condition, String message) {
    if (!condition) throw new BusinessException(message);
  }
}
