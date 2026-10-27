# Changelog

Plume shows the section for its own version in **Plume settings → Updates** after an update.
After an update, a card on the home screen shows the first 4 entries. Put the 4 most interesting changes first.
The build stops if `versionName` has no section here.

Write every entry in ASD-STE100 Simplified Technical English:

- Write one idea in each sentence. Use 20 words or fewer in each sentence.
- Use the active voice and the present tense.
- Write instructions as commands: "Select **Updates**", not "**Updates** can be selected".
- Use one word for one thing. Use the names that Plume shows on the screen.
- Start each entry with "- ". Plume shows each entry as one item. It removes `**` marks.

Put changes that are not released under `## Unreleased`. Rename that heading to the version number when you release.

## Unreleased

- **Edit the top bar.** Hold **OK** on an icon at the top of the home screen. Select **Move**, **Hide** or **Add icon**. In **Move**, press Left or Right to slide the icon. Press **OK** to place it. Press Back to cancel. **Add icon** shows the icons that you hid. **Plume settings** stays on the bar, so you can always bring the icons back.
- **Power icon.** Hold **OK** on a top-bar icon and select **Add icon**, then **Power**. Select the icon to open the Android power menu. It needs the Home button turned on. If it is off, the icon offers to turn it on.
- **App animations.** An app's window grows out of the tile you press. When you leave the app, the home screen settles back out of that tile in a short fade. It works for the app tiles, for Continue watching and for **All apps**. To turn it off, select **Plume settings → Wallpaper & theme → App animations**.
- **Home returns to your app.** Press Home in an app. The home screen shows the tile of that app in focus, not the first tile. Press Home on the home screen to go back to the first tile.
- **Memory.** Plume can end other apps to free memory. Plume ends the Google TV home screen each time it starts. Select **Plume settings → Memory** to turn that off, to pick more apps, or to stop the running apps now. A key on the remote can do the same: select **Plume settings → Remote & Home button → Remote keys**. A black screen shows the progress and the memory freed while Plume taps Force stop in Settings for each app.
- **Languages on demand.** Select **Plume settings → Language**. Plume lists the languages in the repository and downloads the one you pick as a small text file. It keeps one language at a time and checks once a day for a corrected text. English stays in the app. On the first start, a TV in another language asks once a day for the file of its language, until a file exists.
- **Rows of the shows row.** Select **Plume settings → Shows row → Rows**. **Mixed** puts the shows of every source in one row. **By title** keeps one row: select its title and press Left or Right to change the source (Continue watching, Recently added…). **Per channel** gives each source (Android TV Watch Next and every channel that is on) its own row. Push Down or Up on a card to change the row. A row with nothing in it is left out. The title shows the channel and arrows to the other rows.
- **Posters near the focus only.** Plume loads the posters of the focused card and the three or four around it, and drops the others. A channel with 30 cards uses no more memory than one with four. A card whose picture is missing shows the round icon of its app.
- **All apps under the row.** Select the tile at the right end of the apps row, or the grid icon in the top bar. Push Left on the first app to reach that tile at once. The row stays and All apps opens under it, in place of the shows row. It lists the apps that are not in the row. Hold OK on one to move it, add it to the row, or open its app info.
- **Plume settings and Settings in All apps.** Both have a tile in All apps, so you can hide their icons in the top bar and still reach them. The top bar keeps at least one icon. The settings a TV saved with the fork's earlier top bar and cards rows carry over once.
- **Shows wallpaper.** Pick **Shows from your apps** in the light or dark wallpaper list. The picture of a show that your apps publish (Watch Next, or the channels you choose in **Wallpaper from**) becomes the wallpaper and changes as often as a folder of photos does. Only wide pictures count. **Featured: …** shows under the weather. Add the **Play** icon to the top bar, or give a remote key **Play the featured show**, to start it. In dark mode a black layer keeps it readable: **Darkening** is Auto (a bright picture gets more black, a very dark one is lifted a little, the others are left alone) or a fixed strength. Nothing is fetched until you pick it.
- **Remote battery.** Hold OK on a top-bar icon, select **Add icon**, then **Remote battery**. The icon shows the battery level of your Bluetooth remote and turns red at 20 % or less. Hold OK on it to choose the low-battery alert: 20 % (the default), 15 % or off. At or under that level a message shows once a day. Select it to read the level now. The first time, Android asks to allow Bluetooth: Plume needs it to read the level from the remote. It reads at most once in ten minutes while the icon is on the bar, and not at all when it is off.
- **Banner icons are easier to read.** The selected banner has a ring and grows a little more. The other banners are slightly darker.

## 1.6

### Design
- **Glass.** A frosted-glass blur behind the tiles and menus. Plume blurs the wallpaper behind the tiles, menus, cards and the settings panel. Glass is on after the update. To turn it off, select **Plume settings → Appearance → Glass**.
- **A new Plume settings.** Each section has an icon and a live preview. Under its name, each section shows its settings. While you change a setting, the part of the home screen it changes stays bright.
- **Banner icons.** Each app shows its own picture, as on Apple TV. An app with no banner gets its own colour behind a white mark. Select **Plume settings → Apps row → Icon style**.
- **Hide the Shows row.** Show only your apps and wallpaper, and press Down for your shows. Press Up to hide the row again. Select **Plume settings → Shows row → Show row**.
- **Auto follows the sun.** With **Theme** set to **Auto**, Plume changes to dark at sunset and to light at sunrise.
- **Text that fits the wallpaper.** The clock, the weather and the Continue watching text change to white or dark to match your photo.
- **Set how strong the fades are.** Select **Plume settings → Appearance → Top fade** or **Bottom fade**. Choose from 0% to 100%.
- **Colour icons on the Shows row.** Each card can show the app's logo in its own colour. Select **Plume settings → Shows row → Icons**.
- **Monochrome banner.** A fourth icon style. The pictures are grey until you select one.

