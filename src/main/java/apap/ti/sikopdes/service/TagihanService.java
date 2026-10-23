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
public class TagihanService {
  private final TagihanRepository repository;
  private final KlaimAlokasiRepository claims;
  private final EntityLookup lookup;
  private final IdentifierService ids;
  private final ViewMapper mapper;
  private final Clock clock;
  private final LateFeePolicy fee;

  public TagihanService(
      TagihanRepository repository,
      KlaimAlokasiRepository claims,
      EntityLookup lookup,
      IdentifierService ids,
      ViewMapper mapper,
      Clock clock,
      LateFeePolicy fee) {
    this.repository = repository;
    this.claims = claims;
    this.lookup = lookup;
    this.ids = ids;
    this.mapper = mapper;
    this.clock = clock;
    this.fee = fee;
  }

  @Transactional(readOnly = true)
  public List<TagihanView> list(boolean overdue) {
    var stream = repository.findAll().stream().filter(t -> !DIBATALKAN.equals(t.getStatus()));
    if (overdue) stream = stream.filter(t -> JATUH_TEMPO.equals(t.getStatus()));
    Comparator<Tagihan> sort =
        overdue
            ? Comparator.comparing(Tagihan::getTanggalJatuhTempo)
            : Comparator.comparing(Tagihan::getTanggalTerbit).reversed();
    return stream.sorted(sort).map(this::view).toList();
  }

  @Transactional(readOnly = true)
  public TagihanView detail(String id) {
    return view(lookup.tagihan(id));
  }

  public TagihanView create(CreateTagihan r) {
    Anggota member = lookup.lockAnggota(r.anggotaId());
    LocalDateTime now = LocalDateTime.now(clock);
    require(
        !r.tanggalJatuhTempo().isBefore(now.toLocalDate().plusDays(3)),
        "Jatuh tempo minimal 3 hari setelah tanggal terbit.");
    List<KlaimAlokasi> selected = select(r.klaimIds(), member.getId(), null);
    Tagihan t = new Tagihan();
    t.setId(ids.next(IdentifierService.Kind.TGH));
    t.setAnggota(member);
    t.setPlafon(selected.getFirst().getPlafon());
    t.setTanggalTerbit(now);
    // PostgreSQL timestamp(6) membulatkan LocalTime.MAX ke hari berikutnya.
    t.setTanggalJatuhTempo(r.tanggalJatuhTempo().atTime(23, 59, 59, 999_999_000));
    t.setTotalTagihan(total(selected));
    t.setTotalTerbayar(0);
    t.setStatus(BELUM_LUNAS);
    repository.save(t);
    selected.forEach(
        k -> {
          k.setTagihan(t);
          k.setStatus(SUDAH_DITAGIH);
        });
    claims.flush();
    return view(t);
  }

  public TagihanView updateClaims(UpdateTagihan r) {
    Tagihan t = lookup.lockTagihan(r.id());
    requireEditable(t);
    List<KlaimAlokasi> selected = select(r.klaimIds(), t.getAnggota().getId(), t);
    require(
        selected.stream().allMatch(k -> k.getPlafon().getId().equals(t.getPlafon().getId())),
        "Klaim harus berasal dari plafon tagihan yang sama.");
    claims
        .findByTagihanId(t.getId())
        .forEach(
            k -> {
              k.setTagihan(null);
              k.setStatus(BELUM_DITAGIH);
            });
    selected.forEach(
        k -> {
          k.setTagihan(t);
          k.setStatus(SUDAH_DITAGIH);
        });
    t.setTotalTagihan(total(selected));
    repository.flush();
    return view(t);
  }

