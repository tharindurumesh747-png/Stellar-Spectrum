[app]

# (string) Title of your application
title = Stellar Spectrum

# (string) Package name
package.name = stellarspectrum

# (string) Package domain (needed for android packaging)
package.domain = org.stellar

# (string) Source code where the main.py lives
source.dir = .

# (list) Source files to include (let empty to include all the files)
source.include_exts = py,png,jpg,kv,atlas,wav,json

# (string) Application versioning (method 1)
version = 1.0.0

# (list) Application requirements
# comma separated e.g. requirements = sqlite3,kivy
requirements = python3,kivy==2.3.0,hostpython3,android,urllib3

# (str) Custom source folders for requirements
# It may be useful when developing a new project or library
# requirements.source.kivy = ../../kivy

# (list) Garden requirements
#garden_requirements =

# (list) Presplash of the application
#presplash.filename = %(source.dir)s/data/presplash.png

# (list) Icon of the application
#icon.filename = %(source.dir)s/data/icon.png

# (list) Supported orientations
# Valid values are: landscape, portrait, portrait-upside-down, all
orientation = portrait

# (bool) Indicate if the application should be fullscreen or not
fullscreen = 1

# (list) Permissions
android.permissions = VIBRATE, INTERNET

# (int) Target Android API, should be as high as possible.
# Currently Play Store requires targetSdkVersion to be at least 34
android.targetapi = 34

# (int) Minimum API your APK will support.
# API level 27 corresponds to Android 8.1 (Vivo Y93 native level)
android.minapi = 27

# (str) Android NDK version to use
android.ndk = 25b

# (bool) Use --private data directory (True) or public (False)
android.private_storage = True

# (list) The Android architectures to build for.
# Vivo Y93 is 64-bit ARM (arm64-v8a), we also support 32-bit (armeabi-v7a)
android.archs = arm64-v8a, armeabi-v7a

# (bool) Accept SDK license agreement
# This prevents build failures by auto-accepting licenses
android.accept_sdk_license = True

# (list) Gradle dependencies
# android.gradle_dependencies = 

# (bool) Enable AndroidX support
android.enable_androidx = True

# (bool) Copy library instead of making a lib/ directory
#android.copy_libs = 1

# (str) Path to Android SDK if you want to use a local copy
# REMOVED custom hardcoded paths so Buildozer can auto-download and manage them cleanly
# android.sdk_path =

# (str) Path to Android NDK if you want to use a local copy
# REMOVED custom hardcoded paths so Buildozer can auto-download and manage them cleanly
# android.ndk_path =

# (str) Path to a custom Android SDK templates directory
#android.templates = 

# (list) Android additionnal libraries to copy into libs/armeabi
#android.add_libs_armeabi = 

# (list) List of Java files to add to the android project
#android.add_src = 

# (str) Android entry point, default is to use start.py of python-for-android
#android.entrypoint = 

# (list) Pattern to exclude from the build
#android.exclude_src = 

# (list) External native libraries to include
#android.ext_libs =

# (list) List of Java jar files to add to the android project
#android.add_jars = 

# (list) List of Java aar files to add to the android project
#android.add_aars = 

# (list) Gradle plugins
#android.gradle_plugins = 

# (list) Java compiler options
#android.javac_options = 

# (list) Proguard rules file
#android.proguard_rules = 

# (str) Logcat filter to use
#android.logcat_filter = *:S python:D

# (str) Android standard boot intent
#android.boot_intent = 

# (str) Android extra boot intents
#android.extra_boot_intents = 

# (list) Android application services
#android.services = 

# (str) Android packaging options (e.g. to exclude redundant duplicate files)
#android.packaging_options = 

# (bool) If directory exists, it will delete old resources before compiling
#android.clean = True

# (bool) Use the modern gradle toolchain instead of older legacy ant/gradle tasks
android.gradle = True

[buildozer]

# (int) Log level (0 = error only, 1 = info, 2 = debug (with command output))
log_level = 2

# (int) Display warning if buildozer is run as root (0 = false, 1 = true)
warn_on_root = 1
