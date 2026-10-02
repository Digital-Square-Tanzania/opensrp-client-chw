# Native libraries on 16 KB Android devices

The APK must satisfy both ELF segment alignment and APK ZIP alignment. A newer
NDK setting alone does not rebuild native code inside dependency AARs.

PR #1044 originally included Realm 4.1.1 through Kujaku 0.9.0. Its ARM64 RELRO
segment failed the Android 17 emulator's compatibility check. The
`release/play-store` branch already replaces that dependency with a small
stateless Kujaku adapter backed by Mapbox 8.3.3; this patch carries over those
five adapter classes and removes the original Kujaku network receiver.

The adapter has the same limitations as the Play release: Kujaku's Realm-backed
offline services, floating current-location button, location-marker wrapper,
point editing and dynamic Kujaku layers are not provided. The app's existing
Mapbox style, camera and rendered-feature click handling remain available.

Both branches previously used SQLCipher 4.13.0. Its ARM64 LOAD segments were
16 KB aligned, but its RELRO end was not. SQLCipher 4.19.1 adds the
`common-page-size` linker flag and its packaged ARM64 binary passes both checks.
This version requires **Android 6.0 / API 23**, raising the former API 22 minimum.
The updated build uses compile SDK 36, AGP 8.13.2, Kotlin 2.3.10 and Gradle 8.13
from the Play release. Target SDK stays at 35 for this PR.

All variants package `armeabi-v7a` and `arm64-v8a`, matching the release branch's
production ABI policy. Legacy x86, x86_64, armeabi and MIPS binaries are excluded.
Use an ARM emulator for debug testing. The 16 KB requirement applies to 64-bit
libraries; ARM32 devices continue using their normal page size.

After building, run:

```sh
python3 scripts/check_16kb_alignment.py \
  opensrp-chw/build/outputs/apk/nacp/debug/*.apk \
  opensrp-chw/build/outputs/apk/nacp/release/*.apk
```

The checker validates every packaged ARM64/x86_64 library's LOAD alignment,
LOAD offset/address congruence, RELRO end and uncompressed ZIP offset. It fails
on incompatible or malformed libraries. The existing Android CI workflow runs
it after a successful build; that workflow's existing branch filters remain
unchanged.

The native database regression test creates a disposable encrypted database in
the app cache, writes and updates a row, reopens it, checks integrity, deletes the
row and removes the test database. It never uses the clinical database:

```sh
./gradlew :opensrp-chw:connectedNacpDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=org.smartregister.chw.repository.SqlCipherNativeCompatibilityTest
```

For runtime validation, use a 16 KB emulator, preserve app data when installing
the upgrade, exercise login/database reads and the ANC map, and inspect crash
logs. On Android 17, the following temporary emulator settings request strict
native-library testing:

```sh
adb -s SERIAL shell getconf PAGE_SIZE
adb -s SERIAL shell setprop bionic.linker.16kb.app_compat.enabled fatal
adb -s SERIAL shell setprop pm.16kb.app_compat.disabled true
```

Record the previous property values and restore them after testing. An app
merely launching in compatibility mode does not prove its libraries are fixed.
Do not suppress the compatibility warning with a manifest override.

References:

- [Android 16 KB compatibility and validation](https://developer.android.com/guide/practices/page-sizes)
- [SQLCipher 4.19.1 linker update](https://discuss.zetetic.net/t/sqlcipher-for-android-4-19-1/7268)
- Release adapter source commit: `16d9273c3b45fc84e7302727b3b02b79c778a5db`
- Compared release head: `db4b627455073618a7017632309a8a2926124f15`