  public TagihanView pay(Pembayaran r) {
    Tagihan t = lookup.lockTagihan(r.id());
    require(
        Set.of(BELUM_LUNAS, JATUH_TEMPO).contains(t.getStatus()),
        "Tagihan final tidak dapat dibayar.");
    LocalDateTime now = LocalDateTime.now(clock);
    require(
        !r.tanggalBayar().isBefore(t.getTanggalTerbit().toLocalDate()),
        "Tanggal bayar tidak boleh sebelum tanggal terbit.");
    require(
        !r.tanggalBayar().isAfter(now.toLocalDate()), "Tanggal bayar tidak boleh di masa depan.");
    LocalDateTime at =
        r.tanggalBayar().equals(now.toLocalDate()) ? now : r.tanggalBayar().atTime(LocalTime.MAX);
    fee.apply(t, at);
    double remaining = Money.subtract(t.getTotalTagihan(), t.getTotalTerbayar());
    require(
        Double.isFinite(r.nominalPembayaran()) && r.nominalPembayaran() > 0,
        "Nominal pembayaran harus lebih dari 0.");
    require(
        Money.decimal(r.nominalPembayaran()).compareTo(Money.decimal(remaining)) <= 0,
        "Pembayaran tidak boleh melebihi sisa tagihan.");
    t.setTotalTerbayar(Money.add(t.getTotalTerbayar(), r.nominalPembayaran()));
    if (Money.decimal(t.getTotalTerbayar()).compareTo(Money.decimal(t.getTotalTagihan())) >= 0) {
      t.setStatus(LUNAS);
      List<KlaimAlokasi> billed = claims.findByTagihanId(t.getId());
      billed.forEach(k -> k.setStatus(LUNAS));
      double other =
          Money.sum(
              claims.findByPlafonId(t.getPlafon().getId()).stream()
                  .filter(k -> Set.of(BELUM_DITAGIH, SUDAH_DITAGIH).contains(k.getStatus()))
                  .map(KlaimAlokasi::getNominalKlaim)
                  .toList());
      t.getPlafon()
          .setPlafonTerpakai(
              Money.release(t.getPlafon().getPlafonTerpakai(), t.getTotalTagihan(), other));
    }
    repository.flush();
    return view(t);
  }

  public TagihanView cancel(String id) {
    Tagihan t = lookup.lockTagihan(id);
    requireEditable(t);
    claims
        .findByTagihanId(id)
        .forEach(
            k -> {
              k.setStatus(BELUM_DITAGIH);
              k.setTagihan(null);
            });
    t.setStatus(DIBATALKAN);
    repository.flush();
    return view(t);
  }

  public void processOverdue(String id) {
    Tagihan t = lookup.lockTagihan(id);
    fee.apply(t, LocalDateTime.now(clock));
  }

  private void requireEditable(Tagihan t) {
    require(
        BELUM_LUNAS.equals(t.getStatus()) && Money.decimal(t.getTotalTerbayar()).signum() == 0,
        "Hanya tagihan Belum Lunas tanpa pembayaran yang dapat diubah atau dibatalkan.");
    require(
        !LocalDateTime.now(clock).isAfter(t.getTanggalJatuhTempo()),
        "Tagihan sudah melewati jatuh tempo dan tidak dapat diubah atau dibatalkan.");
  }

  private List<KlaimAlokasi> select(List<String> ids, String memberId, Tagihan existing) {
    require(ids != null && !ids.isEmpty(), "Pilih minimal satu klaim.");
    List<KlaimAlokasi> selected = new LinkedHashSet<>(ids).stream().map(lookup::klaim).toList();
    for (KlaimAlokasi k : selected) {
      require(
          k.getPlafon().getAnggota().getId().equals(memberId),
          "Semua klaim harus milik anggota yang sama.");
      boolean current =
          existing != null
              && k.getTagihan() != null
              && k.getTagihan().getId().equals(existing.getId());
      require(
          BELUM_DITAGIH.equals(k.getStatus()) || current,
          "Klaim sudah ditagih, lunas, atau dibatalkan.");
      require(
          k.getPlafon().getId().equals(selected.getFirst().getPlafon().getId()),
          "Semua klaim harus berasal dari plafon yang sama.");
    }
    return selected;
  }

  private double total(List<KlaimAlokasi> selected) {
    return Money.sum(selected.stream().map(KlaimAlokasi::getNominalKlaim).toList());
  }

  private TagihanView view(Tagihan t) {
    return mapper.tagihan(t, claims.findByTagihanId(t.getId()));
  }
}
