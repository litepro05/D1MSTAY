package id.d1mstay;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
public class Resident {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private String phone;
    @ManyToOne(optional = false) @JoinColumn(name = "room_id")
    private Room room;
    private LocalDate moveIn;
    private LocalDate moveOut;

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getPhone() { return phone; }
    public Room getRoom() { return room; }
    public LocalDate getMoveIn() { return moveIn; }
    public LocalDate getMoveOut() { return moveOut; }
    public boolean isActive() { return moveOut == null; }

    // Perubahan data dibatasi pada service dalam paket yang sama.
    void setName(String value) { this.name = value; }
    void setPhone(String value) { this.phone = value; }
    void setRoom(Room value) { this.room = value; }
    void setMoveIn(LocalDate value) { this.moveIn = value; }
    void setMoveOut(LocalDate value) { this.moveOut = value; }
}
