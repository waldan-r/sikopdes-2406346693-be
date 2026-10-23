package apap.ti.sikopdes.repository;

import apap.ti.sikopdes.model.KlaimAlokasi;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface KlaimAlokasiRepository extends JpaRepository<KlaimAlokasi, String> {
  List<KlaimAlokasi> findByPlafonId(String plafonId);

  List<KlaimAlokasi> findByTagihanId(String tagihanId);

  @Query("select k.plafon.anggota.id from KlaimAlokasi k where k.id=:id")
  Optional<String> memberId(@Param("id") String id);
}
