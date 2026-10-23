package apap.ti.sikopdes.service;

import java.math.*;
import java.util.Collection;

public final class Money {
  private Money() {}

  public static BigDecimal decimal(double value) {
    return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP);
  }

  public static double add(double a, double b) {
    return decimal(a).add(decimal(b)).doubleValue();
  }

  public static double subtract(double a, double b) {
    return decimal(a).subtract(decimal(b)).doubleValue();
  }

  public static double sum(Collection<Double> values) {
    return values.stream()
        .map(Money::decimal)
        .reduce(BigDecimal.ZERO, BigDecimal::add)
        .doubleValue();
  }

  public static double release(double used, double billTotal, double otherClaims) {
    // PDF mengurangi totalTagihan. Batas bawah menjaga alokasi klaim lain dan saldo nonnegatif.
    return Math.max(Math.max(0, otherClaims), subtract(used, billTotal));
  }
}
