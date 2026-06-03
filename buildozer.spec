[app]

# App identity
title = Stellar Spectrum
package.name = stellarspectrum
package.domain = com.tharindurumesh747

# Source
source.dir = .
source.include_exts = py,png,jpg,jpeg,gif,bmp,ttf,otf,json,ogg,wav,mp3

# Version
version = 1.0.0

# Requirements - keep minimal for first successful build
requirements = python3,kivy==2.2.1,pygame

# Orientation
orientation = portrait
fullscreen = 1

# Android config
android.permissions = VIBRATE
android.api = 33
android.minapi = 24
android.ndk = 25b
android.accept_sdk_license = True
android.archs = arm64-v8a

# Gradle
android.gradle_dependencies =

# p4a
p4a.branch = develop

# Logcat
android.logcat_filters = *:S python:D

[buildozer]
log_level = 2
warn_on_root = 1
