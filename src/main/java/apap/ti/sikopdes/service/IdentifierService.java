package apap.ti.sikopdes.service;

import jakarta.annotation.PostConstruct;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class IdentifierService {
  public enum Kind {
    PLF,
    KLM,
    TGH
  }

  private final JdbcTemplate jdbc;

  public IdentifierService(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  @PostConstruct
  void initialize() {
    for (Kind kind : Kind.values())
      jdbc.execute(
          "CREATE SEQUENCE IF NOT EXISTS sikopdes_"
              + kind.name().toLowerCase()
              + "_seq START WITH 1");
  }

  public String next(Kind kind) {
    Long value =
        jdbc.queryForObject(
            "SELECT nextval('sikopdes_" + kind.name().toLowerCase() + "_seq')", Long.class);
    return kind.name() + String.format("%04d", value);
  }
}
