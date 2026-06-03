[app]
title = Stellar Spectrum
package.name = stellarspectrum
package.domain = com.tharindurumesh747
source.dir = .
source.include_exts = py,png,jpg,jpeg,gif,bmp,ttf,otf,json,ogg,wav,mp3
version = 1.0.0
requirements = python3,kivy==2.2.1,pygame
orientation = portrait
fullscreen = 1

android.permissions = VIBRATE
android.api = 33
android.minapi = 24
android.ndk = 25b
android.ndk_path = /home/runner/android-sdk/ndk/android-ndk-r25b
android.sdk_path = /home/runner/android-sdk
android.accept_sdk_license = True
android.archs = arm64-v8a

p4a.branch = develop

[buildozer]
log_level = 2
warn_on_root = 1
