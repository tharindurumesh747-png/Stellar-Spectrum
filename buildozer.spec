[app]
title = Stellar Spectrum
package.name = stellarspectrum
package.domain = com.tharindurumesh747
source.dir = .
source.include_exts = py,png,jpg,jpeg,ttf,json,ogg,wav
version = 1.0.0

# Use stable kivy 2.1.0 — known to work with p4a 2023.2.14
requirements = python3,kivy==2.1.0

orientation = portrait
fullscreen = 1

android.permissions = VIBRATE
android.api = 33
android.minapi = 24
android.ndk = 25b
android.accept_sdk_license = True
android.archs = arm64-v8a

[buildozer]
log_level = 2
warn_on_root = 1
