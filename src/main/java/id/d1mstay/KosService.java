package id.d1mstay;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.*;
import java.time.format.DateTimeParseException;
import java.util.*;

@Service
@Transactional(readOnly = true)
public class KosService {
    private final RoomRepository rooms;
    private final ResidentRepository residents;
    private final BillRepository bills;

    public KosService(RoomRepository rooms, ResidentRepository residents, BillRepository bills) {
        this.rooms = rooms;
        this.residents = residents;
        this.bills = bills;
    }

    public List<Room> rooms() { return rooms.findByDeletedFalseOrderByNumberAsc(); }
    public List<Resident> residents() { return residents.findAllByOrderByIdDesc(); }
    public List<Resident> activeResidents() { return residents.findByMoveOutIsNullOrderByNameAsc(); }
    public List<Bill> bills() { return bills.findAllByOrderByPeriodDescIdDesc(); }
    public boolean occupied(Long roomId) { return residents.existsByRoomIdAndMoveOutIsNull(roomId); }
    public boolean hasBills(Long residentId) { return bills.existsByResidentId(residentId); }

    public Room room(Long id) {
        return rooms.findById(id).filter(r -> !r.isDeleted())
            .orElseThrow(() -> invalid("Kamar tidak ditemukan."));
    }

    public Resident resident(Long id) {
        return residents.findById(id).orElseThrow(() -> invalid("Penghuni tidak ditemukan."));
    }

    public Bill bill(Long id) {
        return bills.findById(id).orElseThrow(() -> invalid("Tagihan tidak ditemukan."));
    }

    public List<Room> availableRooms() {
        var occupiedIds = new HashSet<>(activeResidents().stream().map(r -> r.getRoom().getId()).toList());
        return rooms().stream().filter(r -> !occupiedIds.contains(r.getId())).toList();
    }

    @Transactional
    public void saveRoom(Forms.RoomForm form) {
        var number = form.number().strip().toUpperCase(Locale.ROOT);
        require(!number.isEmpty() && form.rate() != null && form.rate().signum() > 0,
            "Nomor dan tarif kamar harus diisi dengan benar.");
        require(!rooms.existsByNumberIgnoreCaseAndIdNot(number, form.id() == null ? -1L : form.id()),
            "Nomor kamar sudah digunakan, termasuk dalam arsip kamar.");
        var room = form.id() == null ? new Room() : room(form.id());
        room.setNumber(number);
        room.setRate(form.rate());
        rooms.save(room);
    }

    @Transactional
    public void deleteRoom(Long id) {
        var room = room(id);
        require(!occupied(id), "Kamar masih ditempati. Catat penghuni keluar terlebih dahulu.");
        require(!bills.existsByResidentRoomId(id), "Kamar sudah terkait tagihan dan tidak dapat dihapus.");
        // Arsip menjaga nomor kamar pada riwayat penghuni yang pernah keluar.
        room.setDeleted(true);
    }

    @Transactional
    public void saveResident(Forms.ResidentForm form) {
        require(form.moveIn() != null && !form.moveIn().isAfter(LocalDate.now()),
            "Tanggal masuk tidak boleh setelah hari ini.");
        require(form.name() != null && !form.name().isBlank(), "Nama penghuni wajib diisi.");
        var resident = form.id() == null ? new Resident() : lockedResident(form.id());
        if (form.id() == null) {
            resident.setRoom(room(form.roomId()));
            require(!occupied(form.roomId()), "Kamar sudah ditempati penghuni aktif.");
        } else {
            require(Objects.equals(resident.getRoom().getId(), form.roomId()), "Kamar penghuni tidak dapat diganti.");
            require(!hasBills(resident.getId()) || resident.getMoveIn().equals(form.moveIn()),
                "Tanggal masuk tidak dapat diubah setelah ada tagihan.");
            require(resident.getMoveOut() == null || !form.moveIn().isAfter(resident.getMoveOut()),
                "Tanggal masuk tidak boleh setelah tanggal keluar.");
        }
        resident.setName(form.name().strip());
        resident.setPhone(form.phone().strip());
        resident.setMoveIn(form.moveIn());
        residents.save(resident);
    }

    @Transactional
    public void checkout(Long id, LocalDate date) {
        var resident = lockedResident(id);
        require(resident.isActive(), "Penghuni sudah tercatat keluar.");
        require(date != null && !date.isBefore(resident.getMoveIn()) && !date.isAfter(LocalDate.now()),
            "Tanggal keluar harus di antara tanggal masuk dan hari ini.");
        resident.setMoveOut(date);
    }

