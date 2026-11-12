package id.d1mstay;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.time.*;
import java.util.*;

@Controller
public class AppController {
    private final KosService kos;

    public AppController(KosService kos) { this.kos = kos; }

    @ModelAttribute
    void common(Model model, HttpServletRequest request) {
        model.addAttribute("uri", request.getRequestURI());
        model.addAttribute("today", LocalDate.now());
        model.addAttribute("currentMonth", YearMonth.now().toString());
    }

    @GetMapping("/login")
    String login() { return "login"; }

    @GetMapping("/")
    String dashboard(Model model) {
        var rooms = kos.rooms();
        var active = kos.activeResidents();
        var unpaid = kos.filterBills(null, "unpaid", null);
        model.addAttribute("roomCount", rooms.size());
        model.addAttribute("occupied", active.size());
        model.addAttribute("vacant", rooms.size() - active.size());
        model.addAttribute("income", kos.total(kos.receipts(YearMonth.now().toString())));
        model.addAttribute("unpaidTotal", kos.total(unpaid));
        model.addAttribute("unpaid", unpaid.stream().limit(6).toList());
        return "dashboard";
    }

    @GetMapping("/rooms")
    String rooms(Model model) {
        model.addAttribute("rooms", kos.rooms());
        model.addAttribute("occupiedIds", kos.activeResidents().stream().map(r -> r.getRoom().getId()).toList());
        return "rooms";
    }

    @GetMapping("/rooms/new")
    String newRoom(Model model) {
        model.addAttribute("form", new Forms.RoomForm(null, "", null));
        return "room-form";
    }

    @GetMapping("/rooms/{id}/edit")
    String editRoom(@PathVariable Long id, Model model) {
        var room = kos.room(id);
        model.addAttribute("form", new Forms.RoomForm(room.getId(), room.getNumber(), room.getRate()));
        return "room-form";
    }

    @PostMapping("/rooms/save")
    String saveRoom(@Valid @ModelAttribute("form") Forms.RoomForm form, BindingResult errors,
                    RedirectAttributes flash) {
        if (errors.hasErrors()) return "room-form";
        try { kos.saveRoom(form); }
        catch (IllegalArgumentException | DataAccessException e) {
            errors.reject("invalid", message(e));
            return "room-form";
        }
        return success("/rooms", "Data kamar disimpan.", flash);
    }

    @PostMapping("/rooms/{id}/delete")
    String deleteRoom(@PathVariable Long id, RedirectAttributes flash) {
        return action("/rooms", "Kamar dihapus.", flash, () -> kos.deleteRoom(id));
    }

    @GetMapping("/residents")
    String residents(@RequestParam(defaultValue = "") String q, Model model) {
        var keyword = q.strip().toLowerCase(Locale.ROOT);
        model.addAttribute("residents", kos.residents().stream()
            .filter(r -> r.getName().toLowerCase(Locale.ROOT).contains(keyword)
                || r.getRoom().getNumber().toLowerCase(Locale.ROOT).contains(keyword)).toList());
        model.addAttribute("q", q);
        return "residents";
    }

    @GetMapping("/residents/new")
    String newResident(Model model) {
        model.addAttribute("form", new Forms.ResidentForm(null, "", "", null, LocalDate.now()));
        residentOptions(null, model);
        return "resident-form";
    }

    @GetMapping("/residents/{id}/edit")
    String editResident(@PathVariable Long id, Model model) {
        var r = kos.resident(id);
        model.addAttribute("form", new Forms.ResidentForm(r.getId(), r.getName(), r.getPhone(), r.getRoom().getId(), r.getMoveIn()));
        residentOptions(id, model);
        return "resident-form";
    }

    @PostMapping("/residents/save")
    String saveResident(@Valid @ModelAttribute("form") Forms.ResidentForm form, BindingResult errors,
                        Model model, RedirectAttributes flash) {
        if (!errors.hasErrors()) {
            try { kos.saveResident(form); return success("/residents", "Data penghuni disimpan.", flash); }
            catch (IllegalArgumentException | DataAccessException e) { errors.reject("invalid", message(e)); }
        }
        residentOptions(form.id(), model);
        return "resident-form";
    }

    private void residentOptions(Long id, Model model) {
        model.addAttribute("rooms", id == null ? kos.availableRooms() : List.of(kos.resident(id).getRoom()));
        model.addAttribute("lockedDate", id != null && kos.hasBills(id));
    }

