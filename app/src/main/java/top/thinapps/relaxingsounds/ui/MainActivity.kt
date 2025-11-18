package top.thinapps.relaxingsounds.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.card.MaterialCardView
import top.thinapps.relaxingsounds.R

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val cardOcean = findViewById<MaterialCardView>(R.id.card_ocean_waves)
        val cardRain = findViewById<MaterialCardView>(R.id.card_rain)
        val cardBrown = findViewById<MaterialCardView>(R.id.card_brown_noise)

        cardOcean.setOnClickListener {
            openSoundDetail(SoundDetailActivity.SOUND_OCEAN)
        }

        cardRain.setOnClickListener {
            openSoundDetail(SoundDetailActivity.SOUND_RAIN)
        }

        cardBrown.setOnClickListener {
            openSoundDetail(SoundDetailActivity.SOUND_BROWN)
        }
    }

    private fun openSoundDetail(soundKey: String) {
        val intent = Intent(this, SoundDetailActivity::class.java).apply {
            putExtra(SoundDetailActivity.EXTRA_SOUND_KEY, soundKey)
        }
        startActivity(intent)
    }
}
