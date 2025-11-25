package top.thinapps.relaxingsounds.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.card.MaterialCardView
import top.thinapps.relaxingsounds.R

class MainActivity : AppCompatActivity() {

    private val requestNotificationsPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { _: Boolean ->
            // no-op: playback service will automatically show notifications once permission is granted
        }

    private var isNavigatingToDetail = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        ensureNotificationPermission()

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

    private fun ensureNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasPermission = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            if (!hasPermission) {
                requestNotificationsPermissionLauncher.launch(
                    Manifest.permission.POST_NOTIFICATIONS
                )
            }
        }
    }

    private fun openSoundDetail(soundKey: String) {
        val intent = Intent(this, SoundDetailActivity::class.java).apply {
            putExtra(SoundDetailActivity.EXTRA_SOUND_KEY, soundKey)
            putExtra(SoundDetailActivity.EXTRA_LAUNCH_SOURCE, SoundDetailActivity.SOURCE_MAIN)
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
