package apap.ti.sikopdes.dto;

import jakarta.validation.constraints.*;
import java.time.*;
import java.util.List;

public final class Requests {
  private Requests() {}

  public record CreatePlafon(
      @NotBlank String anggotaId,
      @NotNull @Positive @DecimalMax("10000000") @Digits(integer = 8, fraction = 2)
          Double limitPlafon) {}

  public record UpdatePlafon(
      @NotBlank String id,
      @NotNull @Positive @DecimalMax("10000000") @Digits(integer = 8, fraction = 2)
          Double limitPlafon,
      @NotBlank String status) {}

  public record CreateKlaim(
      @NotBlank String plafonId,
      @NotNull LocalDateTime tanggalTransaksi,
      @NotNull @Positive @Digits(integer = 12, fraction = 2) Double nominalKlaim,
      @NotBlank String kategori,
      @NotBlank @Size(max = 1000) String keterangan) {}

  public record UpdateKlaim(
      @NotBlank String id,
      @NotNull LocalDateTime tanggalTransaksi,
      @NotNull @Positive @Digits(integer = 12, fraction = 2) Double nominalKlaim,
      @NotBlank String kategori,
      @NotBlank @Size(max = 1000) String keterangan) {}

  public record CreateTagihan(
      @NotBlank String anggotaId,
      @NotNull LocalDate tanggalJatuhTempo,
      @NotEmpty List<@NotBlank String> klaimIds) {}

  public record UpdateTagihan(@NotBlank String id, @NotEmpty List<@NotBlank String> klaimIds) {}

  public record Pembayaran(
      @NotBlank String id,
      @NotNull @Positive @Digits(integer = 12, fraction = 2) Double nominalPembayaran,
      @NotNull LocalDate tanggalBayar) {}
}
