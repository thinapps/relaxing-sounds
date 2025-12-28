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
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ImageButton
import android.widget.NumberPicker
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import top.thinapps.relaxingsounds.R
import top.thinapps.relaxingsounds.playback.SoundPlaybackService
import top.thinapps.relaxingsounds.core.ClickDebounce
import top.thinapps.relaxingsounds.core.SoundCatalog

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
    private var sleepTimerCountdownRunnable: Runnable? = null

    private var sleepTimerRemainingSeconds: Long = 0L
    private var playbackStateReceiverRegistered: Boolean = false

    private val playbackStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action != SoundPlaybackService.ACTION_PLAYBACK_STATE) return

            val key = intent.getStringExtra(SoundPlaybackService.EXTRA_CURRENT_SOUND_KEY)
            if (key == null || key != soundKey) return

            val playing = intent.getBooleanExtra(
                SoundPlaybackService.EXTRA_IS_PLAYING,
                false
            )

            isPlaying = playing

            if (!playing) {
                playPauseButton.setImageResource(R.drawable.ic_play)
                playPauseButton.contentDescription =
                    getString(R.string.sound_play_label)
                cancelSleepTimerCountdown()
            } else {
                playPauseButton.setImageResource(R.drawable.ic_pause)
                playPauseButton.contentDescription =
                    getString(R.string.sound_pause_label)
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

        soundKey = intent.getStringExtra(EXTRA_SOUND_KEY)
            ?: SoundCatalog.sounds.first().key

        val sound = SoundCatalog.getByKey(soundKey) ?: return

        setupUiForSound(sound)

        val launchSource = intent.getStringExtra(EXTRA_LAUNCH_SOURCE)
        if (launchSource == SOURCE_MAIN) {
            startPlayback(initial = true)
        }

        toolbar.navigationIcon = ContextCompat.getDrawable(
            this,
            androidx.appcompat.R.drawable.abc_ic_ab_back_material
        )
        toolbar.setNavigationIconTint(
            ContextCompat.getColor(this, R.color.rs_color_on_background)
        )

        toolbar.setNavigationOnClickListener {
            if (!ClickDebounce.allowClick()) return@setNavigationOnClickListener
            exitAndPause()
        }

        playPauseButton.setOnClickListener {
            if (!ClickDebounce.allowClick()) return@setOnClickListener
            animatePlayPauseIcon()

            if (isPlaying) {
                isPlaying = false
                cancelSleepTimerCountdown()
                pausePlayback()
            } else {
                startPlayback(initial = false)
            }
        }

        sleepTimerButton.setOnClickListener {
            if (!ClickDebounce.allowClick()) return@setOnClickListener
            showSleepTimerDialog()
        }
    }

    override fun onStart() {
        super.onStart()
        registerPlaybackStateReceiver()
    }

    override fun onResume() {
        super.onResume()
        requestPlaybackStateSync()

        if (isPlaying && sleepTimerRemainingSeconds > 0L) {
            startSleepTimerCountdown()
        }
    }

    override fun onStop() {
        super.onStop()
        unregisterPlaybackStateReceiver()

        if (isFinishing) resetSleepTimer()
    }

    override fun onBackPressed() {
        if (!ClickDebounce.allowClick()) return
        exitAndPause()
    }

    private fun exitAndPause() {
        if (isPlaying) {
            val intent = Intent(this, SoundPlaybackService::class.java).apply {
                action = SoundPlaybackService.ACTION_PAUSE
            }
            startService(intent)
        }
        finish()
    }

    private fun animatePlayPauseIcon() {
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
    }

    private fun setupUiForSound(sound: SoundCatalog.SoundItem) {
        soundTitle.setText(sound.titleResId)
        soundDescription.setText(sound.subtitleResId)

        findViewById<View>(R.id.soundBackground)
            .setBackgroundResource(sound.backgroundResId)
    }

    private fun startPlayback(initial: Boolean) {
        isPlaying = true
        playPauseButton.setImageResource(R.drawable.ic_pause)

        val intent = Intent(this, SoundPlaybackService::class.java).apply {
            action = SoundPlaybackService.ACTION_PLAY
            putExtra(SoundPlaybackService.EXTRA_SOUND_KEY, soundKey)
        }
        ContextCompat.startForegroundService(this, intent)

        if (sleepTimerRemainingSeconds > 0L) startSleepTimerCountdown()
    }

    private fun pausePlayback() {
        isPlaying = false
        playPauseButton.setImageResource(R.drawable.ic_play)

        val intent = Intent(this, SoundPlaybackService::class.java).apply {
            action = SoundPlaybackService.ACTION_PAUSE
        }
        startService(intent)

        cancelSleepTimerCountdown()
    }

    private fun registerPlaybackStateReceiver() {
        if (playbackStateReceiverRegistered) return
        registerReceiver(
            playbackStateReceiver,
            IntentFilter(SoundPlaybackService.ACTION_PLAYBACK_STATE)
        )
        playbackStateReceiverRegistered = true
    }

    private fun unregisterPlaybackStateReceiver() {
        if (!playbackStateReceiverRegistered) return
        unregisterReceiver(playbackStateReceiver)
        playbackStateReceiverRegistered = false
    }

    private fun requestPlaybackStateSync() {
        val intent = Intent(this, SoundPlaybackService::class.java).apply {
            action = SoundPlaybackService.ACTION_REQUEST_STATE
            putExtra(SoundPlaybackService.EXTRA_SOUND_KEY, soundKey)
        }
        startService(intent)
    }

    override fun onPause() {
        super.onPause()
        cancelSleepTimerCountdown()
    }

    override fun onDestroy() {
        super.onDestroy()
        resetSleepTimer()
    }

    companion object {
        const val EXTRA_SOUND_KEY = "sound_key"
        const val EXTRA_LAUNCH_SOURCE = "launch_source"

        const val SOURCE_MAIN = "source_main"
        const val SOURCE_NOTIFICATION = "source_notification"
    }
}
