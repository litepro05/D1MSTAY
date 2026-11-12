-- Bukti tabel dan jumlah data aktual pada database praktikum.
SELECT 'admin_account' AS nama_tabel, COUNT(*) AS jumlah_data FROM admin_account
UNION ALL
SELECT 'room', COUNT(*) FROM room
UNION ALL
SELECT 'resident', COUNT(*) FROM resident
UNION ALL
SELECT 'bill', COUNT(*) FROM bill
ORDER BY nama_tabel;