### New
- **Sources.** A new icon in the top bar lists your TV inputs. A device such as a PS5 shows with its logo.
- **Remote keys.** Choose what a button on your remote does when you press it or hold it. Select **Plume settings → Remote & Home button → Remote keys**.
- **Uninstall an app.** Hold **OK** on an app in the row and select **Uninstall**.

### Easier to use
- **Setup fits your TV.** The Home button setup starts with the step that works on your TV.
- **Settings in home screen order.** The sections follow the home screen from top to bottom. Your settings do not change.
- A long app name scrolls slowly while the app is selected. It no longer runs past its tile.
- Plume shows in the app list on Xiaomi TVs.

### Fixes
- **Less memory.** Plume no longer runs twice after an update. It also keeps each picture only once.
- On a Fire TV, an HDMI tile switches to its input. Before, it opened Live TV.

## 1.5

### New
- **Pick your bottom row.** Show any channel from your apps, such as a Stremio library. Select **Plume settings → Bottom row → Channels**. The row takes the channel's name, or a name you set in **Row name**.
- **Bing wallpaper.** Use the Bing picture of the day as your wallpaper. Select **Plume settings → Appearance → Light mode wallpaper** or **Dark mode wallpaper**, then select **Bing picture of the day**.
- **New languages.** Plume is now available in English, French, Turkish and Arabic. It uses the language of your TV.
- **PIN for Wi-Fi and Android settings.** If you set a PIN, Plume asks for it before it opens Wi-Fi or Android settings from the top bar.

### Design
- **See-through tiles.** The wallpaper shows through the tiles. Menus and dialogs stay solid.
- **Smooth gradients.** The dark tiles and the wallpaper fade no longer show bands.
- **Smoother scrolling.** The app row moves more smoothly.

### Easier to use
- **Continue Watching order.** What you are watching comes before the next-episode suggestions. Plume also reads the time correctly from apps that give it in seconds.
- **App names.** When you move quickly along the app row, Plume shows an app's name only when you reach that app.

### Fixes
- Plume opens on Android 8.0 and 8.1. Before, it closed immediately after it opened.
- If your wallpaper is too big for the memory of your TV, Plume shows the default wallpaper. Before, Plume closed.
- In **All apps**, the tile in the first or last column is no longer cut off when you select it.

## 1.4

### Smaller
- **12% smaller.** Plume is now 365 KB, down from 413 KB.

### New
- **Consoles on your home screen.** Your PS5, Xbox or Nintendo Switch gets a tile with its logo. Select it to switch to that input.
- **More Continue Watching.** The limit of 3 shows for each app is gone. Each app keeps 3 places at the front.
- **Every HDMI input as a tile.** Other HDMI devices get a tile while they are on. All of them are in **All apps**.
- **What's new card.** After an update, a card shows the highlights. Select **See all changes** to read everything.

### Design
- **New dark mode.** True black, with lit edges on the selected item.
- **Plume settings, organised.** Settings are in sections. Each section shows its current settings.
- **App row position.** Put a short app row on the left, in the centre or on the right. Select **Plume settings → App row → App row position**.

### Easier to use
- **Faster settings.** Push Left or Right to change a value. Settings opens where you left it. At the top of a list, push Up to go to the bottom.
- **Weather location.** Set it by hand in **Plume settings → Weather → Weather location**.
- **Home button setup.** A guided setup makes the Home button open Plume. Select **Plume settings → Home button → Set up Home button**. **Home button** is now an on/off switch.
- **Update dot.** A red dot on the Plume settings icon tells you about an update. **Plume settings → Updates** shows what is new.

### Fixes
- After the TV starts, the first app is selected, not the network icon.
- The hold OK menu is only as wide as its items.

## 1.3

- You can use a folder of photos as your wallpaper. The folder can be on the TV or on a USB drive. Select **Plume settings → Folder photo changes** to set how often the photo changes.
- **Photo fade** removes the dark area at the bottom of your photos.
- You can use your photos as the screensaver. Select **Plume settings → Screensaver → Your photos**.
- **Lock layout** keeps the home screen safe from changes. Set a 4-digit PIN in **Plume settings → Lock layout**. Apps still open without the PIN.
- Hold OK on an app to show **Move**, **Remove from row** and **App info**.
- Hold OK on a Continue Watching card to show **Remove** and **Open app**.
- Continue Watching shows a maximum of three shows from each app.
- **Launcher settings** is now **Plume settings**. Back goes up one page.
- **Remove a photo…** deletes wallpapers that you uploaded.
- The Home button switch opens the correct settings screen on more TVs.

## 1.2.1

- Fix: with **Icon style → Colour**, some app icons stayed grey until Plume started again.

## 1.2

- Plume updates itself. It looks for a new version one time each day. Select **Plume settings → Updates** to install it.
- You can hide apps from the home row. Select **Plume settings → Apps in row**. Hidden apps stay in All apps.
- **App names** shows app names below the tiles: on focus, always or never.
- **Icon style → Colour** shows each logo in the colour of its brand.
- You can add wallpapers from your phone. Select **Add from phone…** in a wallpaper list, then scan the QR code.
- Fix: green areas no longer show in the sky of wallpapers.

## 1.1

- The APK is now 0.4 MB. It was 5.1 MB.
- The Gallery screensaver paintings are now a 4.7 MB download. Select **Plume settings → Screensaver → Gallery** to get them.
- Plume deletes the paintings when you select a different screensaver.
- Fix: Plume did not always find its Home button service.

## 1.0

- First release.
