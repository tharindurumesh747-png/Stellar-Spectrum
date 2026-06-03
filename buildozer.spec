[app]
title = Stellar Spectrum
package.name = stellarspectrum
package.domain = com.tharindurumesh747
source.dir = .
source.include_exts = py,png,jpg,jpeg,gif,bmp,ttf,otf,json,ogg,wav,mp3
version = 1.0.0

# Keep requirements minimal - no pygame for now, just kivy
requirements = python3,kivy==2.3.0

orientation = portrait
fullscreen = 1

android.permissions = VIBRATE
android.api = 33
android.minapi = 24
android.ndk = 25b
android.accept_sdk_license = True
android.archs = arm64-v8a

p4a.branch = develop

[buildozer]
log_level = 2
warn_on_root = 1
