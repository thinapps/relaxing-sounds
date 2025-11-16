package top.thinapps.relaxingsounds.ui

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.card.MaterialCardView
import top.thinapps.relaxingsounds.R

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val cardOcean = findViewById<MaterialCardView>(R.id.card_ocean_waves)
        val cardRain = findViewById<MaterialCardView>(R.id.card_rain)
        val cardBrownNoise = findViewById<MaterialCardView>(R.id.card_brown_noise)

        cardOcean.setOnClickListener {
            showComingSoon("Ocean Waves")
        }

        cardRain.setOnClickListener {
            showComingSoon("Rain")
        }

        cardBrownNoise.setOnClickListener {
            showComingSoon("Brown Noise")
        }
    }

    private fun showComingSoon(label: String) {
        val message = "$label will play in a future update. This is a preview build."
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
