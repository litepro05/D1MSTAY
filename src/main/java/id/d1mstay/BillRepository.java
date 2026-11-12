package id.d1mstay;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import java.util.*;

interface BillRepository extends JpaRepository<Bill, Long> {
    List<Bill> findAllByOrderByPeriodDescIdDesc();
    boolean existsByResidentId(Long residentId);
    boolean existsByResidentRoomId(Long roomId);
    boolean existsByResidentIdAndPeriod(Long residentId, String period);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from Bill b where b.id = :id")
    Optional<Bill> findLockedById(Long id);
}
