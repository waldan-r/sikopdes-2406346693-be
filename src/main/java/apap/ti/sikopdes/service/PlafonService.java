package apap.ti.sikopdes.service;

import static apap.ti.sikopdes.exception.BusinessException.require;
import static apap.ti.sikopdes.model.Status.*;

import apap.ti.sikopdes.dto.Requests.*;
import apap.ti.sikopdes.dto.Views.*;
import apap.ti.sikopdes.mapper.ViewMapper;
import apap.ti.sikopdes.model.*;
import apap.ti.sikopdes.repository.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class PlafonService {
  private final PlafonBelanjaRepository repository;
  private final KlaimAlokasiRepository claims;
  private final TagihanRepository bills;
  private final EntityLookup lookup;
  private final IdentifierService ids;
  private final ViewMapper mapper;

  public PlafonService(
      PlafonBelanjaRepository repository,
      KlaimAlokasiRepository claims,
      TagihanRepository bills,
      EntityLookup lookup,
      IdentifierService ids,
      ViewMapper mapper) {
    this.repository = repository;
    this.claims = claims;
    this.bills = bills;
    this.lookup = lookup;
    this.ids = ids;
    this.mapper = mapper;
  }

  @Transactional(readOnly = true)
  public List<PlafonView> list() {
    return repository.findAll().stream()
        .sorted(Comparator.comparing(PlafonBelanja::getId))
        .map(p -> mapper.plafon(p, List.of()))
        .toList();
  }

  @Transactional(readOnly = true)
  public PlafonView detail(String id) {
    return view(lookup.plafon(id));
  }

  public PlafonView create(CreatePlafon request) {
    Anggota member = lookup.lockAnggota(request.anggotaId());
    require(AKTIF.equals(member.getStatusKeanggotaan()), "Anggota harus berstatus Aktif.");
    require(
        repository.findByAnggotaIdAndStatusIn(member.getId(), List.of(AKTIF, DIBEKUKAN)).isEmpty(),
        "Anggota sudah memiliki plafon berjalan.");
    validateLimit(request.limitPlafon(), 0);
    PlafonBelanja p = new PlafonBelanja();
    p.setId(ids.next(IdentifierService.Kind.PLF));
    p.setAnggota(member);
    p.setLimitPlafon(request.limitPlafon());
    p.setPlafonTerpakai(0);
    p.setStatus(AKTIF);
    repository.saveAndFlush(p);
    return view(p);
  }

  public PlafonView update(UpdatePlafon request) {
    PlafonBelanja p = lookup.lockPlafon(request.id());
    require(!DITUTUP.equals(p.getStatus()), "Plafon yang sudah ditutup tidak dapat diubah.");
    require(
        Set.of(AKTIF, DIBEKUKAN).contains(request.status()), "Status harus Aktif atau Dibekukan.");
    validateLimit(request.limitPlafon(), p.getPlafonTerpakai());
    p.setLimitPlafon(request.limitPlafon());
    p.setStatus(request.status());
    repository.flush();
    return view(p);
  }

  public PlafonView close(String id) {
    PlafonBelanja p = lookup.lockPlafon(id);
    require(!DITUTUP.equals(p.getStatus()), "Plafon sudah ditutup.");
    require(
        Money.decimal(p.getPlafonTerpakai()).signum() == 0,
        "Plafon masih memiliki pemakaian berjalan.");
    require(
        bills.findByPlafonIdAndStatusIn(id, List.of(BELUM_LUNAS, JATUH_TEMPO)).isEmpty(),
        "Masih ada tagihan yang belum lunas.");
    p.setStatus(DITUTUP);
    repository.flush();
    return view(p);
  }

  private void validateLimit(double value, double used) {
    require(
        Double.isFinite(value) && value > 0 && value <= 10000000,
        "Limit plafon harus lebih dari 0 dan maksimal Rp10.000.000.");
    require(
        Money.decimal(value).compareTo(Money.decimal(used)) >= 0,
        "Limit tidak boleh lebih kecil dari plafon terpakai.");
  }

  private PlafonView view(PlafonBelanja p) {
    return mapper.plafon(p, claims.findByPlafonId(p.getId()));
  }
}
