# Nothing ThemePicker 1.8.11 and the Lineage port

## Input and setup

The inspection ran in a Debian 13 workspace with OpenJDK 21.0.13.1. The official [Apktool installation guide](https://apktool.org/docs/install/) permits direct JAR execution. The upstream 3.0.3 release JAR matched its published SHA-256, `dbf930b076c6b9be08d57c449cacefc3bdd6b71ebd59b3066fc0e1f5b14f9423`. Both the tool and its framework cache were kept in the inspection directory.

The upload and the working copy matched SHA-256 `1286332ec0bd267ef305ab4ca1e8a1e701f3a3b040f19db95185b1641d976ee5`. The ZIP contains a root manifest, resources.arsc, two DEX files and native libraries. There are no nested split APKs or manifest split requirements. Apktool used framework package 1 and decoded resources, binary manifest and smali with the following command.

```sh
java -jar inspect/tools/apktool.jar d -p inspect/framework -o inspect/decoded inspect/input.apk
```

The manifest identifies `com.nothing.wallpaper`. Apktool reports version 1.8.11 (1081100), minimum API 35 and target API 36. The manifest records compile SDK 35. Installation declares OpenGL ES 3.0 and the required OEM feature `com.nothing.feature.OS.V2_5_Beta2`. Native effect libraries are present under arm64-v8a. The x86_64 directory contains AndroidX support libraries only. Resource and bytecode analysis establishes controls and integration points. Stock-device screenshots, animation timing, server availability and model-specific feature gates were outside the observed evidence.

## Feature inventory and implementation

Paths in the evidence column are relative to the decoded APK. Layout names refer to XML in `res/layout/`.

| Surface | Evidence in the supplied build | Lineage implementation or dependency |
| --- | --- | --- |
| Home and lock preview hub | `nt_fragment_customization_picker` contains a ViewPager2, page indicator, 24 dp wallpaper card, suggested wallpaper list and gallery footer. | Implemented handheld page indicators bound to the selected screen and a gallery footer under the stock carousel. The base branch already renders home and lock previews. The carousel follows the base provider's availability and collapse behavior. |
| Home layout, icons and colour | `fragment_grid_picker`, `nt_fragment_color_picker`, `nt_color_option_bauhaus`, Icon Pack and reset-home strings. Bytecode references `content://com.nothing.launcher.share`. | Existing Lineage grid, icon and Monet controls remain available according to device capabilities. OEM special-edition themes, third-party pack application and destructive workspace reset need a matching launcher or overlay implementation. |
| Clock face, size and alignment | `clock_picker_page`, `clock_picker_bottom_sheet`, `clock_size_radio_button_group`, left/center/right labels, multiple clock face layouts. | AOSP's picker already uses ClockRegistry for installed clocks. Additional faces require clock implementations. Alignment needs a setting observed by the actual clock renderer and its preview. |
| Lock widgets and shortcuts | `fragment_lock_screen_main` combines a drag layer, workspace, clock host, shortcut preview and fingerprint guide. `widget_picker_bottom_sheet`, `widget_picker_details` and `shortcut_picker_page` describe the editors. Expanding the widget area hides the main clock. | Existing quick-affordance slots use SystemUI. Freely placed widgets need a keyguard host and a common editor/runtime layout model. The fingerprint image is a preview affordance, not a biometric configuration. |
| Static and live wallpaper effects | `nt_effect_actions_layout`, `nt_fragment_crop_wallpaper`, `popup_effect_menu`, Atmosphere service/provider, `libblurwallpaper.so`, `libnt_effect.so`. | Implemented original fluted-glass and soft-blur static variants in Home screen → Photo effects. Choose a local image, select an effect, then use the normal preview, crop and apply screen. The pipeline caps the longest edge at 3072 px and keeps temporary previews in app cache. Live Atmosphere transitions are a separate feature. |
| Depth wallpaper | Separate foreground image in `fragment_lock_screen_main`. Depth state classes include module unavailable, off, on and depth not applicable. Strings describe cropping and incompatibility with Atmosphere. | Requires a foreground mask, crop metadata and SystemUI composition around the clock. |
| Wallpaper Studio | `fragment_ai_wallpaper_entrace` (APK spelling), `fragment_creative_selector`, `fragment_creative_result`, history and generation failure strings. | Requires an independently available inference model/service, a result store and generation UI. A backend contract and model are outstanding. |
| Weather and calendar preview | Quick Look clock layouts and bytecode URIs for Nothing weather and calendar providers. | A ROM can use its existing Smartspace feed or a weather provider it ships. Live data must follow user/profile and lock-screen visibility settings. |

## Changes by repository

The included gallery, indicator and static effects changes are contained in ThemePicker. The target is the supplied fork's `lineage-24.0` commit `d5a70ebd9cd04dd9e5a98a74efab4bea319248d9`. The WallpaperPicker2 source used to check the layout IDs and preview activity contract was Lineage commit `faf11f2e8e1e36b07e3163831d34190ab7187265`. A different AOSP release needs its own resource and API comparison.

| Repository | Changes for the remaining features | Acceptance check |
| --- | --- | --- |
| `frameworks/base/packages/SystemUI` | For clock alignment, carry the user's choice through the keyguard clock repository to `keyguard/ui/view/layout/sections/ClockSection.kt` and `keyguard/ui/preview/KeyguardPreviewRenderer.kt`. Check clock controllers that supply their own constraints. Ship original clock faces through the existing clock registry/provider mechanism. | Applied face and alignment match the editor after reboot, switching users, entering AOD, and changing notification/clock size. |
| `frameworks/base/packages/SystemUI` plus ThemePicker | For the widget editor, define a versioned layout including widget IDs, cells, spans and expanded state. Have SystemUI own per-user persistence, rendering and validation. Extend the existing `keyguard/CustomizationProvider.kt` or its companion contract with capability, query and atomic apply operations. ThemePicker stages edits until Apply. Use the actual keyguard preview renderer for placement. | Test collisions, clock hiding, removal, migration, display cutouts, fingerprint clearance and cancellation. Gate personal calendar data and actions while locked. |
| `frameworks/base/packages/SystemUI` plus wallpaper component | For depth, generate an alpha mask in image coordinates and bind it to the wallpaper identity, display, destination and crop. Render the foreground above the clock and below notifications and system affordances. Clear stale layers when wallpaper or crop changes. | Validate aspect ratios, AOD, clock legibility, rotation and wallpaper replacement. |
| `packages/apps/Launcher3` or the ROM launcher | ThemePicker's `DefaultShapeGridManager.kt` already queries `list_options` and `shape_options`, and writes `default_grid` and `shape`. Supply additional supported grids through this provider. For icon packs add pack discovery, preview and apply support to the launcher. Implement any confirmed workspace reset as a launcher transaction. | Home screen, drawer and picker report the same selection after launcher restart. Cancelling an edit preserves the user's layout. |
| `packages/apps/WallpaperPicker2` or a ROM live wallpaper app | For animated Atmosphere, implement a WallpaperService with an original renderer and a per-user source image. Expose its preview through the wallpaper picker and test the engine on lock and home destinations. Put shared crop/editor changes in WallpaperPicker2 if other picker builds consume them. | Test live transitions, surface recreation, static fallback, distinct home/lock images and process death. |
| ThemePicker and an inference service | Wallpaper Studio needs a generation interface, model, error handling, history and an apply path into the normal preview. Reuse the static image preview for generated results. | Test offline, interrupted generation, retries and history removal. |
| Product configuration / overlays | Include ThemePicker and the selected clock, launcher and wallpaper components in the product build. Reuse the ROM's existing privileged picker integration. Additional colour presets can be delivered in compatible overlays and applied through the existing theme manager. | Clean install and upgrade on the intended product. Check feature gating, light/dark mode and secondary users. |

## Verification

Apktool completed the decode on the copied APK. AAPT2 compiled the port's resource directories. The handheld overrides preserve the upstream binder IDs and view classes. A platform link, Kotlin/Soong build and device test require the matching ROM checkout. In a configured and lunched tree the module build is `m ThemePicker`. For on-device acceptance, test gallery navigation, switching between previews, both effects on portrait and landscape photos, cancel/back, rotation during rendering, and destination selection including a separate lock wallpaper.

## Manifest inventory

### Requested permissions (32)

- `android.permission.VIBRATE`
- `com.android.settings.permission.LAUNCH_SUBSETTINGS`
- `com.nothing.launcher.permission.ACCESS_SHARE_INFO`
- `com.nothing.weather.permission.ACCESS_WEATHER_INFO`
- `com.nothing.permission.READ_QUICKLOOK_SHARE`
- `android.permission.CHANGE_OVERLAY_PACKAGES`
- `android.permission.WRITE_SECURE_SETTINGS`
- `android.permission.SET_WALLPAPER_COMPONENT`
- `android.permission.READ_DEVICE_CONFIG`
- `android.permission.MODIFY_DAY_NIGHT_MODE`
- `android.permission.CUSTOMIZE_SYSTEM_UI`
- `android.permission.ACCESS_NETWORK_STATE`
- `android.permission.LAUNCH_MULTI_PANE_SETTINGS_DEEP_LINK`
- `android.permission.INTERNET`
- `android.permission.READ_MEDIA_IMAGES`
- `android.permission.RECEIVE_BOOT_COMPLETED`
- `android.permission.READ_WALLPAPER_INTERNAL`
- `android.permission.SET_WALLPAPER`
- `android.permission.SET_WALLPAPER_DIM_AMOUNT`
- `android.permission.BIND_WALLPAPER`
- `android.permission.WAKE_LOCK`
- `com.android.wallpaper.NOTIFY_ROTATING_WALLPAPER_CHANGED`
- `com.nothing.blurwallpaper.AtmosphereProvider`
- `com.nothing.permission.BIND_CARD_SERVICE`
- `android.permission.QUERY_ALL_PACKAGES`
- `android.permission.READ_SETTINGS`
- `android.permission.WRITE_SETTINGS`
- `com.nothing.soundrecorder.permission.RECORD`
- `com.nothing.wallpaper.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`
- `android.permission.WRITE_EXTERNAL_STORAGE`
- `android.permission.READ_PHONE_STATE`
- `android.permission.READ_EXTERNAL_STORAGE`

### Permissions defined by the APK (4)

- `com.nothing.wallpaper.permission.SET_WALLPAPER` — signatureOrSystem
- `com.android.wallpaper.NOTIFY_ROTATING_WALLPAPER_CHANGED` — signature
- `com.nothing.blurwallpaper.AtmosphereProvider` — signatureOrSystem
- `com.nothing.wallpaper.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` — signature

### Manifest components

| Type | Component | Explicit exported value | Authority or target |
| --- | --- | --- | --- |
| activity | `com.android.wallpaper.picker.CustomizationPickerActivity` | false | — |
| activity-alias | `com.android.wallpaper.picker.CategoryPickerActivity` | true | com.android.wallpaper.picker.CustomizationPickerActivity |
| activity-alias | `com.android.customization.picker.CustomizationPickerActivity` | true | com.android.wallpaper.picker.CustomizationPickerActivity |
| activity | `com.android.wallpaper.picker.PassThroughCustomizationPickerActivity` | false | — |
| activity | `com.android.wallpaper.picker.TrampolinePickerActivity` | false | — |
| activity | `com.android.wallpaper.picker.PreviewActivity` | omitted | — |
| activity | `com.android.wallpaper.picker.FullPreviewActivity` | omitted | — |
| activity | `com.android.wallpaper.picker.StandalonePreviewActivity` | omitted | — |
| activity | `com.android.wallpaper.picker.ViewOnlyPreviewActivity` | omitted | — |
| activity | `com.nothing.wallpaper.picker.customization.ui.activity.NTColorPickerActivity` | true | — |
| activity | `com.nothing.wallpaper.picker.edit.container.EditWallpaperActivity` | omitted | — |
| activity | `com.nothing.wallpaper.picker.edit.container.StandaloneEditWallpaperActivity` | true | — |
| activity | `com.nothing.wallpaper.picker.creative.CreativeWallpaperActivity` | true | — |
| activity | `com.nothing.wallpaper.picker.lockscreen.ui.LockScreenPreviewActivity` | true | — |
| activity | `com.nothing.wallpaper.picker.creative.AiWallpaperEntranceActivity` | true | — |
| service | `com.nothing.blurwallpaper.AtmosphereWallpaperService` | true | — |
| provider | `com.nothing.blurwallpaper.AtmosphereProvider` | true | com.nothing.blurwallpaper.AtmosphereProvider |
| activity | `androidx.slice.compat.SlicePermissionActivity` | omitted | — |
| service | `com.google.mlkit.common.internal.MlKitComponentDiscoveryService` | false | — |
| provider | `com.google.mlkit.common.internal.MlKitInitProvider` | false | com.nothing.wallpaper.mlkitinitprovider |
| activity | `com.google.android.gms.common.api.GoogleApiActivity` | false | — |
| provider | `androidx.startup.InitializationProvider` | false | com.nothing.wallpaper.androidx-startup |
| service | `androidx.room.MultiInstanceInvalidationService` | false | — |
| receiver | `androidx.profileinstaller.ProfileInstallReceiver` | true | — |
| service | `com.google.android.datatransport.runtime.backends.TransportBackendDiscovery` | false | — |
| service | `com.google.android.datatransport.runtime.scheduling.jobscheduling.JobInfoSchedulerService` | false | — |
| receiver | `com.google.android.datatransport.runtime.scheduling.jobscheduling.AlarmManagerSchedulerBroadcastReceiver` | false | — |
