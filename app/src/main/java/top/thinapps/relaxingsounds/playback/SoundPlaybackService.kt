package top.thinapps.relaxingsounds.playback

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.app.Service
import android.content.Intent
import android.media.MediaPlayer
import android.os.IBinder
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.core.app.NotificationManagerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import top.thinapps.relaxingsounds.R
import top.thinapps.relaxingsounds.notifications.NotificationHelper

class SoundPlaybackService : Service() {

    private var mediaPlayer: MediaPlayer? = null
    private var fadeAnimator: ValueAnimator? = null
    private var isPlaying: Boolean = false
    private var currentSoundKey: String? = null

    // ADDED — minimal MediaSession support
    private lateinit var mediaSession: MediaSessionCompat
    private lateinit var playbackState: PlaybackStateCompat.Builder

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createChannelIfNeeded(this)

        // ADDED — initialize MediaSession
        mediaSession = MediaSessionCompat(this, "RelaxingSounds").apply {
            isActive = true
        }

        playbackState = PlaybackStateCompat.Builder().setActions(
            PlaybackStateCompat.ACTION_PLAY or
            PlaybackStateCompat.ACTION_PAUSE or
            PlaybackStateCompat.ACTION_STOP
        )

        // Start in paused state
        mediaSession.setPlaybackState(
            playbackState.setState(PlaybackStateCompat.STATE_PAUSED, 0, 1f).build()
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        val soundKeyFromIntent = intent?.getStringExtra(EXTRA_SOUND_KEY)

        when (action) {
            ACTION_PLAY -> {
                val targetKey = soundKeyFromIntent ?: currentSoundKey
                if (targetKey != null) {
                    preparePlayerIfNeeded(targetKey)
                    startFadeIn()
                }
            }
            ACTION_PAUSE -> {
                pauseWithFade()
            }
            ACTION_TOGGLE -> {
                if (isPlaying) {
                    pauseWithFade()
                } else {
                    val targetKey = soundKeyFromIntent ?: currentSoundKey
                    if (targetKey != null) {
                        preparePlayerIfNeeded(targetKey)
                        startFadeIn()
                    }
                }
            }
            ACTION_STOP -> {
                stopPlaybackAndSelf()
            }
            ACTION_SET_SOUND -> {
                if (soundKeyFromIntent != null) {
                    preparePlayerIfNeeded(soundKeyFromIntent)
                    if (isPlaying) {
                        startFadeIn()
                    } else {
                        updateNotification(false)
                    }
                }
            }
            ACTION_DISMISS -> {
                stopPlaybackAndSelf()
            }
        }

        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        fadeAnimator?.cancel()
        fadeAnimator = null
        mediaPlayer?.release()
        mediaPlayer = null
        isPlaying = false
        currentSoundKey = null

        // ADDED — release media session
        mediaSession.isActive = false
        mediaSession.release()

        NotificationManagerCompat.from(this).cancel(NotificationHelper.NOTIFICATION_ID)
    }

    private fun preparePlayerIfNeeded(soundKey: String) {
        if (mediaPlayer != null && soundKey == currentSoundKey) {
            return
        }

        mediaPlayer?.release()
        mediaPlayer = null

        val resId = when (soundKey) {
            SOUND_OCEAN -> R.raw.ocean_waves
            SOUND_RAIN -> R.raw.rain
            SOUND_BROWN -> R.raw.rain
            else -> R.raw.ocean_waves
        }

        val player = MediaPlayer.create(this, resId)?.apply {
            isLooping = true
            setVolume(0f, 0f)
        }

        mediaPlayer = player
        currentSoundKey = soundKey
    }

    private fun startFadeIn() {
        val player = mediaPlayer ?: return

        fadeAnimator?.cancel()
        isPlaying = true

        // ADDED — update MediaSession to PLAYING
        mediaSession.setPlaybackState(
            playbackState.setState(PlaybackStateCompat.STATE_PLAYING, 0, 1f).build()
        )

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
                    startForeground(
                        NotificationHelper.NOTIFICATION_ID,
                        NotificationHelper.buildPlaybackNotification(
                            this@SoundPlaybackService,
                            true,
                            currentSoundKey,
                            mediaSession.sessionToken // ADDED
                        )
                    )
                }

                override fun onAnimationEnd(animation: Animator) {
                    updateNotification(true)
                }
            })
            start()
        }
    }

    private fun pauseWithFade() {
        val player = mediaPlayer ?: return

        fadeAnimator?.cancel()
        isPlaying = false

        // ADDED — update MediaSession to PAUSED
        mediaSession.setPlaybackState(
            playbackState.setState(PlaybackStateCompat.STATE_PAUSED, 0, 1f).build()
        )

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
                    stopForeground(false)
                    updateNotification(false)
                }
            })
            start()
        }
    }

    private fun stopPlaybackAndSelf() {
        fadeAnimator?.cancel()
        fadeAnimator = null
        isPlaying = false

        // ADDED — update MediaSession to STOPPED
        mediaSession.setPlaybackState(
            playbackState.setState(PlaybackStateCompat.STATE_STOPPED, 0, 1f).build()
        )

        mediaPlayer?.let {
            if (it.isPlaying) {
                it.stop()
            }
            it.release()
        }
        mediaPlayer = null
        currentSoundKey = null

        stopForeground(true)
        NotificationManagerCompat.from(this).cancel(NotificationHelper.NOTIFICATION_ID)
        stopSelf()
    }

    private fun updateNotification(isPlayingNow: Boolean) {
        val notification = NotificationHelper.buildPlaybackNotification(
            this,
            isPlayingNow,
            currentSoundKey,
            mediaSession.sessionToken // ADDED
        )
        NotificationManagerCompat.from(this).notify(
            NotificationHelper.NOTIFICATION_ID,
            notification
        )
    }

    companion object {
        const val ACTION_PLAY = "top.thinapps.relaxingsounds.action.PLAY"
        const val ACTION_PAUSE = "top.thinapps.relaxingsounds.action.PAUSE"
        const val ACTION_TOGGLE = "top.thinapps.relaxingsounds.action.TOGGLE"
        const val ACTION_STOP = "top.thinapps.relaxingsounds.action.STOP"
        const val ACTION_SET_SOUND = "top.thinapps.relaxingsounds.action.SET_SOUND"
        const val ACTION_DISMISS = "top.thinapps.relaxingsounds.action.DISMISS"

        const val EXTRA_SOUND_KEY = "extra_sound_key"

        const val SOUND_OCEAN = "ocean"
        const val SOUND_RAIN = "rain"
        const val SOUND_BROWN = "brown"

        private const val FADE_DURATION_MS = 800L
    }
}
