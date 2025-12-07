package top.thinapps.relaxingsounds.ui

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ImageButton
import android.widget.NumberPicker
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import top.thinapps.relaxingsounds.R
import top.thinapps.relaxingsounds.playback.SoundPlaybackService

class SoundDetailActivity : AppCompatActivity() {

    private var isPlaying: Boolean = false
    private lateinit var soundKey: String

    private lateinit var root: View
    private lateinit var toolbar: MaterialToolbar
    private lateinit var soundTitle: TextView
    private lateinit var soundDescription: TextView
    private lateinit var playPauseButton: ImageButton
    private lateinit var sleepTimerButton: View
    private lateinit var sleepTimerLabel: TextView

    private val sleepTimerHandler = Handler(Looper.getMainLooper())
    private var sleepTimerCountdownRunnable: Runnable? = null

    private var sleepTimerRemainingSeconds: Long = 0L

    // debounce tracking
    private var lastClickTime = 0L
    private val debounceDelay = 350L

    private var playbackStateReceiverRegistered: Boolean = false

    // updates playback icon state only
    private val playbackStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action != SoundPlaybackService.ACTION_PLAYBACK_STATE) return

            val key = intent.getStringExtra(SoundPlaybackService.EXTRA_CURRENT_SOUND_KEY)
            if (key == null || key != soundKey) return

            val playing = intent.getBooleanExtra(
                SoundPlaybackService.EXTRA_IS_PLAYING,
                false
            )

            isPlaying = playing

            if (!playing) {
                playPauseButton.setImageResource(R.drawable.ic_play)
                playPauseButton.contentDescription =
                    getString(R.string.sound_play_label)
                cancelSleepTimerCountdown()
            } else {
                playPauseButton.setImageResource(R.drawable.ic_pause)
                playPauseButton.contentDescription =
                    getString(R.string.sound_pause_label)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sound_detail)

        root = findViewById(R.id.sound_detail_root)
        toolbar = findViewById(R.id.toolbar)
        soundTitle = findViewById(R.id.soundTitle)
        soundDescription = findViewById(R.id.soundDescription)
        playPauseButton = findViewById(R.id.playPauseButton)
        sleepTimerButton = findViewById(R.id.sleepTimerContainer)
        sleepTimerLabel = findViewById(R.id.sleepTimerLabel)

        soundKey = intent.getStringExtra(EXTRA_SOUND_KEY) ?: SOUND_OCEAN

        setupUiForSound(soundKey)

        val launchSource = intent.getStringExtra(EXTRA_LAUNCH_SOURCE)
        if (launchSource == SOURCE_MAIN) {
            startPlayback(initial = true)
        }

        // back arrow always pauses playback
        toolbar.navigationIcon = ContextCompat.getDrawable(
            this,
            androidx.appcompat.R.drawable.abc_ic_ab_back_material
        )
        toolbar.setNavigationIconTint(
            ContextCompat.getColor(this, R.color.rs_color_on_background)
        )
        toolbar.setNavigationOnClickListener { exitAndPause() }

        // play/pause behavior with debounce and animation lock
        playPauseButton.setOnClickListener {
            if (!allowClick()) return@setOnClickListener

            disableButtonTemporarily(playPauseButton)

            animatePlayPauseIcon()

            if (isPlaying) {
                isPlaying = false
                cancelSleepTimerCountdown()
                pausePlayback()
            } else {
                startPlayback(initial = false)
            }
        }

        // sleep timer button debounce
        sleepTimerButton.setOnClickListener {
            if (!allowClick()) return@setOnClickListener
            showSleepTimerDialog()
        }
    }

    override fun onStart() {
        super.onStart()
        registerPlaybackStateReceiver()
    }

    override fun onResume() {
        super.onResume()
        lastClickTime = 0L
        requestPlaybackStateSync()

        if (isPlaying && sleepTimerRemainingSeconds > 0L) {
            startSleepTimerCountdown()
        }
    }

    override fun onStop() {
        super.onStop()
        unregisterPlaybackStateReceiver()

        if (isFinishing) resetSleepTimer()
    }

    override fun onBackPressed() {
        exitAndPause()
    }

    // debounced click check
    private fun allowClick(): Boolean {
        val now = System.currentTimeMillis()
        if (now - lastClickTime < debounceDelay) return false
        lastClickTime = now
        return true
    }

    // temporarily disables button during animation
    private fun disableButtonTemporarily(button: View) {
        button.isEnabled = false
        button.postDelayed({ button.isEnabled = true }, 350L)
    }

    // pauses playback and leaves screen
    private fun exitAndPause() {
        if (isPlaying) {
            val intent = Intent(this, SoundPlaybackService::class.java).apply {
                action = SoundPlaybackService.ACTION_PAUSE
            }
            startService(intent)
        }
        finish()
    }

    // play/pause animation
    private fun animatePlayPauseIcon() {
        playPauseButton.animate()
            .scaleX(0.96f)
            .scaleY(0.96f)
            .setDuration(80L)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    playPauseButton.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(120L)
                        .setInterpolator(AccelerateDecelerateInterpolator())
                        .setListener(null)
                        .start()
                }
            })
            .start()
    }

    // sets visuals for selected sound
    private fun setupUiForSound(key: String) {
        val (titleRes, descriptionRes, backgroundRes) = when (key) {
            SOUND_OCEAN -> Triple(
                R.string.sound_ocean_title,
                R.string.sound_ocean_subtitle,
                R.drawable.bg_sound_ocean
            )
            SOUND_RAIN -> Triple(
                R.string.sound_rain_title,
                R.string.sound_rain_subtitle,
                R.drawable.bg_sound_rain
            )
            SOUND_BROWN -> Triple(
                R.string.sound_brown_title,
                R.string.sound_brown_subtitle,
                R.drawable.bg_sound_brown
            )
            else -> Triple(
                R.string.sound_ocean_title,
                R.string.sound_ocean_subtitle,
                R.drawable.bg_sound_ocean
            )
        }

        soundTitle.setText(titleRes)
        soundDescription.setText(descriptionRes)
        findViewById<View>(R.id.soundBackground).setBackgroundResource(backgroundRes)
    }

    // starts playback
    private fun startPlayback(initial: Boolean) {
        isPlaying = true
        playPauseButton.setImageResource(R.drawable.ic_pause)

        val intent = Intent(this, SoundPlaybackService::class.java).apply {
            action = SoundPlaybackService.ACTION_PLAY
            putExtra(SoundPlaybackService.EXTRA_SOUND_KEY, soundKey)
        }
        ContextCompat.startForegroundService(this, intent)

        if (sleepTimerRemainingSeconds > 0L) startSleepTimerCountdown()
    }

    // pauses playback
    private fun pausePlayback() {
        isPlaying = false
        playPauseButton.setImageResource(R.drawable.ic_play)

        val intent = Intent(this, SoundPlaybackService::class.java).apply {
            action = SoundPlaybackService.ACTION_PAUSE
        }
        startService(intent)

        cancelSleepTimerCountdown()
    }

    // preset timer dialog
    private fun showSleepTimerDialog() {
        val optionMinutes = intArrayOf(
            0, 15, 30, 45, 60, 90, 120, 180, 240, 360, 480, 720, -1
        )

        val labels = arrayOf(
            getString(R.string.sleep_timer_off),
            getString(R.string.sleep_timer_15),
            getString(R.string.sleep_timer_30),
            getString(R.string.sleep_timer_45),
            getString(R.string.sleep_timer_60),
            getString(R.string.sleep_timer_90),
            getString(R.string.sleep_timer_120),
            getString(R.string.sleep_timer_180),
            getString(R.string.sleep_timer_240),
            getString(R.string.sleep_timer_360),
            getString(R.string.sleep_timer_480),
            getString(R.string.sleep_timer_720),
            getString(R.string.sleep_timer_custom)
        )

        val currentIndex = when (sleepTimerRemainingSeconds) {
            15L * 60L -> 1
            30L * 60L -> 2
            45L * 60L -> 3
            60L * 60L -> 4
            90L * 60L -> 5
            120L * 60L -> 6
            180L * 60L -> 7
            240L * 60L -> 8
            360L * 60L -> 9
            480L * 60L -> 10
            720L * 60L -> 11
            else -> 0
        }

        var selectedIndex = currentIndex

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.sleep_timer_title)
            .setSingleChoiceItems(labels, currentIndex) { dialog, which ->
                selectedIndex = which
                if (which == labels.lastIndex) {
                    dialog.dismiss()
                    showCustomSleepTimerDialog()
                }
            }
            .setPositiveButton(android.R.string.ok) { dialog, _ ->
                if (selectedIndex != labels.lastIndex) {
                    applySleepTimerSelection(optionMinutes[selectedIndex])
                }
                dialog.dismiss()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    // custom timer dialog
    private fun showCustomSleepTimerDialog() {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(40, 10, 40, 10)
        }

        val hourPicker = NumberPicker(this).apply {
            minValue = 0
            maxValue = 12
            value = 0
            descendantFocusability = NumberPicker.FOCUS_BLOCK_DESCENDANTS
        }

        val minutePicker = NumberPicker(this).apply {
            minValue = 0
            maxValue = 59
            value = 30
            descendantFocusability = NumberPicker.FOCUS_BLOCK_DESCENDANTS
        }

        val hourLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            addView(hourPicker)
            addView(TextView(this@SoundDetailActivity).apply {
                text = getString(R.string.sleep_timer_hours)
                textSize = 14f
                setPadding(0, 8, 0, 0)
            })
        }

        val minuteLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            addView(minutePicker)
            addView(TextView(this@SoundDetailActivity).apply {
                text = getString(R.string.sleep_timer_minutes)
                textSize = 14f
                setPadding(0, 8, 0, 0)
            })
        }

        layout.addView(hourLayout)
        layout.addView(minuteLayout)

        MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_RelaxingSounds_AlertDialog)
            .setTitle(R.string.sleep_timer_custom)
            .setView(layout)
            .setPositiveButton(android.R.string.ok) { dialog, _ ->
                val hours = hourPicker.value
                val mins = minutePicker.value
                applySleepTimerSelection((hours * 60) + mins)
                dialog.dismiss()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    // applies timer selection
    private fun applySleepTimerSelection(minutes: Int) {
        sleepTimerRemainingSeconds = minutes.toLong() * 60L
        if (sleepTimerRemainingSeconds < 0L) sleepTimerRemainingSeconds = 0L

        if (sleepTimerRemainingSeconds > 0L) {
            val hrs = sleepTimerRemainingSeconds / 3600
            val mins = (sleepTimerRemainingSeconds % 3600) / 60
            val secs = sleepTimerRemainingSeconds % 60
            sleepTimerLabel.text = String.format("%02d:%02d:%02d", hrs, mins, secs)

            if (isPlaying) startSleepTimerCountdown()
        } else {
            resetSleepTimer()
        }
    }

    // starts countdown
    private fun startSleepTimerCountdown() {
        cancelSleepTimerCountdown()
        if (!isPlaying || sleepTimerRemainingSeconds <= 0L) return

        val runnable = object : Runnable {
            override fun run() {
                if (!isPlaying || sleepTimerRemainingSeconds <= 0L) return

                sleepTimerRemainingSeconds--

                if (sleepTimerRemainingSeconds <= 0L) {
                    pausePlayback()
                    resetSleepTimer()
                    return
                }

                val hrs = sleepTimerRemainingSeconds / 3600
                val mins = (sleepTimerRemainingSeconds % 3600) / 60
                val secs = sleepTimerRemainingSeconds % 60
                sleepTimerLabel.text = String.format("%02d:%02d:%02d", hrs, mins, secs)

                sleepTimerHandler.postDelayed(this, 1000L)
            }
        }

        sleepTimerCountdownRunnable = runnable
        sleepTimerHandler.post(runnable)
    }

    // cancels timer loop
    private fun cancelSleepTimerCountdown() {
        sleepTimerCountdownRunnable?.let {
            sleepTimerHandler.removeCallbacks(it)
            sleepTimerCountdownRunnable = null
        }
    }

    // resets timer UI
    private fun resetSleepTimer() {
        sleepTimerRemainingSeconds = 0L
        cancelSleepTimerCountdown()
        sleepTimerLabel.text = getString(R.string.sleep_timer_button_label)
    }

    private fun registerPlaybackStateReceiver() {
        if (playbackStateReceiverRegistered) return
        registerReceiver(
            playbackStateReceiver,
            IntentFilter(SoundPlaybackService.ACTION_PLAYBACK_STATE)
        )
        playbackStateReceiverRegistered = true
    }

    private fun unregisterPlaybackStateReceiver() {
        if (!playbackStateReceiverRegistered) return
        unregisterReceiver(playbackStateReceiver)
        playbackStateReceiverRegistered = false
    }

    // sync request to service
    private fun requestPlaybackStateSync() {
        val intent = Intent(this, SoundPlaybackService::class.java).apply {
            action = SoundPlaybackService.ACTION_REQUEST_STATE
            putExtra(SoundPlaybackService.EXTRA_SOUND_KEY, soundKey)
        }
        startService(intent)
    }

    override fun onPause() {
        super.onPause()
        cancelSleepTimerCountdown()
    }

    override fun onDestroy() {
        super.onDestroy()
        resetSleepTimer()
    }

    companion object {
        const val EXTRA_SOUND_KEY = "sound_key"
        const val EXTRA_LAUNCH_SOURCE = "launch_source"

        const val SOURCE_MAIN = "source_main"
        const val SOURCE_NOTIFICATION = "source_notification"

        const val SOUND_OCEAN = "ocean"
        const val SOUND_RAIN = "rain"
        const val SOUND_BROWN = "brown"
    }
}
