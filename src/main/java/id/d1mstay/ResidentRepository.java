package id.d1mstay;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import java.util.*;

interface ResidentRepository extends JpaRepository<Resident, Long> {
    List<Resident> findAllByOrderByIdDesc();
    List<Resident> findByMoveOutIsNullOrderByNameAsc();
    boolean existsByRoomIdAndMoveOutIsNull(Long roomId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Resident r where r.id = :id")
    Optional<Resident> findLockedById(Long id);
}
