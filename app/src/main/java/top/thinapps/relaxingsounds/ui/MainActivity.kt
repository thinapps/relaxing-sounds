package top.thinapps.relaxingsounds.ui

import android.media.MediaPlayer
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.card.MaterialCardView
import top.thinapps.relaxingsounds.R

class MainActivity : AppCompatActivity() {

    private var mediaPlayer: MediaPlayer? = null
    private var isPlaying = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val cardOcean = findViewById<MaterialCardView>(R.id.card_ocean_waves)
        val cardRain = findViewById<MaterialCardView>(R.id.card_rain)
        val cardBrown = findViewById<MaterialCardView>(R.id.card_brown_noise)

        cardOcean.setOnClickListener { toggleOcean() }
        cardRain.setOnClickListener { showComingSoon("Rain") }
        cardBrown.setOnClickListener { showComingSoon("Brown Noise") }
    }

    private fun toggleOcean() {
        if (isPlaying) {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
            isPlaying = false
            Toast.makeText(this, "Stopped Ocean Waves", Toast.LENGTH_SHORT).show()
        } else {
            mediaPlayer = MediaPlayer.create(this, R.raw.ocean_waves)
            mediaPlayer?.isLooping = true
            mediaPlayer?.start()
            isPlaying = true
            Toast.makeText(this, "Playing Ocean Waves", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showComingSoon(label: String) {
        Toast.makeText(this, "$label coming soon", Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
        mediaPlayer = null
    }
}
