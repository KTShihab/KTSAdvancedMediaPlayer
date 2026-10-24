# KTS Advanced Media Player

**KTS Advanced Media Player** is a powerful, lightweight, and fully accessible Android audio player designed with deep care for **Screen Reader (TalkBack) users** as well as all music and audiobook lovers.

Every button, slider, and list item has clear spoken labels, accessibility headings, and smart double-tap/long-press actions. The app works smoothly in the background, even with the screen turned off.

---

## 📱 App Layout & Interface

The app is organized into a clean, simple, and high-contrast layout:

### 1. Top Navigation Bar (3 Main Tabs)
At the very top of the screen, you will always find three main tabs:
* **Player Tab**: Your main music and media playback screen with your storage files and full playback unit.
* **Library Tab**: Quick access to your Favorites, Bookshelf, Play History, and custom Playlists.
* **Settings Tab**: Sound effects, colorful themes, volume button gestures, cache tools, and in-app updates.

---

### 2. The Middle Area (Folders & Media Files)

When you are on the **Player Tab**, the middle section displays your storage:
* **Current Folder Display**: Shows the active folder or ZIP file path. Tapping it lets you switch between Internal Storage, SD Card, or root folders. Long-pressing shows total folder playback time.
* **Search Box**: A full-width search bar to quickly find any track or folder on your device.
* **Action Buttons**:
  * **Sort**: Sort your files by Name, Date, or Size (Ascending or Descending).
  * **Stream**: Play online radio and live audio streams directly from URLs.
  * **Queue**: View and manage your current play queue. Whenever items are added to the Queue, they are given **immediate precedence**: as soon as your currently playing track finishes (or when you tap Next), the queued tracks play right away. Once the queue is finished, playback automatically resumes your original folder at the next track without missing your place!
* **The File List**:
  * Clean list of folders and audio tracks (MP3, M4A, FLAC, WAV, AAC, OGG, OPUS, and ZIP archives).
  * Every item announces its name, duration, and file size clearly to screen readers.
  * Tapping a folder opens it. Tapping a track plays it immediately.

---

### 3. Bottom Playback Controls (The Control Unit)

At the bottom of the Player tab, you have a complete playback unit:

* **Now Playing Banner**:
  * Displays the title of the track currently playing and its folder.

* **Folder Navigation & Play Mode Row**:
  * **📁⏮ Prev Folder**: Jump to and play the previous sibling folder or ZIP archive containing audio tracks.
  * **Play Mode**: Switch between Play All, Repeat One, Repeat All, and Shuffle.
  * **Next Folder ⏭📁**: Jump to and play the next sibling folder or ZIP archive containing audio tracks.

* **Main Transport Controls**:
  * **Rewind**: Jump back by a few seconds (step size can be configured: 5s, 10s, 30s, 1m).
  * **Previous Track**: Jump to the beginning of the track or to the previous track.
  * **Play / Pause**: Start or pause audio playback.
  * **Next Track**: Skip to the next track in the folder (or next queued item if items are queued).
  * **Forward**: Jump forward by a few seconds.

* **Seek Bar & Time**:
  * Shows current elapsed time and total duration.
  * Screen readers announce progress percentages and exact times as you slide.

* **Fine Navigation & Audio Tools**:
  * **Speed**: Change playback speed smoothly from **0.5x up to 3.0x** (ideal for audiobooks, lectures, and voice notes).
  * **Jump**: Opens a time dialog to jump directly to any exact minute and second in the track.
  * **Loop (A-B Repeat)**: Set a starting Point A and ending Point B to continuously loop a specific section of a song, speech, or lesson.
  * **Bookmark**:
    * Tap to view all saved bookmarks in the active folder or ZIP.
    * Double-tap and hold (Long-press) on the Bookmark button to **Quick Save** your current position instantly without typing.
    * In the Bookmark list, **double-tap and hold on any bookmark** to delete that bookmark.
    * Tap **"Delete a Bookmark"** to turn on checkboxes (ticks) and delete multiple bookmarks together in one click.
    * Use **"Clear All Bookmarks"** to wipe all saved marks in the active folder or ZIP.

* **Bottom Audio Routing Controls (Located directly at the bottom of the playback unit)**:
  * **Audio Separation (Screen Reader Friendly)**:
    * Tap to toggle ON or OFF.
    * When turned ON, your media playback plays smoothly through your connected Bluetooth speaker or headset, while your TalkBack screen reader speaks through the phone's built-in loudspeaker without clashing!
  * **Ear-Speaker Mode**:
    * Tap to cycle between **OFF**, **ON**, and **AUTO**.
    * In **AUTO** mode, when you bring the phone near your ear, the proximity sensor automatically switches the sound to the phone's ear speaker (handset receiver) and turns the screen dark to avoid accidental touches. Moving it away switches back to loudspeaker.
    * In **ON** mode, sound always plays through the ear speaker for private listening.

---

## 📚 The Library Tab

The Library tab organizes your audio collection into four top cards:

1. **Favorites**: Songs and files you have marked with a star for quick listening.
2. **Bookshelf**: Specially built for audiobooks, long lectures, and podcasts. Remembers your exact playback position across different books.
3. **History**: Automatically records tracks and folders you have played, with filter buttons for Tracks, Folders, and Playlists.
4. **Playlists**: Create, rename, edit, and organize custom playlists.

---

## ⚙️ The Settings Tab

The Settings tab is divided into clear, organized categories:

### 🎨 1. Appearance & Themes (Skins)
Choose from **6 vibrant, high-contrast themes** designed for maximum visibility:
1. 💜 **Royal Violet** (Default neon violet and dark velvet)
2. 💚 **Emerald Cyber** (Neon mint and deep jade)
3. 💙 **Oceanic Azure** (Electric cyan and sapphire navy)
4. 🧡 **Sunset Gold** (Warm amber and mocha bronze)
5. 💖 **Neon Synthwave** (Vibrant cyber pink and magenta)
6. 🖤 **AMOLED Pure Dark** (Pitch black with crisp silver text for battery saving)

### 🎵 2. Playback & Navigation
* **Background Playback**: Keep music playing while using other apps or with the screen locked.
* **Resume Playback**: Automatically continues playing from where you last stopped when you launch the app.
* **Auto-Play Next Folder**: Automatically jumps to the next folder when the current folder finishes.
* **Show Media Folders Only**: Automatically hides non-audio folders (like camera pictures, DCIM, documents) so your library remains clean.
* **Slider Step Setting**: Change slider seek sensitivity (1%, 2%, 5%, or 10% per swipe).
* **Sleep Timer**:
  * **Persistent Mode (Permanent)**: Once you set a mode (e.g., End of Track, 15m, 30m, 1h, or custom minutes), it stays permanently active across all tracks and listening sessions until you change or turn it off. Every time you start playing, the timer automatically arms.
  * **One-Time Mode (Manual)**: Runs once for the current track or duration, and turns off completely after expiring.
  * Switch between Persistent and One-Time modes at any time, add 5 minutes to an active timer, or turn it off with one tap.

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
