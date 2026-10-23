package apap.ti.sikopdes.repository;

import apap.ti.sikopdes.model.Anggota;
import jakarta.persistence.LockModeType;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface AnggotaRepository extends JpaRepository<Anggota, String> {
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select a from Anggota a where a.id=:id")
  Optional<Anggota> lockById(@Param("id") String id);
}