    @Transactional
    public void deleteResident(Long id) {
        var resident = lockedResident(id);
        require(!hasBills(id), "Penghuni sudah memiliki tagihan dan tidak dapat dihapus.");
        residents.delete(resident);
    }

    public YearMonth period(String value) {
        try {
            var month = YearMonth.parse(value);
            require(month.getYear() >= 1900 && month.getYear() <= 9999, "Tahun tidak valid.");
            return month;
        } catch (DateTimeParseException | NullPointerException e) {
            throw invalid("Periode harus berupa bulan dan tahun yang benar.");
        }
    }

    public List<Resident> billPreview(String value, Long residentId) {
        var month = period(value);
        require(!month.isAfter(YearMonth.now()), "Tagihan tidak boleh dibuat untuk bulan mendatang.");
        return activeResidents().stream()
            .filter(r -> residentId == null || r.getId().equals(residentId))
            .filter(r -> !month.isBefore(YearMonth.from(r.getMoveIn())))
            .filter(r -> !bills.existsByResidentIdAndPeriod(r.getId(), value)).toList();
    }

    @Transactional
    public int createBills(String value, Long residentId, LocalDate dueDate) {
        require(dueDate != null, "Tanggal jatuh tempo wajib diisi.");
        var candidates = billPreview(value, residentId);
        require(!candidates.isEmpty(), "Tidak ada tagihan baru. Periksa periode, status penghuni, atau tagihan yang sudah ada.");
        for (var candidate : candidates) {
            var resident = lockedResident(candidate.getId());
            require(resident.isActive() && !bills.existsByResidentIdAndPeriod(resident.getId(), value),
                "Data berubah atau tagihan sudah dibuat. Muat ulang daftar nominal.");
            var bill = new Bill();
            bill.setResident(resident);
            bill.setPeriod(value);
            bill.setAmount(resident.getRoom().getRate()); // Salin tarif: perubahan tarif tidak mengubah tagihan lama.
            bill.setDueDate(dueDate);
            bills.save(bill);
        }
        return candidates.size();
    }

    @Transactional
    public void pay(Long id, LocalDate date, String method) {
        var bill = lockedBill(id);
        require(!bill.isPaid(), "Tagihan sudah lunas dan tidak dapat dibayar ulang.");
        require(date != null && !date.isAfter(LocalDate.now()), "Tanggal pembayaran tidak boleh setelah hari ini.");
        require(List.of("Tunai", "Transfer").contains(method), "Pilih metode Tunai atau Transfer.");
        // Nominal selalu memakai nilai tagihan. Tidak ada isian cicilan atau nominal bebas.
        bill.setPaidOn(date);
        bill.setMethod(method);
    }

    @Transactional
    public void deleteBill(Long id) {
        var bill = lockedBill(id);
        require(!bill.isPaid(), "Tagihan lunas tidak dapat diubah atau dihapus.");
        bills.delete(bill);
    }

    public List<Bill> filterBills(String period, String status, String query) {
        var keyword = query == null ? "" : query.strip().toLowerCase(Locale.ROOT);
        return bills().stream()
            .filter(b -> period == null || period.isBlank() || b.getPeriod().equals(period))
            .filter(b -> !"paid".equals(status) || b.isPaid())
            .filter(b -> !"unpaid".equals(status) || !b.isPaid())
            .filter(b -> keyword.isBlank() || b.getResident().getName().toLowerCase(Locale.ROOT).contains(keyword)
                || b.getResident().getRoom().getNumber().toLowerCase(Locale.ROOT).contains(keyword)).toList();
    }

    public List<Bill> receipts(String period) {
        var month = period(period);
        return bills().stream().filter(Bill::isPaid)
            .filter(b -> YearMonth.from(b.getPaidOn()).equals(month))
            .sorted(Comparator.comparing(Bill::getPaidOn).reversed()).toList();
    }

    public BigDecimal total(List<Bill> records) {
        return records.stream().map(Bill::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private Resident lockedResident(Long id) {
        return residents.findLockedById(id).orElseThrow(() -> invalid("Penghuni tidak ditemukan."));
    }

    private Bill lockedBill(Long id) {
        return bills.findLockedById(id).orElseThrow(() -> invalid("Tagihan tidak ditemukan."));
    }

    private void require(boolean condition, String message) {
        if (!condition) throw invalid(message);
    }

    private IllegalArgumentException invalid(String message) { return new IllegalArgumentException(message); }
}
