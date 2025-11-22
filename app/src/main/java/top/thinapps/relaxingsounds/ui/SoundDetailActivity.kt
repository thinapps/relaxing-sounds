package top.thinapps.relaxingsounds.ui

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.media.MediaPlayer
import android.os.Bundle
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ImageButton
import android.widget.TextView
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sound_detail)

        root = findViewById(R.id.sound_detail_root)
        toolbar = findViewById(R.id.toolbar)
        soundTitle = findViewById(R.id.soundTitle)
        soundDescription = findViewById(R.id.soundDescription)
        playPauseButton = findViewById(R.id.playPauseButton)

        soundKey = intent.getStringExtra(EXTRA_SOUND_KEY) ?: SOUND_OCEAN

        setupUiForSound(soundKey)

        // auto start playback with fade-in
        fadeInAndStart()

        // immersive toolbar + themed nav arrow tint
        toolbar.setBackgroundColor(android.graphics.Color.TRANSPARENT)
        toolbar.elevation = 0f
        toolbar.setNavigationIconTint(
            ContextCompat.getColor(this, R.color.rs_color_on_background)
        )

        toolbar.setNavigationOnClickListener {
            finish()
        }

        // subtle bounce on play/pause tap plus fade logic
        playPauseButton.setOnClickListener {
            playPauseButton.animate()
                .scaleX(0.94f)
                .scaleY(0.94f)
                .setDuration(70)
                .setInterpolator(AccelerateDecelerateInterpolator())
                .withEndAction {
                    playPauseButton.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(70)
                        .setInterpolator(AccelerateDecelerateInterpolator())
                        .withEndAction {
                            if (isPlaying) {
                                fadeOutAndPause()
                            } else {
                                fadeInAndStart()
                            }
                        }
                        .start()
                }
                .start()
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
        root.setBackgroundResource(backgroundRes)
    }

    private fun ensureMediaPlayer(): MediaPlayer {
        if (mediaPlayer == null) {
            mediaPlayer = MediaPlayer.create(
                this,
                when (soundKey) {
                    SOUND_OCEAN -> R.raw.ocean_waves
                    SOUND_RAIN -> R.raw.rain
                    SOUND_BROWN -> R.raw.ocean_waves
                    else -> R.raw.ocean_waves
                }
            )
            mediaPlayer?.isLooping = true
        }
        return mediaPlayer!!
    }

    private fun fadeInAndStart() {
        fadeAnimator?.cancel()

        val player = ensureMediaPlayer()
        player.setVolume(0f, 0f)
        player.start()
        isPlaying = true
        playPauseButton.setImageResource(R.drawable.ic_pause)
        playPauseButton.contentDescription = getString(R.string.sound_pause_label)

        fadeAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = FADE_DURATION_MS
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener { animator ->
                val volume = animator.animatedValue as Float
                player.setVolume(volume, volume)
            }
            start()
        }
    }

    private fun fadeOutAndPause() {
        fadeAnimator?.cancel()

        val player = mediaPlayer ?: return

        fadeAnimator = ValueAnimator.ofFloat(1f, 0f).apply {
            duration = FADE_DURATION_MS
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener { animator ->
                val volume = animator.animatedValue as Float
                player.setVolume(volume, volume)
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    if (player.isPlaying) {
                        player.pause()
                    }
                    isPlaying = false
                    playPauseButton.setImageResource(R.drawable.ic_play)
                    playPauseButton.contentDescription =
                        getString(R.string.sound_play_label)
                }
            })
            start()
        }
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
