package apap.ti.sikopdes.repository;

import apap.ti.sikopdes.model.PlafonBelanja;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface PlafonBelanjaRepository extends JpaRepository<PlafonBelanja, String> {
  List<PlafonBelanja> findByAnggotaIdAndStatusIn(String anggotaId, Collection<String> statuses);

  @Query("select p.anggota.id from PlafonBelanja p where p.id=:id")
  Optional<String> memberId(@Param("id") String id);
}
