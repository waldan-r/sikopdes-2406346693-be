package apap.ti.sikopdes;

import static apap.ti.sikopdes.model.Status.*;
import static org.junit.jupiter.api.Assertions.*;

import apap.ti.sikopdes.dto.Requests.*;
import apap.ti.sikopdes.dto.Views.*;
import apap.ti.sikopdes.exception.BusinessException;
import apap.ti.sikopdes.model.*;
import apap.ti.sikopdes.repository.*;
import apap.ti.sikopdes.service.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(
    properties = {
      "spring.datasource.url=jdbc:h2:mem:workflow;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
      "spring.datasource.username=sa",
      "spring.datasource.password=",
      "spring.datasource.driver-class-name=org.h2.Driver",
      "spring.jpa.hibernate.ddl-auto=create-drop",
      "spring.jpa.show-sql=false",
      "app.overdue.enabled=false"
    })
@Transactional
class WorkflowTests {
  @Autowired PlafonService plafon;
  @Autowired KlaimService klaim;
  @Autowired TagihanService tagihan;
  @Autowired AnggotaService anggota;
  @Autowired ReportService report;
  @Autowired PlafonBelanjaRepository limits;
  @Autowired TagihanRepository bills;
  @Autowired Clock clock;
  @Autowired jakarta.persistence.EntityManager entityManager;

  private LocalDate today() {
    return LocalDate.now(clock);
  }

  private PlafonView limit() {
    return plafon.create(new CreatePlafon("ANG0001", 1000000d));
  }

  private KlaimView claim(String limitId, double amount) {
    return klaim.create(
        new CreateKlaim(
            limitId,
            LocalDateTime.now(clock).minusMinutes(1),
            amount,
            "Sembako",
            "Belanja pengujian"));
  }

  private TagihanView bill(KlaimView claim) {
    return tagihan.create(
        new CreateTagihan(claim.anggotaId(), today().plusDays(3), List.of(claim.id())));
  }

  @Test
  void dueDateSurvivesDatabaseRoundTripWithoutShiftingToTheNextDay() {
    var invoice = bill(claim(limit().id(), 100));
    bills.flush();
    entityManager.clear();
    var persisted = tagihan.detail(invoice.id());
    assertEquals(today().plusDays(3), persisted.tanggalJatuhTempo().toLocalDate());
    assertEquals(23, persisted.tanggalJatuhTempo().getHour());
  }

  @Test
  void seedsMembersOnceAndRejectsInactiveAndDuplicateLimits() {
    assertEquals(15, anggota.list().size());
    assertThrows(
        BusinessException.class, () -> plafon.create(new CreatePlafon("ANG0015", 100000d)));
    limit();
    assertThrows(BusinessException.class, this::limit);
  }

  @Test
  void limitChangesAndFinalClosureRespectOutstandingUsage() {
    var p = limit();
    var k = claim(p.id(), 100000);
    assertThrows(
        BusinessException.class, () -> plafon.update(new UpdatePlafon(p.id(), 99999d, AKTIF)));
    assertThrows(BusinessException.class, () -> plafon.close(p.id()));
    klaim.cancel(k.id());
    plafon.close(p.id());
    assertThrows(
        BusinessException.class, () -> plafon.update(new UpdatePlafon(p.id(), 200000d, AKTIF)));
    assertThrows(BusinessException.class, () -> plafon.close(p.id()));
    assertNotEquals(p.id(), limit().id());
  }

  @Test
  void claimEditsUseAvailableCreditIncludingItsOldAmount() {
    var p = limit();
    var k = claim(p.id(), 900000);
    klaim.update(
        new UpdateKlaim(
            k.id(),
            LocalDateTime.now(clock).minusMinutes(1),
            950000d,
            "Pendidikan",
            "Perubahan nominal"));
    assertEquals(950000, plafon.detail(p.id()).plafonTerpakai());
    assertThrows(BusinessException.class, () -> claim(p.id(), 50000.01));
    klaim.cancel(k.id());
    assertEquals(0, plafon.detail(p.id()).plafonTerpakai());
    assertTrue(klaim.list().isEmpty());
    assertThrows(BusinessException.class, () -> klaim.cancel(k.id()));
  }

  @Test
  void frozenLimitsRejectNewClaimsAndFutureDatesAreRejected() {
    var p = limit();
    assertThrows(
        BusinessException.class,
        () ->
            klaim.create(
                new CreateKlaim(
                    p.id(), LocalDateTime.now(clock).plusDays(1), 10d, "Sembako", "Masa depan")));
    plafon.update(new UpdatePlafon(p.id(), 1000000d, DIBEKUKAN));
    assertThrows(BusinessException.class, () -> claim(p.id(), 1));
  }

