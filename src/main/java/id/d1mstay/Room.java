package id.d1mstay;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
public class Room {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String number;
    private BigDecimal rate;
    private boolean deleted;

    public Long getId() { return id; }
    public String getNumber() { return number; }
    public BigDecimal getRate() { return rate; }

    // Perubahan data dibatasi pada service dalam paket yang sama.
    void setNumber(String value) { this.number = value; }
    void setRate(BigDecimal value) { this.rate = value; }
    void setDeleted(boolean value) { this.deleted = value; }
    public boolean isDeleted() { return deleted; }
}
