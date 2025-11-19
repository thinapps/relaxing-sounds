package top.thinapps.relaxingsounds.ui

import android.media.MediaPlayer
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import top.thinapps.relaxingsounds.R

class SoundDetailActivity : AppCompatActivity() {

    private var mediaPlayer: MediaPlayer? = null
    private var isPlaying: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sound_detail)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        val titleView = findViewById<TextView>(R.id.soundTitle)
        val descriptionView = findViewById<TextView>(R.id.soundDescription)
        val playPauseButton = findViewById<MaterialButton>(R.id.buttonPlayPause)

        toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        val soundKey = intent.getStringExtra(EXTRA_SOUND_KEY) ?: SOUND_OCEAN

        val (titleRes, subtitleRes, hasAudio) = when (soundKey) {
            SOUND_OCEAN -> Triple(
                R.string.sound_ocean_title,
                R.string.sound_ocean_subtitle,
                true
            )
            SOUND_RAIN -> Triple(
                R.string.sound_rain_title,
                R.string.sound_rain_subtitle,
                false
            )
            SOUND_BROWN -> Triple(
                R.string.sound_brown_title,
                R.string.sound_brown_subtitle,
                false
            )
            else -> Triple(
                R.string.sound_ocean_title,
                R.string.sound_ocean_subtitle,
                true
            )
        }

        titleView.text = getString(titleRes)

        if (hasAudio) {
            descriptionView.text = getString(subtitleRes)

            mediaPlayer = MediaPlayer.create(this, R.raw.ocean_waves).apply {
                isLooping = true
                start()
            }
            isPlaying = true
            playPauseButton.text = getString(R.string.sound_pause_label)

            playPauseButton.setOnClickListener {
                togglePlayback(playPauseButton)
            }
        } else {
            val baseText = getString(subtitleRes)
            descriptionView.text = "$baseText\n\nAudio coming soon in a future update."
            playPauseButton.visibility = View.GONE
        }
    }

    private fun togglePlayback(button: MaterialButton) {
        if (isPlaying) {
            mediaPlayer?.pause()
            isPlaying = false
            button.text = getString(R.string.sound_play_label)
        } else {
            mediaPlayer?.start()
            isPlaying = true
            button.text = getString(R.string.sound_pause_label)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
        mediaPlayer = null
    }

    companion object {
        const val EXTRA_SOUND_KEY = "sound_key"
        const val SOUND_OCEAN = "ocean"
        const val SOUND_RAIN = "rain"
        const val SOUND_BROWN = "brown"
    }
}
