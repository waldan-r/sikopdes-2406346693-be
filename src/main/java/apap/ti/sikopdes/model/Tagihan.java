package apap.ti.sikopdes.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
public class Tagihan extends BaseEntity {
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "anggota_id")
  private Anggota anggota;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "plafon_id")
  private PlafonBelanja plafon;

  @Column(nullable = false)
  private LocalDateTime tanggalTerbit;

  @Column(nullable = false)
  private LocalDateTime tanggalJatuhTempo;

  @Column(nullable = false)
  private double totalTagihan;

  @Column(nullable = false)
  private double totalTerbayar;

  @Column(nullable = false)
  private String status;

  @Column(nullable = false)
  private boolean dendaDikenakan;

  @Column(nullable = false)
  private double nominalDenda;

  @OneToMany(mappedBy = "tagihan")
  private List<KlaimAlokasi> listOfKlaimAlokasi = new ArrayList<>();
}
