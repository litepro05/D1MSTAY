package id.d1mstay;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

/** Data formulir dipisahkan dari entitas agar isian tidak langsung menimpa database. */
public final class Forms {
    private Forms() {}

    public record RoomForm(Long id,
        @NotBlank(message = "Nomor kamar wajib diisi.")
        @Size(max = 30, message = "Maksimal 30 karakter.") String number,
        @NotNull(message = "Tarif wajib diisi.")
        @DecimalMin(value = "1", message = "Tarif harus lebih dari nol.")
        @DecimalMax(value = "999999999999", message = "Tarif terlalu besar.")
        @Digits(integer = 12, fraction = 2, message = "Maksimal dua angka desimal.") BigDecimal rate) {}

    public record ResidentForm(Long id,
        @NotBlank(message = "Nama wajib diisi.")
        @Size(max = 100, message = "Maksimal 100 karakter.") String name,
        @NotBlank(message = "Nomor HP wajib diisi.")
        @Pattern(regexp = "[+0-9() -]{8,20}", message = "Isi nomor HP 8–20 karakter.") String phone,
        @NotNull(message = "Pilih kamar.") Long roomId,
        @NotNull(message = "Tanggal masuk wajib diisi.")
        @PastOrPresent(message = "Tanggal masuk tidak boleh setelah hari ini.") LocalDate moveIn) {}
}
