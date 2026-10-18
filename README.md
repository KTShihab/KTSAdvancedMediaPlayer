# KTS Advanced Media Player

**KTS Advanced Media Player** is a powerful, lightweight, and fully accessible Android audio player designed with deep care for **Screen Reader (TalkBack) users** as well as all music and audiobook lovers.

Every single button, slider, and list item has clear spoken labels, accessibility headings, and smart double-tap/long-press actions. The app works smoothly in the background, even with the screen turned off.

---

## 📱 App Layout & Interface

The app is organized into a clean, simple, and high-contrast layout:

### 1. Top Navigation Bar (3 Main Tabs)
At the very top of the screen, you will always find three main tabs:
* **Player Tab**: Your main music and media playback screen.
* **Library Tab**: Quick access to your Favorites, Bookshelf, Play History, and Playlists.
* **Settings Tab**: Sound effects, colorful themes, volume button controls, audio routing, and in-app updates.

---

### 2. The Middle Area (Folders & Media Files)

When you are on the **Player Tab**, the middle section displays your storage:
* **Current Folder Display**: Shows the active folder or ZIP file path. Tapping it lets you switch between Internal Storage, SD Card, or root folders. Long-pressing shows total folder playback time.
* **Search Box**: A full-width search bar to quickly find any track or folder on your device.
* **Action Buttons**:
  * **Sort**: Sort your files by Name, Date, or Size (Ascending or Descending).
  * **Stream**: Play online radio and live audio streams directly from URLs.
  * **Queue**: View and manage your current play queue (reorder tracks or clear).
* **The File List**:
  * Clean list of folders and audio tracks (MP3, M4A, FLAC, WAV, AAC, OGG, OPUS, and ZIP archives).
  * Every item announces its name, duration, and file size clearly to screen readers.
  * Tapping a folder opens it. Tapping a track plays it immediately.

---

### 3. Bottom Playback Controls

At the bottom of the Player tab, you have a complete control panel:

* **Now Playing Banner**:
  * Displays the title of the track currently playing and its folder.
* **Main Transport Controls**:
  * **Play / Pause**: Start or pause audio playback.
  * **Previous Track**: Jump to the beginning of the track or to the previous track.
  * **Next Track**: Skip to the next track.
  * **Play Mode**: Switch between Sequential, Repeat All, Repeat One, and Shuffle.
* **Fine Navigation & Audio Tools**:
  * **Rewind**: Jump back by a few seconds (step size can be configured: 5s, 10s, 30s, 1m).
  * **Forward**: Jump forward by a few seconds.
  * **Speed**: Change playback speed smoothly from **0.5x up to 3.0x** (ideal for audiobooks, lectures, and voice notes).
  * **Jump**: Opens a time dialog to jump directly to any exact minute and second in the track.
  * **Loop (A-B Repeat)**: Set a starting Point A and ending Point B to continuously loop a specific section of a song, speech, or lesson.
  * **Bookmark**:
    * Tap to view all saved bookmarks in the folder or ZIP.
    * Double-tap and hold (Long-press) on the Bookmark button to **Quick Save** your current position instantly without typing.
    * In the Bookmark list, **double-tap and hold on any bookmark** to delete it.
    * Tap **"Delete a Bookmark"** to turn on checkboxes (ticks) and delete multiple bookmarks together in one click.
    * Use **"Clear All Bookmarks"** to wipe all saved marks in the active folder or ZIP.
* **Progress Bar (SeekBar)**:
  * Shows current elapsed time and total duration.
  * Screen readers announce progress percentages and exact times as you slide.
  * **Slider Step Button**: Change slider seek sensitivity (1%, 2%, 5%, or 10% per swipe).

---

## 📚 The Library Tab

The Library tab organizes your audio collection into four top cards:

1. **Favorites**: Songs and files you have marked with a star for quick listening.
2. **Bookshelf**: Specially built for audiobooks, long lectures, and podcasts. Remembers your exact playback position across different books.
3. **History**: Automatically records tracks and folders you have played, with filter buttons for Tracks, Folders, and Playlists.
4. **Playlists**: Create, rename, edit, and organize custom playlists.

---

## ⚙️ The Settings Tab

