package apap.ti.sikopdes.repository;

import apap.ti.sikopdes.model.Tagihan;
import java.time.LocalDateTime;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface TagihanRepository extends JpaRepository<Tagihan, String> {
  List<Tagihan> findByPlafonIdAndStatusIn(String plafonId, Collection<String> statuses);

  List<Tagihan> findByStatusAndTanggalJatuhTempoBefore(String status, LocalDateTime time);

  @Query("select t.anggota.id from Tagihan t where t.id=:id")
  Optional<String> memberId(@Param("id") String id);
}
