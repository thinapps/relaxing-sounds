package top.thinapps.relaxingsounds.ui

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ImageButton
import android.widget.NumberPicker
import android.widget.TextView
import android.widget.Toast
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
    private var sleepTimerRunnable: Runnable? = null
    private var sleepTimerDurationMs: Long = 0L
    private var sleepTimerEndRealtime: Long = 0L
    private var sleepTimerCountdownRunnable: Runnable? = null

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

        // auto start playback via service
        startPlayback(initial = true)

        // immersive toolbar + themed nav arrow tint
        toolbar.setBackgroundColor(android.graphics.Color.TRANSPARENT)
        toolbar.elevation = 0f
        toolbar.navigationIcon = ContextCompat.getDrawable(
            this,
            androidx.appcompat.R.drawable.abc_ic_ab_back_material
        )
        toolbar.setNavigationIconTint(
            ContextCompat.getColor(this, R.color.rs_color_on_background)
        )

        toolbar.setNavigationOnClickListener {
            stopPlaybackAndFinish()
        }

        playPauseButton.setOnClickListener {
            // subtle tap bounce
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

            if (isPlaying) {
                pausePlayback()
            } else {
                startPlayback(initial = false)
            }
        }

        sleepTimerButton.setOnClickListener {
            showSleepTimerDialog()
        }
    }

    override fun onBackPressed() {
        // stop playback (with fade-out via service) and close screen
        stopPlaybackAndFinish()
    }

    private fun stopPlaybackAndFinish() {
        // use the same fade-out logic as the pause button by sending ACTION_PAUSE
        val intent = Intent(this, SoundPlaybackService::class.java).apply {
            action = SoundPlaybackService.ACTION_PAUSE
        }
        startService(intent)

        isPlaying = false
        cancelSleepTimer()
        finish()
    }

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

        val backgroundView: View = findViewById(R.id.soundBackground)
        backgroundView.setBackgroundResource(backgroundRes)
    }

    private fun startPlayback(initial: Boolean) {
        isPlaying = true
        playPauseButton.setImageResource(R.drawable.ic_pause)
        playPauseButton.contentDescription = getString(R.string.sound_pause_label)

        val intent = Intent(this, SoundPlaybackService::class.java).apply {
            action = SoundPlaybackService.ACTION_PLAY
            putExtra(SoundPlaybackService.EXTRA_SOUND_KEY, soundKey)
        }
        ContextCompat.startForegroundService(this, intent)

        // if a sleep timer is configured, schedule it
        scheduleSleepTimerIfNeeded()
    }

    private fun pausePlayback() {
        isPlaying = false
        playPauseButton.setImageResource(R.drawable.ic_play)
        playPauseButton.contentDescription = getString(R.string.sound_play_label)

        val intent = Intent(this, SoundPlaybackService::class.java).apply {
            action = SoundPlaybackService.ACTION_PAUSE
        }
        startService(intent)

        cancelSleepTimer()
    }

    private fun showSleepTimerDialog() {
        val optionMinutes = intArrayOf(0, 15, 30, 60, -1)
        val labels = arrayOf(
            getString(R.string.sleep_timer_off),
            getString(R.string.sleep_timer_15),
            getString(R.string.sleep_timer_30),
            getString(R.string.sleep_timer_60),
            getString(R.string.sleep_timer_custom)
        )

        val currentIndex = when (sleepTimerDurationMs) {
            15L * 60_000L -> 1
            30L * 60_000L -> 2
            60L * 60_000L -> 3
            else -> 0
        }

        var selectedIndex = currentIndex

        MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_RelaxingSounds_AlertDialog)
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
                    val minutes = optionMinutes[selectedIndex]
                    applySleepTimerSelection(minutes)
                }
                dialog.dismiss()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun showCustomSleepTimerDialog() {
        val picker = NumberPicker(this).apply {
            minValue = 5
            maxValue = 120
            value = 30
            wrapSelectorWheel = false
        }

        MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_RelaxingSounds_AlertDialog)
            .setTitle(R.string.sleep_timer_custom)
            .setView(picker)
            .setPositiveButton(android.R.string.ok) { dialog, _ ->
                val minutes = picker.value
                applySleepTimerSelection(minutes)
                dialog.dismiss()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun applySleepTimerSelection(minutes: Int) {
        sleepTimerDurationMs = minutes.toLong() * 60_000L

        if (sleepTimerDurationMs > 0L) {
            if (isPlaying) {
                scheduleSleepTimerIfNeeded()
            } else {
                val totalSeconds = sleepTimerDurationMs / 1000L
                val m = totalSeconds / 60L
                val s = totalSeconds % 60L
                sleepTimerLabel.text = String.format("%d:%02d", m, s)
            }
            Toast.makeText(
                this,
                getString(R.string.sleep_timer_set_message, minutes),
                Toast.LENGTH_SHORT
            ).show()
        } else {
            cancelSleepTimer()
            Toast.makeText(
                this,
                R.string.sleep_timer_off_message,
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun scheduleSleepTimerIfNeeded() {
        sleepTimerRunnable?.let {
            sleepTimerHandler.removeCallbacks(it)
            sleepTimerRunnable = null
        }

        if (!isPlaying || sleepTimerDurationMs <= 0L) {
            sleepTimerEndRealtime = 0L
            cancelSleepTimerCountdown()
            updateSleepTimerLabelForOffState()
            return
        }

        sleepTimerEndRealtime = SystemClock.elapsedRealtime() + sleepTimerDurationMs

        val runnable = Runnable {
            if (isPlaying) {
                pausePlayback()
            } else {
                cancelSleepTimer()
            }
        }
        sleepTimerRunnable = runnable
        sleepTimerHandler.postDelayed(runnable, sleepTimerDurationMs)

        startSleepTimerCountdown()
    }

    private fun cancelSleepTimer() {
        sleepTimerRunnable?.let {
            sleepTimerHandler.removeCallbacks(it)
            sleepTimerRunnable = null
        }
        sleepTimerEndRealtime = 0L
        cancelSleepTimerCountdown()
        updateSleepTimerLabelForOffState()
    }

    private fun startSleepTimerCountdown() {
        cancelSleepTimerCountdown()

        if (sleepTimerDurationMs <= 0L || sleepTimerEndRealtime <= 0L) {
            updateSleepTimerLabelForOffState()
            return
        }

        val runnable = object : Runnable {
            override fun run() {
                val remaining = sleepTimerEndRealtime - SystemClock.elapsedRealtime()
                if (remaining <= 0L || !isPlaying) {
                    cancelSleepTimer()
                    return
                }

                val totalSeconds = remaining / 1000L
                val minutes = totalSeconds / 60L
                val seconds = totalSeconds % 60L
                sleepTimerLabel.text = String.format("%d:%02d", minutes, seconds)

                sleepTimerHandler.postDelayed(this, 1000L)
            }
        }

        sleepTimerCountdownRunnable = runnable
        sleepTimerHandler.post(runnable)
    }

    private fun cancelSleepTimerCountdown() {
        sleepTimerCountdownRunnable?.let {
            sleepTimerHandler.removeCallbacks(it)
            sleepTimerCountdownRunnable = null
        }
    }

    private fun updateSleepTimerLabelForOffState() {
        sleepTimerLabel.text = getString(R.string.sleep_timer_button_label)
    }

    override fun onPause() {
        super.onPause()
        // let playback continue in the background; only stop countdown ui
        cancelSleepTimerCountdown()
    }

    override fun onDestroy() {
        super.onDestroy()
        cancelSleepTimer()
    }

    companion object {
        const val EXTRA_SOUND_KEY = "sound_key"
        const val SOUND_OCEAN = "ocean"
        const val SOUND_RAIN = "rain"
        const val SOUND_BROWN = "brown"
    }
}
