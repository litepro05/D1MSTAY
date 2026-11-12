package id.d1mstay;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.*;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional // Semua data pengujian dibatalkan setelah setiap pengujian.
class KosWorkflowTest {
    @Autowired KosService kos;
    @Autowired RoomRepository rooms;
    @Autowired ResidentRepository residents;
    @Autowired MockMvc mvc;

    private Room room() {
        String number = "TEST-" + UUID.randomUUID().toString().substring(0, 8);
        kos.saveRoom(new Forms.RoomForm(null, number, new BigDecimal("850000")));
        return rooms.findByDeletedFalseOrderByNumberAsc().stream()
            .filter(r -> r.getNumber().equalsIgnoreCase(number)).findFirst().orElseThrow();
    }

    private Resident resident(Room room) {
        kos.saveResident(new Forms.ResidentForm(null, "Penghuni Uji", "081234567890",
            room.getId(), LocalDate.now().minusMonths(2)));
        return kos.activeResidents().stream().filter(r -> r.getRoom().getId().equals(room.getId())).findFirst().orElseThrow();
    }

    private Bill bill(Resident resident, String period) {
        kos.createBills(period, resident.getId(), LocalDate.now());
        return kos.bills().stream().filter(b -> b.getResident().getId().equals(resident.getId())
            && b.getPeriod().equals(period)).findFirst().orElseThrow();
    }

