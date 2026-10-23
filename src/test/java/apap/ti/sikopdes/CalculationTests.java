package apap.ti.sikopdes;

import static org.junit.jupiter.api.Assertions.*;

import apap.ti.sikopdes.service.*;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class CalculationTests {
  @Test
  void currencyArithmeticAvoidsFloatingPointResidue() {
    assertEquals(0.3, Money.add(0.1, 0.2));
    assertEquals(0.2, Money.subtract(0.3, 0.1));
  }

  @Test
  void partialDayLatenessRoundsUp() {
    var policy = new LateFeePolicy();
    var due = LocalDateTime.of(2026, 1, 1, 23, 59, 59, 999999999);
    assertEquals(0, policy.lateDays(due, due));
    assertEquals(1, policy.lateDays(due, due.plusNanos(1)));
    assertEquals(1, policy.lateDays(due, due.plusHours(5)));
    assertEquals(2, policy.lateDays(due, due.plusHours(30)));
  }

  @Test
  void settlementFloorProtectsOtherOutstandingClaims() {
    assertEquals(0, Money.release(100, 10100, 0));
    assertEquals(500, Money.release(600, 10100, 500));
  }
}
