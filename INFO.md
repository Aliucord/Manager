## Obtaining Patched APKs

Unfortunately, starting with Android 10, file managers cannot access external data of other apps without root.
You will have to manually download the patched APK via `adb`.

ADB command (assuming the first user profile):

```shell
$ adb pull /storage/emulated/0/Android/data/com.aliucord.manager/cache/patched/patched.apk
```

This file should only be used after the patching process has successfully completed.
If the process failed at any step before "Installing", then the APK will be unusable.

The patched APK **should not be reused on another device**. Refer to the section [below](#backing-up-aliucord)
for more information.

## Backing up Aliucord

Note: This section is about the app installation itself, not the installed plugins, themes, and settings.

The patched Aliucord APK is tailored to the current device's Android version and CPU architecture.
Reusing a patched Aliucord APK from one device on another will likely break. Backing up and
restoring on another device will likely break.

Install Aliucord **directly via Manager** on the device you wish to use it on.
