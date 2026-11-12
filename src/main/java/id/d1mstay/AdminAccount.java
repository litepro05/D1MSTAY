package id.d1mstay;

import jakarta.persistence.*;

@Entity
class AdminAccount {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String username;
    private String passwordHash;

    // Perubahan data dibatasi pada service dalam paket yang sama.
    public Long getId() { return id; }
    void setUsername(String value) { this.username = value; }
    public String getUsername() { return username; }
    void setPasswordHash(String value) { this.passwordHash = value; }
    public String getPasswordHash() { return passwordHash; }
}
