package id.d1mstay;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
public class Bill {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false) @JoinColumn(name = "resident_id")
    private Resident resident;
    private String period;
    private BigDecimal amount;
    private LocalDate dueDate;
    private LocalDate paidOn;
    private String method;

    public Long getId() { return id; }
    public Resident getResident() { return resident; }
    public String getPeriod() { return period; }
    public BigDecimal getAmount() { return amount; }
    public LocalDate getDueDate() { return dueDate; }
    public LocalDate getPaidOn() { return paidOn; }
    public String getMethod() { return method; }
    public boolean isPaid() { return paidOn != null; }
    public boolean isOverdue() { return !isPaid() && dueDate.isBefore(LocalDate.now()); }

    // Perubahan data dibatasi pada service dalam paket yang sama.
    void setResident(Resident value) { this.resident = value; }
    void setPeriod(String value) { this.period = value; }
    void setAmount(BigDecimal value) { this.amount = value; }
    void setDueDate(LocalDate value) { this.dueDate = value; }
    void setPaidOn(LocalDate value) { this.paidOn = value; }
    void setMethod(String value) { this.method = value; }
}
