# D1MSTAY untuk Praktikum RPL 2

Aplikasi web pengelolaan kos dengan Java, Spring Boot, Hibernate, Thymeleaf, dan PostgreSQL. Proyek ini dibuka dan dijalankan melalui Apache NetBeans. Aplikasi diakses dari browser, bukan Java Swing.

## Menjalankan melalui NetBeans

1. File > Open Project, pilih folder D1MSTAY-NetBeans.
2. Gunakan JDK 21 atau lebih baru untuk Maven. Target kompilasi proyek adalah Java 21.
3. Pastikan database lokal PostgreSQL D1MSTAY pada port 5433 aktif. Jika diperlukan, klik dua kali doctor.cmd untuk memulai dan memeriksa database.
4. Klik kanan proyek > Run, atau tekan F6. nbactions.xml memetakan Run ke spring-boot:run.
5. Buka http://127.0.0.1:8081. Login latihan: admin / D1mstay123!
6. Hentikan server dari tombol Stop pada tab Output NetBeans setelah selesai menggunakan aplikasi.

Konfigurasi koneksi lokal berada di C:/Users/ASUS/Documents/Codex/toolchains/d1mstay-netbeans-local.properties, di luar source. PostgreSQL memakai database d1mstay_praktikum, terpisah dari proyek lama. Jangan memasukkan berkas konfigurasi rahasia tersebut ke GitHub. Untuk komputer lain, isi D1MSTAY_DB_URL, D1MSTAY_DB_USER, dan D1MSTAY_DB_PASSWORD.

## Bagian kode

- Entitas Room, Resident, Bill, dan AdminAccount: atribut privat, getter, dan method pengubah data. ID dihasilkan database.
- Repository: akses PostgreSQL melalui Hibernate/JPA.
- KosService: aturan kamar, penghuni, tagihan dan pembayaran; perubahan data memakai transaksi.
- AppController: rute HTTP, formulir dan pesan hasil tindakan.
- SecurityConfig: autentikasi admin, hash BCrypt, sesi dan logout.
- templates: halaman HTML Thymeleaf; static: CSS dan JavaScript.
- schema.sql: struktur tabel admin_account, room, resident, dan bill. Pembayaran penuh disimpan pada baris bill melalui paid_on dan method.

## Cakupan

Fitur vital tersedia: login/logout, kamar, penghuni dan keluar kos, tagihan bulanan, pembayaran penuh Tunai/Transfer, pencarian, laporan serta cetak laporan. Tidak ada akun penghuni, pemesanan daring, cicilan, denda, atau pembayaran otomatis.

LA2 membahas penerapan awal OOP, login, dashboard dan koneksi database. Pengembangan berikutnya dapat membahas fitur pengelolaan secara lebih rinci mengikuti materi pertemuan. Isi laporan harus sesuai kode dan pengujian yang benar-benar dilakukan.

Data contoh, jika dimuat untuk demonstrasi, adalah data fiktif dan bukan data penghuni nyata. Akun admin dibuat sekali pada database kosong dan disimpan sebagai hash. Kata sandi di atas hanya untuk latihan lokal.

## Pengujian dan GitHub

Clean and Build menjalankan pengujian integrasi serta membuat JAR pada target/. Data pengujian dibatalkan setelah setiap tes. Semua source Java, HTML, CSS, JavaScript, SQL, pom.xml dan nbactions.xml dapat disimpan di GitHub. Folder target/, konfigurasi rahasia dan berkas database tidak perlu diunggah.
