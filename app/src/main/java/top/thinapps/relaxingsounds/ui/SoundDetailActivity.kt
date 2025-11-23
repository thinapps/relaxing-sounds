package top.thinapps.relaxingsounds.ui

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.appbar.MaterialToolbar
import top.thinapps.relaxingsounds.R

class SoundDetailActivity : AppCompatActivity() {

    private var mediaPlayer: MediaPlayer? = null
    private var fadeAnimator: ValueAnimator? = null
    private var isPlaying: Boolean = false
    private lateinit var soundKey: String

    private lateinit var root: View
    private lateinit var toolbar: MaterialToolbar
    private lateinit var soundTitle: TextView
    private lateinit var soundDescription: TextView
    private lateinit var playPauseButton: ImageButton
    private lateinit var sleepTimerButton: View

    private val sleepTimerHandler = Handler(Looper.getMainLooper())
    private var sleepTimerRunnable: Runnable? = null
    private var sleepTimerDurationMs: Long = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sound_detail)

        root = findViewById(R.id.sound_detail_root)
        toolbar = findViewById(R.id.toolbar)
        soundTitle = findViewById(R.id.soundTitle)
        soundDescription = findViewById(R.id.soundDescription)
        playPauseButton = findViewById(R.id.playPauseButton)
        sleepTimerButton = findViewById(R.id.sleepTimerContainer)

        soundKey = intent.getStringExtra(EXTRA_SOUND_KEY) ?: SOUND_OCEAN

        setupUiForSound(soundKey)

        // auto start playback with fade-in
        fadeInAndStart()

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
            finish()
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
                fadeOutAndPause()
            } else {
                fadeInAndStart()
            }
        }

        sleepTimerButton.setOnClickListener {
            showSleepTimerDialog()
        }
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

    private fun ensureMediaPlayer() {
        if (mediaPlayer != null) return

        val resId = when (soundKey) {
            SOUND_OCEAN -> R.raw.ocean_waves
            SOUND_RAIN -> R.raw.rain
            SOUND_BROWN -> R.raw.rain
            else -> R.raw.ocean_waves
        }

        mediaPlayer = MediaPlayer.create(this, resId).apply {
            isLooping = true
            setVolume(0f, 0f)
        }
    }

    private fun fadeInAndStart() {
        isPlaying = true
        playPauseButton.setImageResource(R.drawable.ic_pause)
        playPauseButton.contentDescription = getString(R.string.sound_pause_label)

        ensureMediaPlayer()
        val player = mediaPlayer ?: return

        fadeAnimator?.cancel()

        fadeAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = FADE_DURATION_MS
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener { animator ->
                val v = animator.animatedValue as Float
                player.setVolume(v, v)
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationStart(animation: Animator) {
                    if (!player.isPlaying) {
                        player.start()
                    }
                }
            })
            start()
        }

        // if a sleep timer is configured, schedule it
        scheduleSleepTimerIfNeeded()
    }

    private fun fadeOutAndPause() {
        isPlaying = false
        playPauseButton.setImageResource(R.drawable.ic_play)
        playPauseButton.contentDescription = getString(R.string.sound_play_label)

        val player = mediaPlayer ?: return

        fadeAnimator?.cancel()
        cancelSleepTimer()

        fadeAnimator = ValueAnimator.ofFloat(1f, 0f).apply {
            duration = FADE_DURATION_MS
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener { animator ->
                val v = animator.animatedValue as Float
                player.setVolume(v, v)
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    player.pause()
                }
            })
            start()
        }
    }

    private fun showSleepTimerDialog() {
        val optionMinutes = intArrayOf(0, 15, 30, 60)
        val labels = arrayOf(
            getString(R.string.sleep_timer_off),
            getString(R.string.sleep_timer_15),
            getString(R.string.sleep_timer_30),
            getString(R.string.sleep_timer_60)
        )

        val currentIndex = when (sleepTimerDurationMs) {
            15L * 60_000L -> 1
            30L * 60_000L -> 2
            60L * 60_000L -> 3
            else -> 0
        }

        var selectedIndex = currentIndex

        AlertDialog.Builder(this)
            .setTitle(R.string.sleep_timer_title)
            .setSingleChoiceItems(labels, currentIndex) { _, which ->
                selectedIndex = which
            }
            .setPositiveButton(android.R.string.ok) { dialog, _ ->
                val minutes = optionMinutes[selectedIndex]
                sleepTimerDurationMs = minutes * 60_000L

                if (sleepTimerDurationMs > 0L) {
                    if (isPlaying) {
                        scheduleSleepTimerIfNeeded()
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

                dialog.dismiss()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun scheduleSleepTimerIfNeeded() {
        cancelSleepTimer()

        if (!isPlaying || sleepTimerDurationMs <= 0L) {
            return
        }

        val runnable = Runnable {
            if (isPlaying) {
                fadeOutAndPause()
            }
        }
        sleepTimerRunnable = runnable
        sleepTimerHandler.postDelayed(runnable, sleepTimerDurationMs)
    }

    private fun cancelSleepTimer() {
        val runnable = sleepTimerRunnable ?: return
        sleepTimerHandler.removeCallbacks(runnable)
        sleepTimerRunnable = null
    }

    override fun onPause() {
        super.onPause()
        if (isPlaying) {
            fadeOutAndPause()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        fadeAnimator?.cancel()
        fadeAnimator = null
        cancelSleepTimer()
        mediaPlayer?.release()
        mediaPlayer = null
    }

    companion object {
        const val EXTRA_SOUND_KEY = "sound_key"
        const val SOUND_OCEAN = "ocean"
        const val SOUND_RAIN = "rain"
        const val SOUND_BROWN = "brown"

        private const val FADE_DURATION_MS = 800L
    }
}
