package id.d1mstay;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

interface RoomRepository extends JpaRepository<Room, Long> {
    List<Room> findByDeletedFalseOrderByNumberAsc();
    boolean existsByNumberIgnoreCaseAndIdNot(String number, Long id);
}
