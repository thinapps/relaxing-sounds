# Relaxing Sounds

## Changelog

## 0.4.27
- added global debounce to prevent rapid double-taps on play/pause and sleep timer buttons  
- blocked user input during play/pause icon animation to avoid overlapping commands  
- improved UI responsiveness by resetting click state when the screen resumes  
- added defensive timer validation to prevent negative countdown values  

### 0.4.26
- restored correct back navigation behavior by pausing playback whenever the user leaves the sound detail screen
- added a unified `exitAndPause()` method to ensure consistent pause handling across all exit paths
- replaced direct `finish()` calls with `exitAndPause()` to prevent playback from continuing unexpectedly

### 0.4.25
- fixed remaining sleep timer drift by ensuring the countdown never restarts from service broadcasts and only resumes when the user explicitly presses play

### 0.4.24
- fixed the sleep timer losing seconds during pause by freezing the countdown instantly on button press for true zero-drift behavior

### 0.4.23
- fixed sleep timer drift that caused one or two seconds to be lost each time playback was paused and resumed
- improved sleep timer accuracy by switching to a zero-drift countdown model using exact remaining seconds
- prevented timer flicker when leaving the sound detail screen by resetting the timer only after the activity finishes
- refined pause and resume behavior so UI and playback state always stay synchronized with smooth fade transitions
- improved internal fade-in and fade-out handling in the playback service for more reliable volume changes and fewer edge cases

### 0.4.22
- improved sleep timer logic so pausing the sound now freezes the countdown instead of resetting it
- resuming playback now continues the existing timer from the remaining time
- pressing the back button resets the timer as expected
- timer expiration now pauses playback and fully resets the timer display
- refined countdown engine to store remaining seconds for accurate pausing and resuming

### 0.4.21
- custom sleep timer now uses a two-column layout with clear Hours and Minutes labels
- custom number pickers are evenly spaced and centered for a cleaner modal appearance
- NumberPicker keyboard input is disabled to avoid accidental keypad pop-ups

### 0.4.20
- updated the sleep-timer to use a clearer and more intuitive Hours + Minutes picker instead of a single large minutes selector
- added dedicated NumberPickers for selecting hours (0–12) and minutes (0–59), improving usability for long sleep durations
- ensured consistent integration with the improved `HH:MM:SS` timer format introduced in the previous version
- removed all toast pop-ups related to sleep-timer changes
- the timer display now updates instantly and clearly, making the toast messages unnecessary

### 0.4.19
- updated sleep-timer display to use a consistent `HH:MM:SS` format across all presets and custom durations
- enhanced countdown behavior to update every second for a smoother and more accurate timer experience
- improved time-formatting logic for clarity, especially for long durations (e.g., multi-hour timers)

### 0.4.18
- added a semi-transparent scrim behind the home screen cards to improve readability on bright backgrounds
- kept the photo background intact while darkening only the card region for better contrast and a more intentional layout

### 0.4.17
- replaced the stock system alarm icon in the sleep timer row with a custom sleep timer vector to better match the app’s visual style

### 0.4.16
- added a subtle dark scrim over the sound detail background image to improve text and button readability across all wallpapers
- improved home screen sound cards with stronger visual tap affordance using borderless ripple, card elevation, and compat padding, while keeping the existing scale animation

### 0.4.15
- added a dedicated brown noise audio track (Brown Noise by DigitalSpa, via Pixabay) and updated SoundPlaybackService to use the correct `brown_noise.mp3` resource instead of reusing the rain sound

### 0.4.14
- fixed a rare crash when rapidly switching between sounds by cancelling fade animations before replacing the MediaPlayer instance

### 0.4.13
- improved playback sync by broadcasting the paused state immediately when fade-out begins, removing the short UI delay that occurred when pausing from the notification tray
- updated the sound detail layout so the play button and sleep-timer controls sit lower on the screen using proper vertical-bias positioning

### 0.4.12
- added `ACTION_REQUEST_STATE` to SoundPlaybackService and implemented immediate playback-state broadcasting when requested
- updated SoundDetailActivity to call a state-sync request on resume so the play/pause button always matches the actual playback state
- fixed issue where toggling playback from the notification tray could leave the detail screen showing a stale icon

### 0.4.11
- updated SoundDetailActivity, MainActivity, and NotificationHelper to support a new `launch_source` flag that controls when auto-play should occur
- home-screen navigation now triggers auto-play via `source_main`, while notification-tray taps use `source_notification` to open the app without altering playback
- fixed the issue where tapping the notification content unintentionally toggled playback, ensuring only the play/pause action button changes audio state

### 0.4.10
- adjusted media notification behavior so the play/pause button only controls playback, while tapping elsewhere on the notification now just opens the sound detail screen without toggling audio

### 0.4.9
- removed the Stop action from the playback notification to simplify user controls  
- updated `NotificationHelper` to display only a single play/pause toggle in the media notification
- replaced invalid notification small icon with a proper monochrome vector to ensure the correct app icon appears in the Android media tray

### 0.4.8
- added playback state broadcasts from `SoundPlaybackService` whenever play, pause, or stop occurs  
- updated `SoundDetailActivity` to listen for playback state changes and keep the play/pause button in sync with system media controls  
- ensured the detail screen UI stays accurate when audio is controlled from the notification, lock screen, or Bluetooth headset  
- preserved existing fade-in, fade-out, and sleep timer behavior while improving syncing between service, notification, and UI  

### 0.4.7
- added `MediaSessionCompat.Callback` to handle system-level play, pause, and stop commands  
- fixed issue where play/pause buttons in the Android media tray were unresponsive  

