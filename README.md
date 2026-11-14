# Float Volume

Aplikasi Android native untuk tombol volume mengambang, dapat digeser, menampilkan tombol volume media + / -, mute, dan slider volume.

## Build APK

Buka tab **Actions** lalu cari workflow **Build Float Volume APK**. Workflow dijalankan otomatis pada push ke `main`, dan dapat dijalankan manual. Jika sukses, unduh artifact **FloatVolume-debug-apk** untuk memperoleh `app-debug.apk`.

Proyek sumber disimpan sebagai `FloatVolumeAndroid-build.tar.xz` dan diekstrak oleh workflow. Folder proyek Android di dalam arsip: `FloatVolumeAndroid`.

Izin: *Display over other apps* (`SYSTEM_ALERT_WINDOW`) dan foreground service. Tidak menggunakan layanan aksesibilitas, tidak meminta izin internet.

**APK perlu diuji di HP Android 8.0+ setelah build.**
