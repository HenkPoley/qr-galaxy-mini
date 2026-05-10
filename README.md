# QR Galaxy

For @SorroundSilence (Noob⁰⁴).

A tiny QR code reader for Android 2.3.6 Gingerbread, built for the Samsung Galaxy Mini S5570.

The release APK is built against Android API 10 and shrunk with ProGuard.

## Build

```sh
./build.sh
```

The APK is written to:

```text
build/qr-galaxy-debug.apk
```

## Install on the phone

1. On the Galaxy Mini, enable `Settings > Applications > Development > USB debugging`.
2. Connect the phone over USB.
3. Run:

```sh
./install.sh
```

If Android asks about installing from unknown sources, enable `Settings > Applications > Unknown sources`.

If USB debugging does not show the phone in `adb devices`, copy `build/qr-galaxy-debug.apk` to the SD card, open it with the phone's file manager, and accept the install prompt.
