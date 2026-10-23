package apap.ti.sikopdes.service;

import apap.ti.sikopdes.exception.BusinessException;
import apap.ti.sikopdes.model.*;
import apap.ti.sikopdes.repository.*;
import org.springframework.stereotype.Component;

@Component
public class EntityLookup {
  private final AnggotaRepository anggota;
  private final PlafonBelanjaRepository plafon;
  private final KlaimAlokasiRepository klaim;
  private final TagihanRepository tagihan;

  public EntityLookup(
      AnggotaRepository anggota,
      PlafonBelanjaRepository plafon,
      KlaimAlokasiRepository klaim,
      TagihanRepository tagihan) {
    this.anggota = anggota;
    this.plafon = plafon;
    this.klaim = klaim;
    this.tagihan = tagihan;
  }

  public Anggota anggota(String id) {
    return anggota.findById(id).orElseThrow(() -> BusinessException.notFound("Anggota", id));
  }

  public Anggota lockAnggota(String id) {
    return anggota.lockById(id).orElseThrow(() -> BusinessException.notFound("Anggota", id));
  }

  public PlafonBelanja plafon(String id) {
    return plafon.findById(id).orElseThrow(() -> BusinessException.notFound("Plafon", id));
  }

  public KlaimAlokasi klaim(String id) {
    return klaim.findById(id).orElseThrow(() -> BusinessException.notFound("Klaim", id));
  }

  public Tagihan tagihan(String id) {
    return tagihan.findById(id).orElseThrow(() -> BusinessException.notFound("Tagihan", id));
  }

  // Semua mutasi mengunci anggota terlebih dahulu: urutan lock konsisten lintas fitur dan
  // scheduler.
  public PlafonBelanja lockPlafon(String id) {
    lockAnggota(plafon.memberId(id).orElseThrow(() -> BusinessException.notFound("Plafon", id)));
    return plafon(id);
  }

  public KlaimAlokasi lockKlaim(String id) {
    lockAnggota(klaim.memberId(id).orElseThrow(() -> BusinessException.notFound("Klaim", id)));
    return klaim(id);
  }

  public Tagihan lockTagihan(String id) {
    lockAnggota(tagihan.memberId(id).orElseThrow(() -> BusinessException.notFound("Tagihan", id)));
    return tagihan(id);
  }
}