  @Test
  void billingDeduplicatesIdsAndRequiresSameMemberAndLimit() {
    var p = limit();
    var k = claim(p.id(), 123.45);
    var second = plafon.create(new CreatePlafon("ANG0002", 100000d));
    var other = claim(second.id(), 100);
    assertThrows(
        BusinessException.class,
        () ->
            tagihan.create(
                new CreateTagihan("ANG0001", today().plusDays(3), List.of(k.id(), other.id()))));
    assertThrows(
        BusinessException.class,
        () -> tagihan.create(new CreateTagihan("ANG0001", today().plusDays(2), List.of(k.id()))));
    var t =
        tagihan.create(new CreateTagihan("ANG0001", today().plusDays(3), List.of(k.id(), k.id())));
    assertEquals(123.45, t.totalTagihan());
    assertEquals(1, t.listOfKlaimAlokasi().size());
    assertThrows(BusinessException.class, () -> bill(k));
    assertThrows(BusinessException.class, () -> klaim.cancel(k.id()));
  }

  @Test
  void invoiceEditingAndCancellationReturnClaimsWithoutRestoringUsedCredit() {
    var p = limit();
    var first = claim(p.id(), 100);
    var second = claim(p.id(), 200);
    var t = bill(first);
    var changed = tagihan.updateClaims(new UpdateTagihan(t.id(), List.of(second.id())));
    assertEquals(200, changed.totalTagihan());
    assertEquals(BELUM_DITAGIH, klaim.detail(first.id()).status());
    tagihan.cancel(t.id());
    assertEquals(300, plafon.detail(p.id()).plafonTerpakai());
    assertEquals(BELUM_DITAGIH, klaim.detail(second.id()).status());
    assertNull(klaim.detail(second.id()).tagihanId());
    assertThrows(BusinessException.class, () -> tagihan.pay(new Pembayaran(t.id(), 10d, today())));
    assertTrue(tagihan.list(false).isEmpty());
  }

  @Test
  void instalmentsDoNotRestoreCreditUntilFullSettlementAndRejectOverpayment() {
    var p = limit();
    var k = claim(p.id(), 0.30);
    var t = bill(k);
    assertThrows(BusinessException.class, () -> tagihan.pay(new Pembayaran(t.id(), 0.31, today())));
    tagihan.pay(new Pembayaran(t.id(), 0.10, today()));
    assertEquals(0.30, plafon.detail(p.id()).plafonTerpakai());
    assertThrows(BusinessException.class, () -> tagihan.cancel(t.id()));
    assertThrows(
        BusinessException.class,
        () -> tagihan.updateClaims(new UpdateTagihan(t.id(), List.of(k.id()))));
    var paid = tagihan.pay(new Pembayaran(t.id(), 0.20, today()));
    assertEquals(LUNAS, paid.status());
    assertEquals(0, paid.sisaTagihan());
    assertEquals(LUNAS, klaim.detail(k.id()).status());
    assertEquals(0, plafon.detail(p.id()).plafonTerpakai());
    assertThrows(BusinessException.class, () -> tagihan.pay(new Pembayaran(t.id(), 0.01, today())));
  }

  @Test
  void overdueProcessingChargesOnceAndSettlementPreservesOtherClaims() {
    var p = limit();
    var first = claim(p.id(), 100);
    claim(p.id(), 500);
    var t = bill(first);
    var entity = bills.findById(t.id()).orElseThrow();
    entity.setTanggalJatuhTempo(LocalDateTime.now(clock).minusHours(30));
    bills.flush();
    tagihan.processOverdue(t.id());
    var overdue = tagihan.detail(t.id());
    assertEquals(20000, overdue.nominalDenda());
    assertEquals(20100, overdue.totalTagihan());
    assertEquals(DIBEKUKAN, plafon.detail(p.id()).status());
    tagihan.processOverdue(t.id());
    assertEquals(20100, tagihan.detail(t.id()).totalTagihan());
    tagihan.pay(new Pembayaran(t.id(), 20100d, today()));
    assertEquals(500, plafon.detail(p.id()).plafonTerpakai());
    assertEquals(DIBEKUKAN, plafon.detail(p.id()).status());
  }

  @Test
  void paymentAppliesFineBeforeCheckingAmountAndNeverCreatesNegativeCredit() {
    var p = limit();
    var t = bill(claim(p.id(), 100));
    bills
        .findById(t.id())
        .orElseThrow()
        .setTanggalJatuhTempo(LocalDateTime.now(clock).minusHours(5));
    bills.flush();
    var paid = tagihan.pay(new Pembayaran(t.id(), 10100d, today()));
    assertEquals(LUNAS, paid.status());
    assertEquals(10000, paid.nominalDenda());
    assertEquals(0, plafon.detail(p.id()).plafonTerpakai());
  }

  @Test
  void statisticsExcludeCancelledInvoicesAndDashboardTracksOutstandingBalances() {
    var p = limit();
    var first = bill(claim(p.id(), 100));
    var second = bill(claim(p.id(), 200));
    tagihan.cancel(second.id());
    tagihan.pay(new Pembayaran(first.id(), 25d, today()));
    assertEquals(75, report.dashboard().totalPiutang());
    assertEquals(1, report.dashboard().tagihanBelumLunas());
    assertEquals(
        100,
        report.statistik("bulanan", today().getYear()).stream()
            .mapToDouble(Statistik::totalNilai)
            .sum());
    assertEquals(4, report.statistik("kuartal", today().getYear()).size());
    assertThrows(BusinessException.class, () -> report.statistik("harian", today().getYear()));
  }
}