    @Test
    void anonymousCannotManageDataAndCsrfIsRequired() throws Exception {
        mvc.perform(get("/rooms")).andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrlPattern("**/login"));
        mvc.perform(post("/rooms/save").with(user("admin"))
            .param("number", "INVALID").param("rate", "100"))
            .andExpect(status().isForbidden());
        mvc.perform(get("/login")).andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Masuk ke dashboard")));
    }

    @Test @WithMockUser(roles = "ADMIN")
    void formsSaveAndAllPagesRender() throws Exception {
        mvc.perform(post("/rooms/save").with(csrf()).param("number", " ")
            .param("rate", "0")).andExpect(status().isOk())
            .andExpect(model().attributeHasFieldErrors("form", "number", "rate"));
        var room = room();
        mvc.perform(post("/residents/save").with(csrf())
            .param("name", "Penghuni Form").param("phone", "081234567890")
            .param("roomId", room.getId().toString()).param("moveIn", LocalDate.now().toString()))
            .andExpect(redirectedUrl("/residents"));
        var resident = kos.activeResidents().stream().filter(r -> r.getRoom().getId().equals(room.getId())).findFirst().orElseThrow();
        var bill = bill(resident, YearMonth.now().toString());
        for (String path : new String[]{"/", "/rooms", "/rooms/new", "/rooms/" + room.getId() + "/edit",
                "/residents", "/residents/new", "/residents/" + resident.getId() + "/edit",
                "/residents/" + resident.getId() + "/checkout", "/bills", "/bills/new",
                "/bills/" + bill.getId(), "/reports"}) {
            mvc.perform(get(path)).andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("D1MSTAY")));
        }
    }

    @Test
    void roomOccupancyAndResidentDatesAreValidated() {
        var room = room();
        var resident = resident(room);
        assertThatThrownBy(() -> resident(room)).hasMessageContaining("sudah ditempati");
        assertThatThrownBy(() -> kos.deleteRoom(room.getId())).hasMessageContaining("masih ditempati");
        assertThatThrownBy(() -> kos.checkout(resident.getId(), resident.getMoveIn().minusDays(1)))
            .hasMessageContaining("Tanggal keluar");
        assertThatThrownBy(() -> kos.saveResident(new Forms.ResidentForm(resident.getId(), resident.getName(),
            resident.getPhone(), room.getId(), LocalDate.now().plusDays(1))))
            .hasMessageContaining("Tanggal masuk");
        kos.checkout(resident.getId(), LocalDate.now());
        assertThat(kos.occupied(room.getId())).isFalse();
        kos.deleteRoom(room.getId());
        assertThat(kos.rooms()).noneMatch(r -> r.getId().equals(room.getId()));
        assertThat(kos.resident(resident.getId()).getRoom().getNumber()).isEqualTo(room.getNumber());
    }

    @Test
    void duplicatePeriodsAndTariffChangesPreserveOldBills() {
        var room = room();
        var resident = resident(room);
        var month = YearMonth.now().toString();
        var bill = bill(resident, month);
        assertThatThrownBy(() -> kos.createBills(month, resident.getId(), LocalDate.now()))
            .hasMessageContaining("Tidak ada tagihan baru");
        assertThatThrownBy(() -> kos.createBills(YearMonth.now().plusMonths(1).toString(),
            resident.getId(), LocalDate.now())).hasMessageContaining("mendatang");
        assertThatThrownBy(() -> kos.createBills(YearMonth.from(resident.getMoveIn()).minusMonths(1).toString(),
            resident.getId(), LocalDate.now())).hasMessageContaining("Tidak ada tagihan baru");
        kos.saveRoom(new Forms.RoomForm(room.getId(), room.getNumber(), new BigDecimal("950000")));
        assertThat(kos.bill(bill.getId()).getAmount()).isEqualByComparingTo("850000");
        assertThatThrownBy(() -> kos.saveResident(new Forms.ResidentForm(resident.getId(), resident.getName(),
            resident.getPhone(), room.getId(), resident.getMoveIn().plusDays(1)))).hasMessageContaining("Tanggal masuk");
        assertThatThrownBy(() -> kos.deleteResident(resident.getId())).hasMessageContaining("memiliki tagihan");
        kos.deleteBill(bill.getId());
        assertThat(kos.bills()).noneMatch(b -> b.getId().equals(bill.getId()));
    }

    @Test
    void checkoutKeepsBillsPayableAndPaymentIsFinal() {
        var resident = resident(room());
        var bill = bill(resident, YearMonth.now().toString());
        kos.checkout(resident.getId(), LocalDate.now());
        assertThat(kos.billPreview(YearMonth.now().minusMonths(1).toString(), resident.getId())).isEmpty();
        assertThatThrownBy(() -> kos.pay(bill.getId(), LocalDate.now().plusDays(1), "Tunai"))
            .hasMessageContaining("Tanggal pembayaran");
        assertThatThrownBy(() -> kos.pay(bill.getId(), LocalDate.now(), "Kredit"))
            .hasMessageContaining("metode");
        kos.pay(bill.getId(), LocalDate.now(), "Transfer");
        assertThat(kos.bill(bill.getId()).isPaid()).isTrue();
        assertThatThrownBy(() -> kos.pay(bill.getId(), LocalDate.now(), "Tunai"))
            .hasMessageContaining("sudah lunas");
        assertThatThrownBy(() -> kos.deleteBill(bill.getId())).hasMessageContaining("Tagihan lunas");
        assertThatThrownBy(() -> kos.deleteRoom(resident.getRoom().getId())).hasMessageContaining("terkait tagihan");
    }

    @Test
    void reportsUsePaymentDateRatherThanBillPeriod() {
        var resident = resident(room());
        String oldPeriod = YearMonth.now().minusMonths(1).toString();
        var bill = bill(resident, oldPeriod);
        kos.pay(bill.getId(), LocalDate.now(), "Tunai");
        assertThat(kos.receipts(YearMonth.now().toString())).anyMatch(b -> b.getId().equals(bill.getId()));
        assertThat(kos.receipts(oldPeriod)).noneMatch(b -> b.getId().equals(bill.getId()));
        assertThat(kos.filterBills(oldPeriod, "paid", resident.getRoom().getNumber())).anyMatch(b -> b.getId().equals(bill.getId()));
        assertThat(kos.filterBills(oldPeriod, "unpaid", resident.getRoom().getNumber())).isEmpty();
    }
}
