# ZX Shizuku Shell Runner

**ZX Shizuku Shell Runner** adalah aplikasi Android open-source untuk menjalankan shell script/module Android secara nyata menggunakan backend **Shizuku** (Privileged Shell) tanpa membutuhkan root dan tanpa perantara seperti Bravent.

---

## ⚡ Fitur Utama

- **Privileged Shell Backend**: Menggunakan API Shizuku resmi (`rikka.shizuku:api`) untuk menjalankan perintah dengan hak istimewa ADB langsung dari perangkat.
- **Import Module via SAF**: Mendukung impor file `.sh`, `.bash`, `.txt`, dan skrip tanpa ekstensi dari penyimpanan lokal (Downloads, Documents, atau melalui file manager seperti ZArchiver) via Android Storage Access Framework.
- **Persistent URI Permissions**: Izin akses file skrip dipertahankan (`takePersistableUriPermission`), sehingga skrip tetap dapat dibaca dan dieksekusi setelah aplikasi atau perangkat di-restart.
- **Room Database Module Manager**: Menyimpan daftar modul, riwayat eksekusi, exit code, ukuran file, tanggal impor, serta cache preview skrip.
- **Pre-Run Validation & Security Confirmation**: Memeriksa ketersediaan file skrip sebelum dieksekusi, memverifikasi status koneksi Shizuku, mendeteksi perintah berbahaya (seperti `rm -rf /`, `reboot`, atau write partition), dan menampilkan konfirmasi sebelum eksekusi.
- **Real-Time Streaming Terminal**: Menampilkan `STDOUT` dan `STDERR` secara real-time, status proses (`RUNNING`, `SUCCESS`, `FAILED`, `STOPPED`), exit code nyata, toggle auto-scroll, salin output, dan export/share log.
- **Tombol STOP Nyata**: Menghentikan proses shell aktif secara langsung (`process.destroy()`), menutup stream, dan memperbarui status menjadi `STOPPED`.
- **Direct Command Runner**: Menjalankan perintah ADB/shell cepat (seperti `id`, `getprop`, `pm list packages`, `uptime`, `cat /proc/version`) dengan tombol preset.
- **Panduan Aktivasi Shizuku**: Menampilkan status binder, UID, versi Shizuku, panduan Wireless Debugging (Android 11+), serta perintah ADB PC dengan tombol salin 1-klik.

---

## 📲 Cara Mengaktifkan Shizuku

### Metode 1: Wireless Debugging (Android 11+)
1. Buka aplikasi **Shizuku**.
2. Masuk ke **Developer Options** di pengaturan Android, aktifkan **Wireless Debugging**.
3. Pilih **Pair device with pairing code** dan masukkan kode 6 digit ke notifikasi Shizuku.
4. Buka kembali Shizuku dan tekan **Start**.
5. Buka **ZX Shizuku Shell Runner**, berikan izin Shizuku ketika diminta.

### Metode 2: Menggunakan PC / ADB
Sambungkan ponsel ke komputer dengan USB Debugging aktif, lalu jalankan perintah:
```bash
adb shell sh /sdcard/Android/data/moe.shizuku.privileged.api/start.sh
```

---

## 🛠️ Cara Build dari Source Code

Aplikasi ini menggunakan Kotlin DSL Gradle dan Jetpack Compose:

```bash
# Clone repository
git clone https://github.com/USERNAME/zx-shizuku-shell-runner.git
cd zx-shizuku-shell-runner

# Build Debug APK
./gradlew assembleDebug

# Lokasi APK:
# app/build/outputs/apk/debug/app-debug.apk
```

---

## 📂 Struktur Proyek

```
├── app/
│   ├── src/main/java/com/example/
│   │   ├── MainActivity.kt               # Entrypoint & Bottom Navigation
│   │   ├── data/
│   │   │   ├── ModuleRepository.kt       # SAF File I/O & Validasi Keamanan
│   │   │   └── db/
│   │   │       ├── ModuleEntity.kt       # Room Database Entity
│   │   │       ├── ModuleDao.kt          # Room Database DAO
│   │   │       └── AppDatabase.kt        # Room Database Singleton
│   │   ├── shizuku/
│   │   │   ├── ShizukuManager.kt         # Lifecycle & Permission Shizuku
│   │   │   └── ShellExecutor.kt          # Real Shell Process Engine
│   │   └── ui/
│   │       ├── MainViewModel.kt          # State & Business Logic
│   │       ├── screens/                  # Modules, Import, Command, Terminal, Guide
│   │       ├── dialogs/                  # Pre-Run Confirmation & View Details
│   │       └── theme/                    # Cyber Dark Material 3 Theme
│   └── AndroidManifest.xml               # Shizuku Provider & SAF configuration
└── build.gradle.kts
```

---

## 📄 Lisensi
Open-source di bawah lisensi Apache 2.0 / MIT.
