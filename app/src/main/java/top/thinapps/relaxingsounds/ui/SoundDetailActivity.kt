package top.thinapps.relaxingsounds.ui

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.media.MediaPlayer
import android.os.Bundle
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
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
    private lateinit var buttonPlayPause: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sound_detail)

        root = findViewById(R.id.sound_detail_root)
        toolbar = findViewById(R.id.toolbar)
        soundTitle = findViewById(R.id.soundTitle)
        soundDescription = findViewById(R.id.soundDescription)
        buttonPlayPause = findViewById(R.id.buttonPlayPause)

        soundKey = intent.getStringExtra(EXTRA_SOUND_KEY) ?: SOUND_OCEAN

        setupUiForSound(soundKey)

        // auto start playback with fade-in
        fadeInAndStart()

        toolbar.setNavigationOnClickListener {
            finish()
        }

        buttonPlayPause.setOnClickListener {
            if (isPlaying) {
                fadeOutAndPause()
            } else {
                fadeInAndStart()
            }
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
            // placeholder: until you add a real brown noise file, reuse rain background/text if wanted
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
        root.background = ContextCompat.getDrawable(this, backgroundRes)

        // initial state: not playing
        isPlaying = false
        buttonPlayPause.setText(R.string.sound_play_label)
    }

    private fun ensureMediaPlayer() {
        if (mediaPlayer != null) return

        val resId = when (soundKey) {
            SOUND_OCEAN -> R.raw.ocean_waves
            SOUND_RAIN -> R.raw.rain
            // until you have a dedicated brown noise file, you can point this somewhere else
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
        buttonPlayPause.setText(R.string.sound_pause_label)

        ensureMediaPlayer()
        val player = mediaPlayer ?: return

        fadeAnimator?.cancel()

        // start from silent
        player.setVolume(0f, 0f)
        if (!player.isPlaying) {
            player.start()
        }

        fadeAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = FADE_DURATION_MS
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener { animator ->
                val v = animator.animatedValue as Float
                player.setVolume(v, v)
            }
            start()
        }
    }

    private fun fadeOutAndPause() {
        isPlaying = false
        buttonPlayPause.setText(R.string.sound_play_label)

        val player = mediaPlayer ?: return

        fadeAnimator?.cancel()

        fadeAnimator = ValueAnimator.ofFloat(1f, 0f).apply {
            duration = FADE_DURATION_MS
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener { animator ->
                val v = animator.animatedValue as Float
                player.setVolume(v, v)
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    if (!isPlaying) {
                        if (player.isPlaying) {
                            player.pause()
                            player.seekTo(0)
                        }
                    }
                }
            })
            start()
        }
    }

    override fun onPause() {
        super.onPause()
        // if user leaves screen, fade out gracefully
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
