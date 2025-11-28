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
import android.os.SystemClock
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ImageButton
import android.widget.NumberPicker
import android.widget.LinearLayout
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

    private var playbackStateReceiverRegistered: Boolean = false
    private val playbackStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action != SoundPlaybackService.ACTION_PLAYBACK_STATE) {
                return
            }

            val key = intent.getStringExtra(SoundPlaybackService.EXTRA_CURRENT_SOUND_KEY)
            if (key == null || key != soundKey) {
                return
            }

            val playing = intent.getBooleanExtra(
                SoundPlaybackService.EXTRA_IS_PLAYING,
                false
            )

            isPlaying = playing

            if (playing) {
                playPauseButton.setImageResource(R.drawable.ic_pause)
                playPauseButton.contentDescription =
                    getString(R.string.sound_pause_label)
            } else {
                playPauseButton.setImageResource(R.drawable.ic_play)
                playPauseButton.contentDescription =
                    getString(R.string.sound_play_label)
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
        val shouldAutoPlay = launchSource == SOURCE_MAIN

        if (shouldAutoPlay) {
            startPlayback(initial = true)
        }

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

            if (isPlaying) pausePlayback() else startPlayback(initial = false)
        }

        sleepTimerButton.setOnClickListener {
            showSleepTimerDialog()
        }
    }

    override fun onStart() {
        super.onStart()
        registerPlaybackStateReceiver()
    }

    override fun onResume() {
        super.onResume()
        requestPlaybackStateSync()
    }

    override fun onStop() {
        super.onStop()
        unregisterPlaybackStateReceiver()
    }

    override fun onBackPressed() {
        stopPlaybackAndFinish()
    }

    private fun stopPlaybackAndFinish() {
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

        val intent = Intent(this, SoundPlaybackService::class.java).apply {
            action = SoundPlaybackService.ACTION_PLAY
            putExtra(SoundPlaybackService.EXTRA_SOUND_KEY, soundKey)
        }
        ContextCompat.startForegroundService(this, intent)

        scheduleSleepTimerIfNeeded()
    }

    private fun pausePlayback() {
        isPlaying = false
        playPauseButton.setImageResource(R.drawable.ic_play)

        val intent = Intent(this, SoundPlaybackService::class.java).apply {
            action = SoundPlaybackService.ACTION_PAUSE
        }
        startService(intent)

        cancelSleepTimer()
    }

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

        val currentIndex = when (sleepTimerDurationMs) {
            15L * 60_000L -> 1
            30L * 60_000L -> 2
            45L * 60_000L -> 3
            60L * 60_000L -> 4
            90L * 60_000L -> 5
            120L * 60_000L -> 6
            180L * 60_000L -> 7
            240L * 60_000L -> 8
            360L * 60_000L -> 9
            480L * 60_000L -> 10
            720L * 60_000L -> 11
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
                    val minutes = optionMinutes[selectedIndex]
                    applySleepTimerSelection(minutes)
                }
                dialog.dismiss()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    /***
     * Custom Timer dialog with Hours + Minutes pickers
     */
    private fun showCustomSleepTimerDialog() {

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(40, 10, 40, 10)
        }

        val hourPicker = NumberPicker(this).apply {
            minValue = 0
            maxValue = 12
            value = 0
        }

        val minutePicker = NumberPicker(this).apply {
            minValue = 0
            maxValue = 59
            value = 30
        }

        layout.addView(hourPicker)
        layout.addView(minutePicker)

        MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_RelaxingSounds_AlertDialog)
            .setTitle(R.string.sleep_timer_custom)
            .setView(layout)
            .setPositiveButton(android.R.string.ok) { dialog, _ ->
                val hours = hourPicker.value
                val mins = minutePicker.value
                val totalMinutes = (hours * 60) + mins
                applySleepTimerSelection(totalMinutes)
                dialog.dismiss()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun applySleepTimerSelection(minutes: Int) {
        sleepTimerDurationMs = minutes.toLong() * 60_000L

        if (sleepTimerDurationMs > 0L) {

            val totalSeconds = minutes * 60
            val hours = totalSeconds / 3600
            val mins = (totalSeconds % 3600) / 60
            val secs = totalSeconds % 60

            sleepTimerLabel.text = String.format("%02d:%02d:%02d", hours, mins, secs)

            if (isPlaying) scheduleSleepTimerIfNeeded()

            Toast.makeText(
                this,
                getString(R.string.sleep_timer_set_message, minutes),
                Toast.LENGTH_SHORT
            ).show()

        } else {
            cancelSleepTimer()
            Toast.makeText(this, R.string.sleep_timer_off_message, Toast.LENGTH_SHORT).show()
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
            if (isPlaying) pausePlayback() else cancelSleepTimer()
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

                val totalSeconds = remaining / 1000
                val hours = totalSeconds / 3600
                val minutes = (totalSeconds % 3600) / 60
                val seconds = totalSeconds % 60

                sleepTimerLabel.text = String.format("%02d:%02d:%02d", hours, minutes, seconds)

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

    private fun registerPlaybackStateReceiver() {
        if (playbackStateReceiverRegistered) return
        val filter = IntentFilter(SoundPlaybackService.ACTION_PLAYBACK_STATE)
        registerReceiver(playbackStateReceiver, filter)
        playbackStateReceiverRegistered = true
    }

    private fun unregisterPlaybackStateReceiver() {
        if (!playbackStateReceiverRegistered) return
        unregisterReceiver(playbackStateReceiver)
        playbackStateReceiverRegistered = false
    }

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
        cancelSleepTimer()
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
