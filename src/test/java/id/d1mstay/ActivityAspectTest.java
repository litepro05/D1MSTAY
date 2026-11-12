package id.d1mstay;

import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.annotation.Transactional;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@Transactional
@ExtendWith(OutputCaptureExtension.class)
class ActivityAspectTest {
    @Autowired KosService kos;

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void recordsSuccessfulOperationWithoutLoggingFormData(CapturedOutput output) {
        String number = "AOP-" + UUID.randomUUID().toString().substring(0, 8);
        kos.saveRoom(new Forms.RoomForm(null, number, new BigDecimal("850000")));
        assertThat(kos.rooms()).anyMatch(room -> room.getNumber().equalsIgnoreCase(number));
        assertThat(output.getOut()).contains("[Aktivitas] pengguna=admin aksi=saveRoom hasil=selesai")
            .doesNotContain(number);
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void recordsFailureAndPreservesValidation(CapturedOutput output) {
        long before = kos.rooms().size();
        assertThatThrownBy(() -> kos.saveRoom(new Forms.RoomForm(null, "", BigDecimal.ZERO)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Nomor dan tarif kamar harus diisi dengan benar.");
        assertThat(kos.rooms()).hasSize((int) before);
        assertThat(output.getOut()).contains(
            "[Aktivitas] pengguna=admin aksi=saveRoom hasil=gagal sebab=IllegalArgumentException");
    }
}
