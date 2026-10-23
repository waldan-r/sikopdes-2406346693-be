package apap.ti.sikopdes.service;

import static apap.ti.sikopdes.exception.BusinessException.require;
import static apap.ti.sikopdes.model.Status.*;

import apap.ti.sikopdes.dto.Requests.*;
import apap.ti.sikopdes.dto.Views.*;
import apap.ti.sikopdes.mapper.ViewMapper;
import apap.ti.sikopdes.model.*;
import apap.ti.sikopdes.repository.*;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class KlaimService {
  private final KlaimAlokasiRepository repository;
  private final EntityLookup lookup;
  private final IdentifierService ids;
  private final ViewMapper mapper;
  private final Clock clock;

  public KlaimService(
      KlaimAlokasiRepository repository,
      EntityLookup lookup,
      IdentifierService ids,
      ViewMapper mapper,
      Clock clock) {
    this.repository = repository;
    this.lookup = lookup;
    this.ids = ids;
    this.mapper = mapper;
    this.clock = clock;
  }

  @Transactional(readOnly = true)
  public List<KlaimView> list() {
    return repository.findAll().stream()
        .filter(k -> !DIBATALKAN.equals(k.getStatus()))
        .sorted(Comparator.comparing(KlaimAlokasi::getTanggalTransaksi).reversed())
        .map(mapper::klaim)
        .toList();
  }

  @Transactional(readOnly = true)
  public KlaimView detail(String id) {
    return mapper.klaim(lookup.klaim(id));
  }

  public KlaimView create(CreateKlaim r) {
    PlafonBelanja p = lookup.lockPlafon(r.plafonId());
    require(AKTIF.equals(p.getStatus()), "Klaim baru hanya dapat memakai plafon Aktif.");
    validate(p, r.tanggalTransaksi(), r.nominalKlaim(), r.kategori(), r.keterangan(), 0);
    KlaimAlokasi k = new KlaimAlokasi();
    k.setId(ids.next(IdentifierService.Kind.KLM));
    k.setPlafon(p);
    k.setStatus(BELUM_DITAGIH);
    assign(k, r.tanggalTransaksi(), r.nominalKlaim(), r.kategori(), r.keterangan());
    p.setPlafonTerpakai(Money.add(p.getPlafonTerpakai(), k.getNominalKlaim()));
    repository.saveAndFlush(k);
    return mapper.klaim(k);
  }

  public KlaimView update(UpdateKlaim r) {
    KlaimAlokasi k = lookup.lockKlaim(r.id());
    require(BELUM_DITAGIH.equals(k.getStatus()), "Hanya klaim Belum Ditagih yang dapat diubah.");
    PlafonBelanja p = k.getPlafon();
    validate(
        p,
        r.tanggalTransaksi(),
        r.nominalKlaim(),
        r.kategori(),
        r.keterangan(),
        k.getNominalKlaim());
    p.setPlafonTerpakai(
        Money.add(Money.subtract(p.getPlafonTerpakai(), k.getNominalKlaim()), r.nominalKlaim()));
    assign(k, r.tanggalTransaksi(), r.nominalKlaim(), r.kategori(), r.keterangan());
    repository.flush();
    return mapper.klaim(k);
  }

  public KlaimView cancel(String id) {
    KlaimAlokasi k = lookup.lockKlaim(id);
    require(
        BELUM_DITAGIH.equals(k.getStatus()), "Hanya klaim Belum Ditagih yang dapat dibatalkan.");
    k.getPlafon()
        .setPlafonTerpakai(Money.subtract(k.getPlafon().getPlafonTerpakai(), k.getNominalKlaim()));
    k.setStatus(DIBATALKAN);
    repository.flush();
    return mapper.klaim(k);
  }

  private void validate(
      PlafonBelanja p,
      LocalDateTime date,
      double value,
      String category,
      String description,
      double oldValue) {
    require(
        !date.isAfter(LocalDateTime.now(clock)), "Tanggal transaksi tidak boleh di masa depan.");
    require(Double.isFinite(value) && value > 0, "Nominal klaim harus lebih dari 0.");
    double available =
        Money.add(Money.subtract(p.getLimitPlafon(), p.getPlafonTerpakai()), oldValue);
    require(
        Money.decimal(value).compareTo(Money.decimal(available)) <= 0,
        "Nominal klaim melebihi sisa plafon.");
    require(KATEGORI.contains(category), "Kategori klaim tidak sesuai.");
    require(description != null && !description.isBlank(), "Keterangan wajib diisi.");
  }

  private void assign(
      KlaimAlokasi k, LocalDateTime date, double value, String category, String description) {
    k.setTanggalTransaksi(date);
    k.setNominalKlaim(value);
    k.setKategori(category);
    k.setKeterangan(description.trim());
  }
}
