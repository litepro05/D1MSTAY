package id.d1mstay;

import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Component("display")
public class Display {
    private static final Locale ID = Locale.forLanguageTag("id-ID");

    public String money(BigDecimal value) {
        var format = NumberFormat.getNumberInstance(ID);
        format.setMaximumFractionDigits(2);
        return "Rp " + format.format(value);
    }

    public String date(LocalDate value) {
        return value == null ? "—" : value.format(DateTimeFormatter.ofPattern("dd MMM yyyy", ID));
    }

    public String month(String value) {
        return YearMonth.parse(value).format(DateTimeFormatter.ofPattern("MMMM yyyy", ID));
    }
}
