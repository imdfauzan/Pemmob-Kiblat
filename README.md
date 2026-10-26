# SensorKiblat 🧭✨

Aplikasi Android untuk mencari arah Kiblat dan memantau intensitas cahaya ruangan pakai *Light Sensor* secara real-time.

---

## 🌟 Fitur-Fitur

### 1. 💡 Light Sensor & Auto Dark Mode
* **Real-time Lux Monitoring**: Menampilkan intensitas cahaya lingkungan secara akurat lengkap dengan gauge visual dan animasi morphing ikon Matahari & Bulan.
* **Smart Auto Dark Mode (Hysteresis)**: Transisi tema yang smooth antara *Light Mode* dan *Dark Mode* otomatis.
* **Custom Ambang Batas**: Terdapat slider ambang batas *Dark/Light Lux* secara dinamis. Disediakan juga opsi mode manual (*Auto*, *Terang*, *Gelap*).

### 2. 🕋 Kiblat Finder (Sensor Fusion & GPS)
* Menggunakan *Sensor Fusion* (Accelerometer + Magnetometer) dengan kalkulasi *Great-Circle Bearing* ke Ka'bah.
* Menghitung sudut dari Utara Sejati secara otomatis berdasarkan sensor GPS.
* **Indikator Visual & Haptic**: Kompas dinamis yang berubah menjadi warna hijau dan ada *Haptic Feedback* saat HP tepat menghadap Kiblat.
* **Fitur Tambahan**: Deteksi kemiringan HP, jarak ke Ka'bah (km), indikator akurasi sensor, serta opsi input koordinat lokasi secara manual.

---

## 🛠️ Tech Stack & Library

* **Language**: Java (Android SDK)
* **UI Architecture**: Glassmorphism Custom Views, Material Components, AndroidX
* **Min SDK**: 24 (Android 7.0 Nougat)
* **Target SDK**: 37