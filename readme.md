# Relaxing Sounds

## Changelog

### 0.1.16
- fixed semi-transparent title and subtitle on the home screen by repositioning the gradient overlay behind UI elements to ensure full-opacity text
- kept the subtle top gradient (`bg_toolbar_gradient.xml`) to maintain readability against varying background areas without shifting layout content
- reduced click animation duration on sound cards for a faster, more responsive feel
- slightly adjusted scale effect to improve touch feedback without noticeable delay

### 0.1.15
- added full-screen illustrated background image (`bg_home_01.png`) for the main activity, using `drawable-nodpi` so it stays crisp across devices
- wired a subtle top gradient overlay (`bg_toolbar_gradient.xml`) on the home screen to improve text/icon readability against the background artwork

### 0.1.14
- increased sound title and subtitle text sizes, centered them, and moved them further down on the sound detail screen
- kept the immersive toolbar but forced the navigation (back) icon tint to white so it stays readable

### 0.1.13
- switched from system media icons to custom Material-style play and pause vector icons
- added circular background with subtle glow for a more polished central play/pause button
- increased button size and enhanced layout for better aesthetics and easier tapping
- updated icon toggling logic to match new visuals while keeping smooth fade behavior

### 0.1.12
- replaced small text play/pause button with a large centered play/pause icon for a more immersive UI
- updated button logic to toggle icons correctly while keeping smooth audio fade behavior
- restored autoplay on screen load so audio now begins with fade-in automatically

### 0.1.11
- changed to smooth press-in/out animation to sound cards before opening detail screen
- improved relaxation feel by delaying detail screen until animation completes
- added gentle audio fade-in on play and fade-out on pause/stop in detail screen
- ensured fade-out also triggers when leaving the screen

### 0.1.10
- added ripple press effects to all sound cards on the main screen
- improved immersive design by preparing transparent floating toolbar on sound detail screen

### 0.1.9
- updated `themes.xml` to enforce a consistent dark theme (no DayNight auto-switching)
- ensured Material components now use branded primary/accent and dark background colors from theme values
- consolidated all app color values into `colors.xml` for consistent styling
- added gradient color values (`rs_color_*_start` / `end`) for sound cards into `colors.xml`
- updated gradient drawables (`bg_sound_brown.xml`, `bg_sound_ocean.xml`, `bg_sound_rain.xml`) to reference theme colors instead of hex codes
- removed all remaining hard-coded color values across the app to avoid inconsistent visuals

### 0.1.8
- added a background `ImageView` to the sound detail screen layout
- wired background drawables dynamically inside `SoundDetailActivity` based on selected sound
- gradients and/or images will be added in a future update to complete the visual design

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