### 0.4.6
- added full `MediaSessionCompat` integration to enable Android’s system media tray  
- added proper playback state updates (play, pause, stop) for system UI sync  
- notifications now support lock-screen controls and Bluetooth headset actions  
- updated `SoundPlaybackService` to broadcast active media session state  
- updated `NotificationHelper` to link notifications with the MediaSession token  
- improved overall notification behavior during fade-in and fade-out playback  

### 0.4.5
- repositioned the play button lower to restore proper visual centering on the sound detail screen
- expanded sleep timer presets with a full range of practical long-duration options: 15, 30, 45, 60, 90, 120, 180, 240, 360, 480, and 720 minutes, plus Custom
- updated timer dialog, preset mapping, and custom picker (now up to 12 hours)

### 0.4.4
- fixed back navigation behavior so both the toolbar back arrow and Android system back button now trigger the same fade-out pause as the play/pause button (no more instant stop)
- raised the “Set Timer” section higher on the screen for better balance with the play button
- increased padding and text size in the timer row to improve touch target and overall usability

## 0.4.3
- updated SoundDetailActivity to stop playback and close the screen when using either the toolbar back button or the Android system back button (sending ACTION_STOP to SoundPlaybackService)
- updated AndroidManifest.xml to request `POST_NOTIFICATIONS` so the playback notification bar reliably appears on Android 13+ once the user grants permission

### 0.4.2 
- updated AndroidManifest.xml to include both required permissions for Android 14+ foreground media playback (`FOREGROUND_SERVICE` and `FOREGROUND_SERVICE_MEDIA_PLAYBACK`)

### 0.4.1
- fixed a crash when opening a sound by adding the required media playback foreground-service permission to AndroidManifest.xml

### 0.4.0
- added a full background playback system by introducing a new foreground service (implemented in `playback/SoundPlaybackService.kt`) to handle audio, fading, and lifecycle-safe playback
- added a media-style notification with quick play/pause and stop controls, powered by the new helper class `notifications/NotificationHelper.kt`
- introduced a dedicated monochrome wave icon for status-bar branding (`res/drawable/ic_notification_small.xml`) and a new stop action icon (`res/drawable/ic_stop.xml`) to complete the notification UI
- updated `SoundDetailActivity.kt` to route all playback actions through the new service while keeping the UI and timer behavior identical to before
- updated `AndroidManifest.xml` to register the new playback service using `foregroundServiceType="mediaPlayback"`
- playback now continues when the app is backgrounded or the screen is off, with smooth fade-in/out preserved exactly as in earlier versions
- pausing playback now leaves a dismissible “Paused” notification that remains visible until swiped away or stopped

## 0.3.8
- improved sound card taps to prevent accidental double-opening of detail screens
- minor layout tweak on sound detail screen to prevent title text overflow

### 0.3.7
- fixed a bug where selecting “Custom” for the sleep timer sometimes still required tapping OK first
- improved the reliability of automatically opening the number picker on future selections of “Custom”
- updated sleep timer dialogs to use a consistent dark theme with branded app colors

### 0.3.6
- selecting “Custom” in the sleep timer now jumps directly to the minute picker without needing an extra confirm tap

### 0.3.5
- added a “Custom” option to the sleep timer with a simple minute picker (5–120 minutes)
- custom timer values now work with the existing countdown display on the sound detail screen
- no settings or preferences are stored; timer choices are used only for the current session

### 0.3.4
- sleep timer now shows a live countdown on the sound detail screen after it is set
- “Set Timer” label is automatically replaced by a `mm:ss` timer while active and restored when the timer ends or is turned off

### 0.3.3
- fixed missing back arrow on the sound detail screen and ensured it is always visible on dark backgrounds
- adjusted home screen header so the title and subtitle sit fully inside the gradient background
- removed fixed height and linked gradient size dynamically to header content for proper alignment

### 0.3.2
- centered the title and subtitle on the sound detail screen
- removed the gradient/header overlay so the background is seamless behind the toolbar
- increased size of the “Set Timer” icon and label for better visibility

### 0.3.1
- sleep timer control is now more prominently located beneath the play/pause button
- added text label next to the clock icon for clearer functionality

### 0.3.0
- added a sleep timer icon on the sound detail screen with simple presets for off, 15, 30, and 60 minutes
- wired the sleep timer to fade out and pause playback automatically when the selected duration finishes
- kept all timer behavior local to the current session with no tracking, history, or background scheduling

### 0.2.2
- refined home screen spacing and adjusted toolbar gradient height for a cleaner visual hierarchy
- updated card styling with consistent corner radius and Material3-based elevation behavior
- unified card ripple behavior with a softer ripple color across the UI
- made play/pause button sizing responsive to screen width for better appearance on all devices

### 0.2.1
- added a unified ripple effect color for cards and wired all home screen cards to use it for a more consistent touch response
- improved the play/pause button with a subtle tap bounce animation to make playback actions feel more responsive

### 0.2.0
- migrated the app fully to Material Design 3 with explicit color role assignments in colors.xml and themes.xml to eliminate mixed or default styling
- standardized dark mode surfaces across the entire UI including status and navigation bars for a more consistent appearance
- changed to a top-to-bottom toolbar gradient to improve icon and text visibility while keeping a clean minimal look

### 0.1.17
- set `app:strokeWidth="0dp"` on home screen sound cards to eliminate default dark outlines
- added a global MaterialCardView style in the app theme to keep card strokes disabled everywhere going forward

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
