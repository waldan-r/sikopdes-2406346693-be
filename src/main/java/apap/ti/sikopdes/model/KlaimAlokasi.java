package apap.ti.sikopdes.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
public class KlaimAlokasi extends BaseEntity {
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "plafon_id")
  private PlafonBelanja plafon;

  @Column(nullable = false)
  private LocalDateTime tanggalTransaksi;

  @Column(nullable = false)
  private double nominalKlaim;

  @Column(nullable = false)
  private String kategori;

  @Column(nullable = false, length = 1000)
  private String keterangan;

  @Column(nullable = false)
  private String status;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "tagihan_id")
  private Tagihan tagihan;
}
