[app]

# App identity
title = Stellar Spectrum
package.name = stellarspectrum
package.domain = com.tharindurumesh747

# Source
source.dir = .
source.include_exts = py,png,jpg,jpeg,gif,bmp,ttf,otf,atlas,json,ogg,wav,mp3

# Version
version = 1.0.0

# Requirements
requirements = python3,kivy==2.3.0,pygame,plyer

# App icon & presplash (optional - comment out if no icon file)
# icon.filename = %(source.dir)s/assets/icon.png
# presplash.filename = %(source.dir)s/assets/presplash.png

# Orientation
orientation = portrait

# Fullscreen
fullscreen = 1

# Android config
android.permissions = VIBRATE, INTERNET
android.api = 33
android.minapi = 24
android.ndk = 25b
android.sdk = 33
android.accept_sdk_license = True
android.archs = arm64-v8a, armeabi-v7a

# Release config (leave blank for debug)
android.release_artifact = apk

# Logcat filters
android.logcat_filters = *:S python:D

# Gradle dependencies (none needed for basic pygame)
# android.gradle_dependencies =

# Build tools
p4a.branch = develop

[buildozer]
log_level = 2
warn_on_root = 1