    @GetMapping("/residents/{id}/checkout")
    String checkout(@PathVariable Long id, Model model) {
        model.addAttribute("resident", kos.resident(id));
        return "checkout";
    }

    @PostMapping("/residents/{id}/checkout")
    String checkout(@PathVariable Long id, @RequestParam LocalDate date, RedirectAttributes flash) {
        return action("/residents", "Penghuni tercatat keluar. Kamar kembali tersedia.", flash,
            () -> kos.checkout(id, date));
    }

    @PostMapping("/residents/{id}/delete")
    String deleteResident(@PathVariable Long id, RedirectAttributes flash) {
        return action("/residents", "Data penghuni dihapus.", flash, () -> kos.deleteResident(id));
    }

    @GetMapping("/bills")
    String bills(@RequestParam(defaultValue = "") String period,
                 @RequestParam(defaultValue = "") String status,
                 @RequestParam(defaultValue = "") String q, Model model) {
        var records = kos.filterBills(period, status, q);
        model.addAttribute("bills", records);
        model.addAttribute("total", kos.total(records));
        model.addAttribute("period", period);
        model.addAttribute("status", status);
        model.addAttribute("q", q);
        return "bills";
    }

    @GetMapping("/bills/new")
    String newBill(@RequestParam(required = false) String period,
                   @RequestParam(required = false) Long residentId, Model model) {
        var selectedPeriod = period == null ? YearMonth.now().toString() : period;
        model.addAttribute("period", selectedPeriod);
        model.addAttribute("residentId", residentId);
        model.addAttribute("residents", kos.activeResidents());
        model.addAttribute("dueDate", LocalDate.now());
        try { model.addAttribute("preview", kos.billPreview(selectedPeriod, residentId)); }
        catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("preview", List.of());
        }
        return "bill-form";
    }

    @PostMapping("/bills/create")
    String createBill(@RequestParam String period, @RequestParam(required = false) Long residentId,
                      @RequestParam LocalDate dueDate, RedirectAttributes flash) {
        try {
            int count = kos.createBills(period, residentId, dueDate);
            return success("/bills", count + " tagihan dibuat.", flash);
        } catch (IllegalArgumentException | DataAccessException e) {
            flash.addFlashAttribute("error", message(e));
            return "redirect:/bills/new";
        }
    }

    @GetMapping("/bills/{id}")
    String bill(@PathVariable Long id, Model model) {
        model.addAttribute("bill", kos.bill(id));
        return "bill-detail";
    }

    @PostMapping("/bills/{id}/pay")
    String pay(@PathVariable Long id, @RequestParam LocalDate date, @RequestParam String method,
               RedirectAttributes flash) {
        return action("/bills/" + id, "Pembayaran dicatat. Tagihan sudah lunas.", flash,
            () -> kos.pay(id, date, method));
    }

    @PostMapping("/bills/{id}/delete")
    String deleteBill(@PathVariable Long id, RedirectAttributes flash) {
        return action("/bills", "Tagihan belum lunas dihapus.", flash, () -> kos.deleteBill(id));
    }

    @GetMapping("/reports")
    String reports(@RequestParam(required = false) String period, Model model) {
        String month = period == null || period.isBlank() ? YearMonth.now().toString() : period;
        var receipts = kos.receipts(month);
        var unpaid = kos.filterBills(month, "unpaid", "");
        model.addAttribute("period", month);
        model.addAttribute("receipts", receipts);
        model.addAttribute("unpaid", unpaid);
        model.addAttribute("income", kos.total(receipts));
        model.addAttribute("unpaidTotal", kos.total(unpaid));
        return "reports";
    }

    @ExceptionHandler(IllegalArgumentException.class)
    String invalid(IllegalArgumentException e, Model model) {
        model.addAttribute("error", e.getMessage());
        return "problem";
    }

    private String action(String target, String notice, RedirectAttributes flash, Runnable work) {
        try { work.run(); return success(target, notice, flash); }
        catch (IllegalArgumentException | DataAccessException e) {
            flash.addFlashAttribute("error", message(e));
            return "redirect:" + target;
        }
    }

    private String success(String target, String notice, RedirectAttributes flash) {
        flash.addFlashAttribute("notice", notice);
        return "redirect:" + target;
    }

    private String message(Exception e) {
        return e instanceof IllegalArgumentException ? e.getMessage()
            : "Data tidak dapat disimpan. Nomor kamar, penghunian, atau tagihan mungkin sudah tercatat. Muat ulang dan periksa kembali.";
    }
}
