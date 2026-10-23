package apap.ti.sikopdes.model;

import jakarta.persistence.*;
import java.util.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
public class Anggota extends BaseEntity {
  @Column(nullable = false, unique = true)
  private String nomorAnggota;

  @Column(nullable = false)
  private String nama;

  @Column(nullable = false)
  private String email;

  @Column(nullable = false)
  private String telepon;

  @Column(nullable = false)
  private String statusKeanggotaan;
}
