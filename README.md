<h1 align="center">Plume Launcher</h1>

<p align="center">
  <em>Light as a feather.</em> A minimal, ad-free home screen for Android TV and Google TV.
</p>

<p align="center">
  <a href="https://github.com/aradhyas8/Plume_Launcher/releases/latest"><img alt="Latest release" src="https://img.shields.io/github/v/release/aradhyas8/Plume_Launcher?label=release"></a>
  <img alt="Android 8.0+" src="https://img.shields.io/badge/Android-8.0%2B-3ddc84">
  <img alt="APK size" src="https://img.shields.io/badge/APK-0.4%20MB-blue">
  <a href="https://github.com/aradhyas8/Plume_Launcher/releases"><img alt="Downloads" src="https://img.shields.io/github/downloads/aradhyas8/Plume_Launcher/total"></a>
  <a href="LICENSE"><img alt="GPL-3.0 License" src="https://img.shields.io/github/license/aradhyas8/Plume_Launcher"></a>
</p>

<p align="center">
  <img src="docs/home-light.jpg" alt="Plume home screen, light theme" width="49%">
  <img src="docs/home-dark.jpg" alt="Plume home screen, dark theme" width="49%">
</p>

Plume replaces the cluttered stock home screen with the two things you actually use: your apps and what you were watching. There are no ads, no sponsored rows, no account and no tracking, and it stays out of the way of the apps it launches.

## Highlights

