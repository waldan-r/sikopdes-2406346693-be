package apap.ti.sikopdes.service;

import static apap.ti.sikopdes.exception.BusinessException.require;
import static apap.ti.sikopdes.model.Status.*;

import apap.ti.sikopdes.dto.Views.*;
import apap.ti.sikopdes.model.*;
import apap.ti.sikopdes.repository.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ReportService {
  private final AnggotaRepository anggota;
  private final PlafonBelanjaRepository plafon;
  private final TagihanRepository tagihan;

  public ReportService(
      AnggotaRepository anggota, PlafonBelanjaRepository plafon, TagihanRepository tagihan) {
    this.anggota = anggota;
    this.plafon = plafon;
    this.tagihan = tagihan;
  }

  public Dashboard dashboard() {
    List<Tagihan> pending =
        tagihan.findAll().stream()
            .filter(t -> Set.of(BELUM_LUNAS, JATUH_TEMPO).contains(t.getStatus()))
            .toList();
    double total =
        Money.sum(
            pending.stream()
                .map(t -> Money.subtract(t.getTotalTagihan(), t.getTotalTerbayar()))
                .toList());
    return new Dashboard(
        anggota.count(),
        plafon.findAll().stream().filter(p -> AKTIF.equals(p.getStatus())).count(),
        pending.size(),
        total,
        pending.stream().filter(t -> JATUH_TEMPO.equals(t.getStatus())).count());
  }

  public List<Statistik> statistik(String period, int year) {
    require(Set.of("bulanan", "kuartal").contains(period), "Periode harus bulanan atau kuartal.");
    require(year >= 2000 && year <= 9999, "Tahun tidak valid.");
    List<Tagihan> bills =
        tagihan.findAll().stream()
            .filter(
                t -> !DIBATALKAN.equals(t.getStatus()) && t.getTanggalTerbit().getYear() == year)
            .toList();
    String[] months = {
      "Januari",
      "Februari",
      "Maret",
      "April",
      "Mei",
      "Juni",
      "Juli",
      "Agustus",
      "September",
      "Oktober",
      "November",
      "Desember"
    };
    List<Statistik> result = new ArrayList<>();
    int width = period.equals("bulanan") ? 1 : 3;
    for (int i = 0; i < 12 / width; i++) {
      final int bucket = i;
      List<Tagihan> group =
          bills.stream()
              .filter(t -> (t.getTanggalTerbit().getMonthValue() - 1) / width == bucket)
              .toList();
      result.add(
          new Statistik(
              width == 1 ? months[i] : "Kuartal " + (i + 1),
              group.size(),
              Money.sum(group.stream().map(Tagihan::getTotalTagihan).toList())));
    }
    return result;
  }
}
