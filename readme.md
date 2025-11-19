# Relaxing Sounds

## Changelog

### 0.1.7
- added a new local MP3 (“Soft Rain 6”) sourced from `elevenlabs.io/sound-effects/soft-rain` and placed in `res/raw/`
- updated the Rain detail screen to play the new audio using existing play/pause controls

### 0.1.6
- added a top app bar with a back arrow to the sound detail screen
- wired the toolbar navigation icon to use the system back behavior

### 0.1.5
- added a play/pause button to the sound detail screen for the Ocean Waves audio
- playback now loops and can be toggled without leaving the screen
- Rain and Brown Noise screens hide the button until their audio is added

### 0.1.4
- replaced toast-based behavior by opening a new detail screen for each sound card
- added `SoundDetailActivity` to handle per-sound playback and future customization
- moved Ocean Waves playback into the new detail screen (looping audio)
- Rain and Brown Noise now open placeholder detail screens (audio coming later)
- updated `AndroidManifest.xml` to register the new activity

### 0.1.3
- added a new local MP3 ("Ocean 3: Soft Ocean Waves") sourced from `elevenlabs.io/sound-effects/ocean` and placed in `res/raw/`
- implemented a simple looping play/pause toggle on the Ocean Waves card

### 0.1.2
- switched app theme to `Theme.Material3.DayNight.NoActionBar` to fix a launch crash caused by MaterialCardView
- cleaned up home screen layout by removing hardcoded text and adding proper string resources for all sound card titles and subtitles

### 0.1.1
- added three full-width tappable sound cards (Ocean Waves, Rain, Brown Noise)
- implemented basic on-tap behavior with placeholder toast messages
- updated main screen layout with scrolling, Material cards, and improved text hierarchy

### 0.1.0
- initial test release (signed)
- general project structure with GitHub Actions support
- added minimal `AndroidManifest.xml` with correct package and launcher activity
- added base layout (`activity_main.xml`) with title and subtitle
- added starter theme, colors, and strings
- XML-based Material UI (no Jetpack Compose)
