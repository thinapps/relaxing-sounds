package top.thinapps.relaxingsounds.ui

import android.content.Intent
import android.os.Bundle
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.card.MaterialCardView
import top.thinapps.relaxingsounds.R

class MainActivity : AppCompatActivity() {

    private var isNavigatingToDetail = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val cardOcean = findViewById<MaterialCardView>(R.id.card_ocean_waves)
        val cardRain = findViewById<MaterialCardView>(R.id.card_rain)
        val cardBrown = findViewById<MaterialCardView>(R.id.card_brown_noise)

        cardOcean.setRelaxingClick(SoundDetailActivity.SOUND_OCEAN)
        cardRain.setRelaxingClick(SoundDetailActivity.SOUND_RAIN)
        cardBrown.setRelaxingClick(SoundDetailActivity.SOUND_BROWN)
    }

    override fun onResume() {
        super.onResume()
        isNavigatingToDetail = false
    }

    private fun openSoundDetail(soundKey: String) {
        val intent = Intent(this, SoundDetailActivity::class.java).apply {
            putExtra(SoundDetailActivity.EXTRA_SOUND_KEY, soundKey)
        }
        startActivity(intent)
    }

    private fun MaterialCardView.setRelaxingClick(soundKey: String) {
        setOnClickListener {
            if (isNavigatingToDetail) {
                return@setOnClickListener
            }
            isNavigatingToDetail = true

            // gentle press in (faster)
            this.animate()
                .scaleX(0.98f)
                .scaleY(0.98f)
                .setDuration(90)
                .setInterpolator(AccelerateDecelerateInterpolator())
                .withEndAction {
                    // gentle release, then open screen (faster)
                    this.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(90)
                        .setInterpolator(AccelerateDecelerateInterpolator())
                        .withEndAction {
                            openSoundDetail(soundKey)
                        }
                        .start()
                }
                .start()
        }
    }
}
