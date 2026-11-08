# Cara instal FocusFlow di iPhone / iPad (beta)

FocusFlow untuk iOS belum ada di App Store. Versi beta dibagikan sebagai file **`.ipa` tanpa tanda
tangan**. File itu harus ditandatangani dengan Apple ID kamu sendiri (gratis) memakai **AltStore**
atau **SideStore**. Ini cara resmi yang dipakai banyak aplikasi open-source, bukan jailbreak.

**Yang dibutuhkan**

- iPhone atau iPad dengan iOS 15 atau lebih baru.
- Apple ID (boleh akun biasa, tidak perlu bayar). Sebaiknya pakai Apple ID cadangan.
- Komputer Windows atau Mac, sekali di awal (dan untuk AltStore juga setiap perpanjangan).

**Batasan Apple ID gratis**

- Aplikasi harus diperpanjang (refresh) setiap **7 hari**. Kalau lewat, aplikasi tidak bisa dibuka
  sampai di-refresh lagi. Data di dalamnya tetap aman.
- Maksimal 3 aplikasi sideload aktif sekaligus (AltStore/SideStore ikut dihitung).

## Pilihan A: AltStore (paling gampang, perlu komputer tiap refresh)

1. Di komputer, pasang **AltServer** dari [altstore.io](https://altstore.io). Di Windows juga perlu
   iTunes dan iCloud versi dari situs Apple (bukan dari Microsoft Store).
2. Sambungkan iPhone/iPad ke komputer pakai kabel, pilih **Trust / Percayai** di HP.
3. Klik ikon AltServer → **Install AltStore** → pilih perangkatmu → masukkan Apple ID.
4. Di HP: **Settings → General → VPN & Device Management** → ketuk Apple ID kamu → **Trust**.
5. **iOS 16 ke atas:** nyalakan **Settings → Privacy & Security → Developer Mode**, lalu restart.
6. Di AltStore, buka tab **Sources** → tombol **+**, lalu tempel:

   ```
   https://raw.githubusercontent.com/ignitrondev/FocusFlow/main/altstore/source.json
   ```

   Dari iPhone, link ini langsung membuka halaman tambah source:
   [altstore://source?url=https://raw.githubusercontent.com/ignitrondev/FocusFlow/main/altstore/source.json](altstore://source?url=https://raw.githubusercontent.com/ignitrondev/FocusFlow/main/altstore/source.json)
7. Buka tab **Browse**, ketuk FocusFlow, lalu pasang. Tunggu sampai ikonnya muncul di layar utama.
8. Untuk refresh tiap minggu: HP dan komputer (AltServer menyala) di Wi-Fi yang sama, lalu buka
   AltStore → **Refresh All**.

Tab **Sources** ada di AltStore 2.0 ke atas. Kalau tidak ada, perbarui AltStore dulu. Kalau tetap
ingin memasang dari file, unduh `FocusFlow-ios-unsigned.ipa` dari
[halaman rilis](https://github.com/ignitrondev/FocusFlow/releases/tag/v1.0.0-beta.4), simpan ke
Files, lalu AltStore → **My Apps** → **+**. File dari WhatsApp: buka chat → ketuk file → ikon
bagikan → **Save to Files**.

## Pilihan B: SideStore (komputer cuma sekali, refresh langsung dari HP)

SideStore butuh iOS 14 ke atas dan sedikit lebih ribet di awal, tapi setelah itu refresh bisa
dilakukan dari HP saja. Ikuti panduan resmi di [sidestore.io](https://sidestore.io) (butuh
komputer sekali untuk membuat *pairing file* dan aplikasi VPN lokal yang disebut di panduan itu).
Setelah SideStore terpasang, buka tab **Sources** → **+** dan tempel URL yang sama, atau buka
[sidestore://source?url=https://raw.githubusercontent.com/ignitrondev/FocusFlow/main/altstore/source.json](sidestore://source?url=https://raw.githubusercontent.com/ignitrondev/FocusFlow/main/altstore/source.json).
FocusFlow muncul di **Browse**.

## Setelah terpasang

- Saat pertama menekan **Start**, FocusFlow minta izin notifikasi. Pilih **Allow**, supaya ada
  bunyi dan banner saat sesi selesai, walaupun aplikasi sedang ditutup.
- Suara dan gaya notifikasi diatur di **Settings → FocusFlow → Notifications**.
- Kalau HP dalam mode **Focus/Do Not Disturb**, notifikasi bisa tidak bunyi. Tambahkan FocusFlow
  ke daftar aplikasi yang diizinkan kalau perlu.

## Keamanan

- AltStore dan SideStore hanya memakai Apple ID untuk menandatangani aplikasi di perangkatmu
  sendiri. Tetap saja, lebih aman pakai Apple ID cadangan.
- Unduh `.ipa` hanya dari orang yang kamu percaya atau dari halaman rilis GitHub
  [ignitrondev/FocusFlow](https://github.com/ignitrondev/FocusFlow). FocusFlow open-source;
  file `.ipa` dibuat otomatis oleh GitHub Actions dari kode di repo itu.
- FocusFlow tidak butuh internet dan tidak mengirim data ke mana pun.

## Kalau ada masalah

| Masalah | Solusi |
|---|---|
| "Untrusted Developer" saat membuka aplikasi | Settings → General → VPN & Device Management → Trust |
| Aplikasi langsung tertutup setelah 7 hari | Refresh di AltStore/SideStore |
| Tidak ada opsi Developer Mode (iOS 16+) | Pasang aplikasi lewat AltStore dulu, lalu opsinya muncul |
| Notifikasi tidak muncul | Settings → FocusFlow → Notifications → Allow Notifications |
| "Maximum number of apps" | Hapus salah satu aplikasi sideload lain (batas 3 untuk Apple ID gratis) |

Temukan bug? Kirim screenshot dan model HP + versi iOS ke Bima (IgnitronDev) atau buka issue di
GitHub.
