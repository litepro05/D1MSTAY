package id.d1mstay;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

interface AdminRepository extends JpaRepository<AdminAccount, Long> {
    Optional<AdminAccount> findByUsername(String username);
}
