# Float Volume

Aplikasi Android native Java untuk mengontrol **volume media** tanpa memakai tombol fisik.

## Fitur

- Bubble mengambang yang dapat digeser dan diketuk untuk menampilkan panel.
- Volume +, Volume −, Mute/Unmute, dan slider volume media.
- Pengaturan ukuran bubble (44–88 dp) dan transparansi (40–100%).
- Foreground service dengan notifikasi dan tombol Matikan.
- Menggunakan izin **Display over other apps**, bukan Accessibility Service.
- Tidak memerlukan akses internet atau akun.

## Unduh APK

1. Buka [GitHub Actions](https://github.com/Nipzy12/Float-Volume/actions/workflows/build-apk.yml).
2. Pilih run yang berstatus **Success**.
3. Pada bagian **Artifacts**, unduh **FloatVolume-debug-apk**.
4. Ekstrak ZIP untuk mendapatkan `app-debug.apk`, lalu instal di Android 8.0+.

Build pertama yang berhasil: [run #43571565530](https://github.com/Nipzy12/Float-Volume/actions/runs/43571565530). Build ini menghasilkan **debug APK**, bukan APK rilis bertanda tangan untuk Google Play.

## Cara pakai

1. Jalankan Float Volume.
2. Ketuk **Izinkan tampil di atas aplikasi lain**, lalu aktifkan izin overlay untuk aplikasi.
3. Kembali dan ketuk **Aktifkan tombol mengambang**.
4. Ketuk bubble untuk menampilkan panel, geser bubble untuk memindahkan posisi.
5. Sesuaikan ukuran dan transparansi dari halaman aplikasi. Pengaturan berlaku saat service diaktifkan ulang.

## Kode sumber

Kode tersedia langsung di repository: `app/src/main/java/com/lumavun/floatvolume/`, bersama `AndroidManifest.xml` dan berkas konfigurasi Gradle.

Build otomatis menggunakan GitHub Actions, Android SDK 35, Android Gradle Plugin 8.7.3, Gradle 8.9, dan Java 17:

```bash
gradle :app:assembleDebug --no-daemon
```

Output: `app/build/outputs/apk/debug/app-debug.apk`.

**Catatan:** Build CI berhasil, tetapi perilaku overlay dan kompatibilitas OEM tetap perlu diuji pada ponsel fisik. Volume yang dikendalikan adalah volume media, bukan volume telepon atau alarm.
