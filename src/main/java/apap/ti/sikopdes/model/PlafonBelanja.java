package apap.ti.sikopdes.model;

import jakarta.persistence.*;
import java.util.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
public class PlafonBelanja extends BaseEntity {
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "anggota_id")
  private Anggota anggota;

  @Column(nullable = false)
  private double limitPlafon;

  @Column(nullable = false)
  private double plafonTerpakai;

  @Column(nullable = false)
  private String status;
}
