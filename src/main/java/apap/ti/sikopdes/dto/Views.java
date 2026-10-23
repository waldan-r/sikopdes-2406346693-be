package apap.ti.sikopdes.dto;

import java.time.LocalDateTime;
import java.util.List;

public final class Views {
  private Views() {}

  public record AnggotaView(
      String id,
      String nomorAnggota,
      String nama,
      String email,
      String telepon,
      String statusKeanggotaan) {}

  public record KlaimView(
      String id,
      String plafonId,
      String anggotaId,
      String namaAnggota,
      LocalDateTime tanggalTransaksi,
      double nominalKlaim,
      String kategori,
      String keterangan,
      String status,
      String tagihanId,
      LocalDateTime createdAt,
      LocalDateTime updatedAt) {}

  public record PlafonView(
      String id,
      String anggotaId,
      String namaAnggota,
      double limitPlafon,
      double plafonTerpakai,
      double sisaPlafon,
      String status,
      List<KlaimView> klaimBelumDitagih,
      LocalDateTime createdAt,
      LocalDateTime updatedAt) {}

  public record TagihanView(
      String id,
      String anggotaId,
      String namaAnggota,
      String plafonId,
      LocalDateTime tanggalTerbit,
      LocalDateTime tanggalJatuhTempo,
      double totalTagihan,
      double totalTerbayar,
      double sisaTagihan,
      String status,
      boolean dendaDikenakan,
      double nominalDenda,
      long hariKeterlambatan,
      List<KlaimView> listOfKlaimAlokasi,
      LocalDateTime createdAt,
      LocalDateTime updatedAt) {}

  public record Dashboard(
      long jumlahAnggota,
      long plafonAktif,
      long tagihanBelumLunas,
      double totalPiutang,
      long tagihanJatuhTempo) {}

  public record Statistik(String label, long jumlahTagihan, double totalNilai) {}
}