- **Tiny and fast.** A 0.4 MB APK with no third-party libraries. It uses about 50–70 MB of RAM and has no extra background services.
- **Home button support on Google TV.** The Home button opens Plume directly, even on devices where the system launcher can't be replaced. Holding Home still opens the Google TV dashboard.
- **Continue Watching.** Resume shows and films from Android TV's Watch Next list and from apps that publish their own "Continue Watching" channel, such as Stremio.
- **Clean apps row.** Monochrome logos (or the same logos in brand colour, or each app's own banner) in a single row, with names on focus, always or never. Rearrange it by holding OK, sort by most used, or hide apps you don't use (they stay in All apps). An optional PIN locks the layout so small hands can't rearrange it.
- **All apps under the row.** The tile at the right end of the row, or the grid icon in the top bar, opens every app that is not in the row, with the HDMI inputs. Hold OK on an app to move it or add it to the row, or to uninstall it. The row and All apps share one order.
- **Sources list.** The Sources icon in the top bar opens the list of the TV inputs. A console or set-top box that names itself over HDMI-CEC (for example "PlayStation 5") shows with its logo, and selecting it switches the TV to that input. A port with no signal says so. Hold OK on an input to put it in the apps row, and again to take it out. A remote key can also open one input.
- **Top bar you choose.** Pick which icons show at the top right, and their order: network, Sources, All apps, Android settings, Plume settings and an optional Power icon. Power turns the screen off, opens the power menu (power off, restart) or stops the running apps. Plume settings and Settings also have a tile in All apps, so you can hide their icons and still reach them.
- **Remote keys.** Change what a button on your remote does: Play/Pause, Back, Recent apps, Quick settings, the power menu, Android settings, Network settings, open an app or an input, or nothing. A key can do one thing on a press and another when you hold it.
- **Cards rows.** The row under the apps can show Continue Watching, any channel that one of your apps publishes (such as a Stremio library), or one row for each channel.
- **Optional glass look.** Frosted glass behind the tiles, the menus, the cards and the settings panel: the wallpaper, blurred. Off by default.
- **Optional memory clean-up.** Plume can end the Google TV home screen, or other apps you choose, to free memory. See [Free memory](#free-memory).
- **Languages on demand.** English is built in. French, Turkish and Arabic are text files in this repository that Plume downloads when you pick the language, and refreshes once a day. See [Language](#language).
- **Light, dark or automatic theme**, each with its own wallpaper. Add your own photos from your phone by scanning a QR code, or show a folder of photos from the TV or a USB drive that changes on a timer.
- **Optional Gallery screensaver.** Public-domain landscape paintings in 16:9, downloaded only if you choose it, or a folder of your own photos.
- **Optional weather** under the clock.
- **Updates itself.** Plume checks GitHub once a day and installs new versions from its settings (from 1.2 on). The first update asks you to allow installs from Plume.

## How it compares

Measured on a Hisense Google TV (Android 12, 1.8 GB RAM):

| | Plume | Projectivy Launcher | Google TV launcher |
|---|---|---|---|
| APK size | **0.4 MB** | ~10–14 MB | ~25 MB |
| RAM while running | **~50–70 MB** | ~65–70 MB | ~93 MB, plus a ~22 MB helper process |
| Extra background services | **None** | 2 | 1 always-on helper |
| Return to home from an app | **~95 ms** | – | – |

## Requirements

- Android TV or Google TV running **Android 8.0 (Oreo) or newer**
- A way to sideload an APK: the *Downloader* app, a USB drive with a file manager, or `adb`

## Installation

1. Download the latest `Plume-<version>.apk` from the [Releases](https://github.com/aradhyas8/Plume_Launcher/releases/latest) page.
2. Install it on your TV, using any of these:
   - **Downloader app:** enter the code **`2125356`**. It always points to the latest release.
   - **USB drive:** copy the APK over and open it with a file manager.
   - **adb:** `adb install Plume-<version>.apk`
3. Open **Plume** from your apps. Its settings panel opens automatically on first launch.

### First-run setup

**Plume settings → Home button** has two setup rows:

| Setting | What to do |
|---|---|
| **Home button** | Press OK. In the Accessibility screen that opens, select **Plume: Home button** and turn it on. |
| **Default launcher** | Press OK and choose **Plume** in the dialog. |

Allow the **TV listings** permission when asked; Continue Watching needs it.

> [!NOTE]
> Android shows the same generic warning for every accessibility service. Plume's service listens for the Home key, for the Google TV home screen appearing and for the keys you set under **Remote keys**. It sees only the windows of the Google TV home screen and of Android Settings, never other apps. It reads the Settings screens only while it stops apps for you (see [Free memory](#free-memory)).

> [!TIP]
> **Android 13 and newer:** Android blocks sideloaded apps from Accessibility at first. Open **Settings → Apps → Plume**, choose **Allow restricted settings** from the menu, then turn the service on.

### Optional: grant one extra permission over adb

```sh
adb shell pm grant dev.plume android.permission.WRITE_SECURE_SETTINGS
```

This lets Plume:
- set the Gallery screensaver from its own settings
- turn its Home button service back on automatically if the app is ever force-stopped

Everything else works without it.

### Home button on Android 13 and newer

Android 13 and newer grey out the Accessibility switch for apps installed outside the Play Store, and most Google TV builds hide the **Allow restricted settings** menu that would unlock it. If Plume's setup reaches the ADB step, unlock it once from a computer (or a phone ADB app) on the same network:

1. On the TV, open **Settings → System → About** and press **Android TV OS build** seven times to turn on Developer options.
2. In **Settings → System → Developer options**, turn on **Network debugging** (called **USB debugging** on some TVs).
3. Run these, using the address Plume shows:

   ```sh
   adb connect <tv-address>:5555
   adb shell pm grant dev.plume android.permission.WRITE_SECURE_SETTINGS
   ```

4. Accept the debugging prompt on the TV, then choose **Try again** in Plume. Plume turns its Home button service on by itself.

## Using Plume

Open **Plume settings** with the sliders icon in the top bar, or with the **Plume settings** tile in All apps. Settings are in sections: **Appearance**, **App row**, **Bottom row**, **Weather**, **Screensaver**, **Home button** and **Memory**, then **Remote keys**, **Top bar**, **Updates** and **Lock layout**.

| Action | How |
|---|---|
| Rearrange apps | Hold **OK** on an app, or use **Plume settings → App row → Edit apps row** |
| Hide a Continue Watching item | Hold **OK** on its card, then **Remove from Continue Watching** |
| Remove an app from the row | Hold **OK** on it, then **Remove from row** (it stays in *All apps*) |
| Uninstall an app | Hold **OK** on it in the row, then **Uninstall**. Android asks you to confirm. Plume offers this only for apps Android lets you remove. |
| Open *All apps* | Select the tile at the right end of the row, or the grid icon in the top bar. Push **Left** on the first app to reach the tile at once. |
| Pin an app from *All apps* to the row | Open **All apps**, hold **OK** on the app, then **Add to row** (this also unhides it) |
| Choose which apps feed Continue Watching | **Plume settings → Bottom row → Apps** |
| Hide apps from the row | **Plume settings → App row → Apps in row** |
| Show app names always, on focus or never | **Plume settings → App row → App names** |
| Use the Bing picture of the day as wallpaper | **Plume settings → Appearance → Light mode wallpaper** or **Dark mode wallpaper**, then **Bing picture of the day** |
| Switch between monochrome icons, colour icons and app banners | **Plume settings → App row → Icon style** |
| Choose the icons at the top right | **Plume settings → Top bar** |
| Change the language | **Plume settings → Language** |
| Change what a remote key does | **Plume settings → Remote keys** |
| Turn on the glass look | **Plume settings → Appearance → Glass** |
| Update Plume | **Plume settings → Updates** |
| Lock the layout with a PIN | **Plume settings → Lock layout** |
| Open the Google TV dashboard | Hold the **Home** button |

### Top bar

**Plume settings → Top bar** lists the icons. Press **OK** to show or hide one, and **Left** or **Right** to move it. The **Power** icon is hidden at first. Select it to turn the screen off, open the power menu (power off, restart) or stop the running apps. If you hide **Plume settings** or **Settings**, you still reach them from the tiles of the same name in All apps.

### Cards rows

**Plume settings → Bottom row** sets what shows under the apps.

- **Cards row** is **One row** (the default), **One row per channel** or off. With one row per channel, push **Down** on a card to see the next channel, and **Up** to go back.
- **Channels** picks a channel from your apps for the first row, such as a Stremio library. **Row name** renames that row.
- **Channel rows** picks the channels you can show next to Continue Watching. With **One row**, select the row's title and push **Left** or **Right** to switch channel. With **One row per channel**, each one has its own row.
- **Apps** picks the apps that feed Continue Watching.

### Remote keys

**Plume settings → Remote keys → Add a key**, then press the key on your remote. Back, OK, the arrows, Home, Power and the volume keys can't be changed, so the remote can't be locked out and keeps its volume. For each key, choose what a **Press** does and what a **Hold** does: Play/Pause, Next, Previous, Stop, Rewind, Fast-forward, Back, Recent apps, Notifications, Quick settings, Power menu, Android settings, Network settings, **Open an app or input…**, **Stop running apps**, or **Do nothing**. A key you set stops doing its usual job, and **Remove this key** gives it back. This needs **Plume: Home button** turned on in Accessibility settings.

### Free memory

**Plume settings → Memory** has three rows:

- **Stop other home screens** ends the Google TV home screen each time Plume starts. It is on by default.
- **Apps to stop** picks more apps for Plume to end.
- **Stop running apps** ends them now.

Android 14 lets an app end only itself, so Plume does what you would do: it opens each app's page in Settings and taps **Force stop**. A black screen covers this and shows the progress and the memory freed. Push **Back** to cancel. This needs **Plume: Home button** turned on in Accessibility settings. After an update, turn it off and on again once.

### Glass

**Plume settings → Appearance → Glass** blurs the wallpaper behind the tiles, the menus, the cards, All apps and the settings panel, like frosted glass. It is off by default. Plume blurs a small copy of the wallpaper once, so the look costs no extra work while you browse.

### Language

English is in the app. For another language, select **Plume settings → Language**. Plume downloads that language from this repository (`lang/<code>.txt`, about 10 KB), keeps it on the TV and uses it from then on. Picking another language deletes the old one, so one language is kept at a time. On its first start, Plume asks GitHub for the file of your TV's language: if it is there, Plume downloads it and uses it, if not it stays in English. A TV in English asks for nothing, and any other TV asks once a day at most until a file exists. Once a day it asks GitHub whether the file changed, so a fixed text arrives with no new version. Without a network, Plume keeps the saved language, or shows English.

The languages on offer come from `lang/index.txt`, which Plume downloads too. **A new language needs no new version of Plume:** add `lang/<code>.txt` and its line in `lang/index.txt` (`tools/lang.py new <code> <Name>` starts both), and the language shows in the list. A TV that already has the index sees it at the next daily check, or when the Language page opens.

To add or fix a translation, edit `lang/<code>.txt`. Each line is a key, a tab and the text. A key the file lacks shows in English, and a key Plume does not know is ignored. Never change what a key means: add a new key. Run `tools/lang.py check` to list what is missing in each language. The unit tests check every file in `lang/`, a new one too, with no change to the tests. They stop a file that has an unknown key, an empty text or the wrong `%d` and `%s` markers, and a file with no line in the index (or a line with no file).

### Custom wallpapers

Open **Light mode wallpaper** or **Dark mode wallpaper** in Plume settings and choose **Add from phone…**. Scan the QR code with your phone (it must be on the same Wi-Fi as the TV), pick a photo and upload it. It becomes the wallpaper straight away. The TV turns it upright, crops it to the screen around its centre and saves a small copy. The upload page only works while that screen is open. **Remove a photo…** in the same list deletes one.

You can also copy `.jpg`, `.png` or `.webp` files to `Android/data/dev.plume/files/wallpapers/` yourself:

```sh
adb push my-wallpaper.jpg /sdcard/Android/data/dev.plume/files/wallpapers/
```

A gentle fade is added automatically so the rows stay readable (**Plume settings → Appearance → Photo fade** turns it off).

### Photo folders

Choose **Choose a folder on this TV…** in the wallpaper list to use a whole folder of photos, from the TV's storage or a USB drive. Plume asks for access to your photos the first time. The photos take turns in shuffled order; **Plume settings → Appearance → Folder photo changes** sets how often, from every minute to once a day. The screensaver can show a folder too: **Plume settings → Screensaver → Screensaver → Your photos**.

### Lock layout

**Plume settings → Lock layout** sets a 4-digit PIN. While it's on, Plume settings and anything that changes the home screen (moving, removing or pinning apps, hiding shows) ask for the PIN first, and so do the Wi-Fi and Android settings icons in the top bar. Opening apps works as usual. One PIN unlocks changes for five minutes, until you leave the home screen. Use the arrows or the remote's number keys to enter it.

The lock keeps the layout safe, it isn't parental control: apps can still be opened by voice search or from Android settings. Forgot the PIN? **Android Settings → Apps → Plume → Clear data** removes it, along with your layout and settings.

### Gallery screensaver

Select **Plume settings → Screensaver → Screensaver** to switch to Gallery. Plume downloads the paintings (4.7 MB) from this repository's releases and stores them privately. Switching back to another screensaver deletes them again, so they only take space while you use them.

## Privacy

Plume has no analytics, no ads and no account. It only connects to the internet to:
- load poster images for your Continue Watching items
- check this repository's GitHub releases for a new version, once a day, and download it when you choose to update
- download the Gallery paintings from this repository, **only if you choose that screensaver**
- get the Bing picture of the day (about 300 KB), **only if you choose it as your wallpaper**. Plume sends a request to [bing.com](https://www.bing.com) when the home screen opens: once a day, or once an hour until the picture has changed that day, because Bing changes it at a set time for each country. Bing sees the TV's IP address and its language and country, which Plume sends to get the picture for your country. Plume keeps one picture and deletes it when you choose another wallpaper.
- download a language, **only if you pick one**, or, if your TV is not in English and you chose nothing, once a day at most until a file for its language exists. Plume asks `raw.githubusercontent.com` for the list of languages (a few lines) and for one text file of about 10 KB, and once a day asks whether they changed. GitHub sees the TV's IP address.
- fetch the weather, **only if you turn it on**. It looks up your approximate location from your IP address via [ipwho.is](https://ipwho.is), then gets the forecast from [Open-Meteo](https://open-meteo.com).

**Add from phone…** runs a small upload page on the TV itself, reachable only on your home network and only while that screen is open. Photos go straight from your phone to the TV and nowhere else.

## Troubleshooting

<details>
<summary><b>The Home button still opens the old home screen</b></summary>

Check that **Plume settings → Home button** shows **On**. If the app was force-stopped, Android turns its accessibility service off; open Plume once (or grant the optional adb permission above) to restore it.
</details>

<details>
<summary><b>Continue Watching is empty</b></summary>

Make sure the TV listings permission is allowed (Android **Settings → Apps → Plume → Permissions**), and that the app you watch publishes to Android TV's Watch Next list. Check **Plume settings → Bottom row → Apps** too.
</details>

<details>
<summary><b>The screensaver option says it needs an adb permission</b></summary>

Google TV hides third-party screensavers from its own settings, so Plume needs the one-time `WRITE_SECURE_SETTINGS` grant shown above to switch it for you.
</details>

## Building from source

```sh
git clone https://github.com/aradhyas8/Plume_Launcher.git
cd Plume_Launcher
./gradlew assembleRelease
```

The APK is written to `app/build/outputs/apk/release/`. Without a `keystore.properties` file, release builds are signed with your local debug key, which is fine for installing your own build.

## Contributing

Bug reports, design ideas, translations and pull requests are welcome. Plume stays small on purpose, so please read [CONTRIBUTING.md](CONTRIBUTING.md) first: it lists what's in and out of scope and how to send a change. Open an issue before starting on a feature.

## Credits

- **Screensaver paintings** (public domain, scans via Wikimedia Commons; files in [`screensaver/`](screensaver)):
  - Frederic Edwin Church: *The Heart of the Andes*, *The Icebergs*, *Chimborazo*, *Cotopaxi*
  - Ivan Shishkin: *Rye*
  - Jasper Francis Cropsey: *Autumn on the Hudson River*
  - Martin Johnson Heade: *Thunder Storm on Narragansett Bay*
- **Wallpapers:** "Canada" by Thomas Curryer, via [Unsplash](https://unsplash.com/@tawc20) (CC0)
- **App logos:** [Simple Icons](https://simpleicons.org) (CC0). All trademarks belong to their respective owners.
- **HDMI input icons:** [Material Symbols](https://fonts.google.com/icons) by Google (Apache 2.0)

## License

Copyright (C) 2026 aradhyas8

Plume Launcher is free software, released under the [GNU General Public License v3.0](LICENSE). You may use, modify and redistribute it, but any distributed version, modified or not, must also be released under the GPL-3.0 with its full source code.
