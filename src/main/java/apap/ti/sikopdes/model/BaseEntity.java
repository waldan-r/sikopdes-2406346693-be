package apap.ti.sikopdes.model;

import jakarta.persistence.*;
import java.time.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@MappedSuperclass
public abstract class BaseEntity {
  @Id private String id;

  @Column(nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(nullable = false)
  private LocalDateTime updatedAt;

  @Version private Long version;

  @PrePersist
  void insertTimestamps() {
    createdAt = LocalDateTime.now(ZoneId.of("Asia/Jakarta"));
    updatedAt = createdAt;
  }

  @PreUpdate
  void updateTimestamp() {
    updatedAt = LocalDateTime.now(ZoneId.of("Asia/Jakarta"));
  }
}
