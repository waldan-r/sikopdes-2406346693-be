package apap.ti.sikopdes.service;

import static apap.ti.sikopdes.model.Status.*;

import apap.ti.sikopdes.model.*;
import java.time.*;
import org.springframework.stereotype.Component;

@Component
public class LateFeePolicy {
  public long lateDays(LocalDateTime due, LocalDateTime at) {
    if (!at.isAfter(due)) return 0;
    Duration duration = Duration.between(due, at);
    long days = duration.getSeconds() / 86400;
    return days + (duration.getSeconds() % 86400 != 0 || duration.getNano() != 0 ? 1 : 0);
  }

  public void apply(Tagihan bill, LocalDateTime at) {
    if (LUNAS.equals(bill.getStatus()) || DIBATALKAN.equals(bill.getStatus())) return;
    long days = lateDays(bill.getTanggalJatuhTempo(), at);
    if (days == 0) return;
    if (!bill.isDendaDikenakan()) {
      double fine = Money.decimal(10000).multiply(java.math.BigDecimal.valueOf(days)).doubleValue();
      bill.setNominalDenda(fine);
      bill.setTotalTagihan(Money.add(bill.getTotalTagihan(), fine));
      bill.setDendaDikenakan(true);
    }
    bill.setStatus(JATUH_TEMPO);
    if (AKTIF.equals(bill.getPlafon().getStatus())) bill.getPlafon().setStatus(DIBEKUKAN);
  }
}
