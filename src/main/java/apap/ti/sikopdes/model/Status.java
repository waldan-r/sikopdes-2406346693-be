package apap.ti.sikopdes.model;

import java.util.Set;

public final class Status {
  private Status() {}

  public static final String AKTIF = "Aktif",
      TIDAK_AKTIF = "Tidak Aktif",
      DIBEKUKAN = "Dibekukan",
      DITUTUP = "Ditutup";
  public static final String BELUM_DITAGIH = "Belum Ditagih",
      SUDAH_DITAGIH = "Sudah Ditagih",
      LUNAS = "Lunas",
      DIBATALKAN = "Dibatalkan";
  public static final String BELUM_LUNAS = "Belum Lunas", JATUH_TEMPO = "Jatuh Tempo";
  public static final Set<String> KATEGORI =
      Set.of(
          "Sembako", "Kebutuhan Rumah Tangga", "Kesehatan", "Pendidikan", "Elektronik", "Lainnya");
}
