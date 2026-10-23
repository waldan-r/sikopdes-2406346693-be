package apap.ti.sikopdes.mapper;

import static apap.ti.sikopdes.model.Status.*;

import apap.ti.sikopdes.dto.Views.*;
import apap.ti.sikopdes.model.*;
import apap.ti.sikopdes.service.*;
import java.time.*;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class ViewMapper {
  private final Clock clock;
  private final LateFeePolicy fee;

  public ViewMapper(Clock clock, LateFeePolicy fee) {
    this.clock = clock;
    this.fee = fee;
  }

  public AnggotaView anggota(Anggota a) {
    return new AnggotaView(
        a.getId(),
        a.getNomorAnggota(),
        a.getNama(),
        a.getEmail(),
        a.getTelepon(),
        a.getStatusKeanggotaan());
  }

  public KlaimView klaim(KlaimAlokasi k) {
    return new KlaimView(
        k.getId(),
        k.getPlafon().getId(),
        k.getPlafon().getAnggota().getId(),
        k.getPlafon().getAnggota().getNama(),
        k.getTanggalTransaksi(),
        k.getNominalKlaim(),
        k.getKategori(),
        k.getKeterangan(),
        k.getStatus(),
        k.getTagihan() == null ? null : k.getTagihan().getId(),
        k.getCreatedAt(),
        k.getUpdatedAt());
  }

  public PlafonView plafon(PlafonBelanja p, List<KlaimAlokasi> claims) {
    return new PlafonView(
        p.getId(),
        p.getAnggota().getId(),
        p.getAnggota().getNama(),
        p.getLimitPlafon(),
        p.getPlafonTerpakai(),
        Money.subtract(p.getLimitPlafon(), p.getPlafonTerpakai()),
        p.getStatus(),
        claims.stream().filter(k -> BELUM_DITAGIH.equals(k.getStatus())).map(this::klaim).toList(),
        p.getCreatedAt(),
        p.getUpdatedAt());
  }

  public TagihanView tagihan(Tagihan t, List<KlaimAlokasi> claims) {
    long days =
        JATUH_TEMPO.equals(t.getStatus())
            ? fee.lateDays(t.getTanggalJatuhTempo(), LocalDateTime.now(clock))
            : 0;
    return new TagihanView(
        t.getId(),
        t.getAnggota().getId(),
        t.getAnggota().getNama(),
        t.getPlafon().getId(),
        t.getTanggalTerbit(),
        t.getTanggalJatuhTempo(),
        t.getTotalTagihan(),
        t.getTotalTerbayar(),
        Money.subtract(t.getTotalTagihan(), t.getTotalTerbayar()),
        t.getStatus(),
        t.isDendaDikenakan(),
        t.getNominalDenda(),
        days,
        claims.stream().map(this::klaim).toList(),
        t.getCreatedAt(),
        t.getUpdatedAt());
  }
}