The Settings tab is divided into organized categories:

### 🎨 1. Appearance & Themes (Skins)
Choose from **6 vibrant, high-contrast themes** designed for maximum visibility:
1. 💜 **Royal Violet** (Default neon violet and dark velvet)
2. 💚 **Emerald Cyber** (Neon mint and deep jade)
3. 💙 **Oceanic Azure** (Electric cyan and sapphire navy)
4. 🧡 **Sunset Gold** (Warm amber and mocha bronze)
5. 💖 **Neon Synthwave** (Vibrant cyber pink and magenta)
6. 🖤 **AMOLED Pure Dark** (Pitch black with crisp silver text for battery saving)

### 🎵 2. Playback Behavior & Audio Routing
* **Resume Last Played on Launch**: Automatically continues playing from where you last stopped when you open the app.
* **Background Playback**: Keep music playing while using other apps or with the screen locked.
* **Ear-Speaker Mode**: Switch sound to the phone's ear speaker (handset receiver) when you bring the phone close to your ear (using proximity sensor), or force ear-speaker playback for privacy.
* **Audio Separation (Screen Reader Friendly)**:
  * When using a Bluetooth speaker or headset, your media content plays smoothly through Bluetooth, while your TalkBack screen reader stays on the phone's built-in speaker without clashing!
* **Auto-Play Next Folder**: Automatically jumps to the next folder when the current folder finishes.
* **Filter Media Folders**: Automatically hides non-audio folders (like camera pictures, DCIM, documents) so your library remains clean.

### 🎚️ 3. Audio Effects & Enhancements
* **Volume Booster (Loudness Enhancer)**: Boost audio volume up to 4000 mB for quiet recordings or low-volume voice files.
* **5-Band Equalizer**: Customize bass, midrange, and treble to match your headphones.
* **3D Virtualizer**: Adds spatial depth and stereo surround effect.
* **Skip Silence**: Automatically fast-forwards through dead silence in recordings and audiobooks.

### 🔘 4. Volume Button Shortcuts (Screen-Off Controls)
Control playback without taking the phone out of your pocket:
* **Double-click Volume Up**: Next Track
* **Double-click Volume Down**: Previous Track
* **Long-press Volume Up**: Fast Forward
* **Long-press Volume Down**: Rewind
* Works even when your phone screen is completely locked and dark!

### 🧹 5. Storage & Cache Management
* **Clear ZIP Cache**: Frees storage used by unzipped audio files.
* **Clear Shared Media Cache**: Deletes temporary files from "Open with" audio shares.
* **Clear All Temporary Cache**: One-tap cleanup to reclaim phone storage.

### 📌 6. Shortcuts & Integrations
* **Home Screen Shortcuts**: Pin 1-tap shortcuts for "Resume", "Bookshelf", "Favorites", and "History" directly to your Android home screen.
* **Google Drive Link Integration**: Open Google Drive audio links and folders directly in KTS Player.

### 🚀 7. In-App Updates
* Check for new versions with a single tap (`Check for Updates`).
* When an update is published on GitHub, the app alerts you, shows what is new, and allows you to download and install it in one click without opening a web browser.
* Toggle automatic update checking on app startup.

---

## ♿ Accessibility First (TalkBack Optimized)

* **Semantic Labels**: Every button has descriptive text and TalkBack announcements.
* **Double-Tap and Hold Actions**: Quick-actions available on long-press without cluttering the screen.
* **Haptic Vibration Feedback**: Subtle vibrations confirm button presses and gesture triggers.
* **Headings**: All sections are marked with accessibility headings for easy swipe-navigation by headings.
* **No Cutoff Controls**: Equal button weights and responsive cards ensure every element is easy to touch and explore.

---

## 📥 Download & Installation

You can download the latest official APK from the **[Releases](https://github.com/KTShihab/KTSAdvancedMediaPlayer/releases)** section of this repository.

1. Download `KTSAdvancedMediaPlayer.apk`.
2. Tap the downloaded file to install on your Android device (Android 10+ recommended).
3. Open the app and grant storage and notification permissions when prompted.

---

## 📄 License
This project is open-source and created to bring a delightful, accessible media listening experience to everyone.
