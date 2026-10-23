package apap.ti.sikopdes.config;

import apap.ti.sikopdes.model.Status;
import apap.ti.sikopdes.repository.TagihanRepository;
import apap.ti.sikopdes.service.TagihanService;
import java.time.*;
import org.slf4j.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class OverdueScheduler {
  private static final Logger log = LoggerFactory.getLogger(OverdueScheduler.class);
  private final TagihanRepository repository;
  private final TagihanService service;
  private final Clock clock;
  private final boolean enabled;

  public OverdueScheduler(
      TagihanRepository repository,
      TagihanService service,
      Clock clock,
      @Value("${app.overdue.enabled:true}") boolean enabled) {
    this.repository = repository;
    this.service = service;
    this.clock = clock;
    this.enabled = enabled;
  }

  @Scheduled(
      fixedDelayString = "${app.overdue.interval-ms:60000}",
      initialDelayString = "${app.overdue.interval-ms:60000}")
  public void process() {
    if (!enabled) return;
    for (var bill :
        repository.findByStatusAndTanggalJatuhTempoBefore(
            Status.BELUM_LUNAS, LocalDateTime.now(clock))) {
      try {
        service.processOverdue(bill.getId());
      } catch (Exception e) {
        log.error("Pemeriksaan jatuh tempo gagal untuk {}", bill.getId(), e);
      }
    }
  }
}
