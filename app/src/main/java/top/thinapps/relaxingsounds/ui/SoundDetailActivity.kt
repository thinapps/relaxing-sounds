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
                ?: return

            if (key != soundKey) {
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

    override fun onStart() {
        super.onStart()
        registerPlaybackStateReceiver()
    }

    override fun onStop() {
        super.onStop()
        unregisterPlaybackStateReceiver()
    }

    override fun onBackPressed() {
        // stop playback (with fade-out via service) and close screen
        stopPlaybackAndFinish()
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

        toolbar.title = getString(titleRes)
        soundTitle.text = getString(titleRes)
        soundDescription.text = getString(descriptionRes)
        root.setBackgroundResource(backgroundRes)
    }

    private fun showSleepTimerDialog() {
        // minutes for each preset; last entry is "Custom..."
        val optionMinutes = intArrayOf(
            0,    // Off
            15,
            30,
            45,
            60,
            90,
            120,
            180,
            240,
            360,
            480,
            720
        )

        val optionLabels = optionMinutes.mapIndexed { index, minutes ->
            if (minutes == 0) {
                getString(R.string.sleep_timer_off)
            } else {
                resources.getQuantityString(
                    R.plurals.sleep_timer_minutes,
                    minutes,
                    minutes
                )
            }
        }.toTypedArray()

        var selectedIndex = 0
        val currentMinutes = (sleepTimerDurationMs / 60000L).toInt()
        val currentIndex = optionMinutes.indexOf(currentMinutes)
        if (currentIndex >= 0) {
            selectedIndex = currentIndex
        }

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.sleep_timer_title)
            .setSingleChoiceItems(optionLabels, selectedIndex) { dialog, which ->
                selectedIndex = which
            }
            .setPositiveButton(android.R.string.ok) { dialog, _ ->
                val chosenMinutes = optionMinutes[selectedIndex]
                if (chosenMinutes == 0) {
                    sleepTimerDurationMs = 0L
                    cancelSleepTimer()
                    updateSleepTimerLabel()
                } else {
                    sleepTimerDurationMs = chosenMinutes * 60_000L
                    scheduleSleepTimerIfNeeded()
                    updateSleepTimerLabel()
                }
                dialog.dismiss()
            }
            .setNegativeButton(android.R.string.cancel) { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun scheduleSleepTimerIfNeeded() {
        sleepTimerRunnable?.let {
            sleepTimerHandler.removeCallbacks(it)
        }

        sleepTimerCountdownRunnable?.let {
            sleepTimerHandler.removeCallbacks(it)
        }

        if (!isPlaying || sleepTimerDurationMs <= 0L) {
            sleepTimerRunnable = null
            sleepTimerCountdownRunnable = null
            sleepTimerEndRealtime = 0L
            updateSleepTimerLabel()
            return
        }

        sleepTimerEndRealtime = SystemClock.elapsedRealtime() + sleepTimerDurationMs

        sleepTimerRunnable = Runnable {
            if (isPlaying) {
                pausePlayback()
                Toast.makeText(
                    this,
                    R.string.sleep_timer_completed,
                    Toast.LENGTH_SHORT
                ).show()
            }
        }.also { runnable ->
            sleepTimerHandler.postAtTime(
                runnable,
                sleepTimerEndRealtime
            )
        }

        sleepTimerCountdownRunnable = object : Runnable {
            override fun run() {
                updateSleepTimerLabel()
                if (isPlaying && sleepTimerEndRealtime > 0L) {
                    sleepTimerHandler.postDelayed(this, 1_000L)
                }
            }
        }.also { runnable ->
            sleepTimerHandler.post(runnable)
        }

        updateSleepTimerLabel()
    }

    private fun cancelSleepTimer() {
        sleepTimerRunnable?.let {
            sleepTimerHandler.removeCallbacks(it)
        }
        sleepTimerRunnable = null

        sleepTimerCountdownRunnable?.let {
            sleepTimerHandler.removeCallbacks(it)
        }
        sleepTimerCountdownRunnable = null

        sleepTimerEndRealtime = 0L
        updateSleepTimerLabel()
    }

    private fun updateSleepTimerLabel() {
        if (sleepTimerDurationMs <= 0L || sleepTimerEndRealtime <= 0L) {
            sleepTimerLabel.text = getString(R.string.sleep_timer_off)
            return
        }

        val remainingMs = sleepTimerEndRealtime - SystemClock.elapsedRealtime()
        if (remainingMs <= 0L) {
            sleepTimerLabel.text = getString(R.string.sleep_timer_off)
            return
        }

        val totalSeconds = remainingMs / 1000L
        val minutes = totalSeconds / 60L
        val seconds = totalSeconds % 60L
        sleepTimerLabel.text = getString(
            R.string.sleep_timer_countdown_format,
            minutes,
            seconds
        )
    }

    private fun registerPlaybackStateReceiver() {
        if (playbackStateReceiverRegistered) {
            return
        }
        val filter = IntentFilter(SoundPlaybackService.ACTION_PLAYBACK_STATE)
        registerReceiver(playbackStateReceiver, filter)
        playbackStateReceiverRegistered = true
    }

    private fun unregisterPlaybackStateReceiver() {
        if (!playbackStateReceiverRegistered) {
            return
        }
        unregisterReceiver(playbackStateReceiver)
        playbackStateReceiverRegistered = false
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
