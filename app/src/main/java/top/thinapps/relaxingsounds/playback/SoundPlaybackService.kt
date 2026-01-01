package top.thinapps.relaxingsounds.playback

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.core.app.NotificationManagerCompat
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.datasource.RawResourceDataSource
import androidx.media3.exoplayer.ExoPlayer
import top.thinapps.relaxingsounds.core.ClickDebounce
import top.thinapps.relaxingsounds.core.SoundCatalog
import top.thinapps.relaxingsounds.notifications.NotificationHelper

class SoundPlaybackService : Service() {

    private var player: ExoPlayer? = null
    private var fadeAnimator: ValueAnimator? = null
    private var isPlaying: Boolean = false
    private var currentSoundKey: String? = null

    private lateinit var mediaSession: MediaSessionCompat
    private lateinit var playbackState: PlaybackStateCompat.Builder

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createChannelIfNeeded(this)

        mediaSession = MediaSessionCompat(this, "RelaxingSounds").apply {
            setFlags(
                MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or
                    MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS
            )
            isActive = true
        }

        playbackState = PlaybackStateCompat.Builder().setActions(
            PlaybackStateCompat.ACTION_PLAY or
                PlaybackStateCompat.ACTION_PAUSE or
                PlaybackStateCompat.ACTION_STOP
        )

        mediaSession.setPlaybackState(
            playbackState.setState(PlaybackStateCompat.STATE_PAUSED, 0, 1f).build()
        )

        mediaSession.setCallback(object : MediaSessionCompat.Callback() {
            override fun onPlay() {
                val key = currentSoundKey ?: return
                preparePlayerIfNeeded(key)
                startFadeIn()
            }

            override fun onPause() {
                pauseWithFade()
            }

            override fun onStop() {
                stopPlaybackAndSelf()
            }
        })
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

                // debounce for rapid user taps from notification
                if (!ClickDebounce.allowClick()) {
                    return START_NOT_STICKY
                }

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

            ACTION_REQUEST_STATE -> {
                broadcastPlaybackState(isPlaying, currentSoundKey)
            }
        }

        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()

        fadeAnimator?.cancel()
        fadeAnimator = null

        player?.release()
        player = null

        isPlaying = false
        currentSoundKey = null

        mediaSession.isActive = false
        mediaSession.release()

        NotificationManagerCompat.from(this).cancel(NotificationHelper.NOTIFICATION_ID)
    }

    private fun preparePlayerIfNeeded(soundKey: String) {
        if (player != null && soundKey == currentSoundKey) {
            return
        }

        fadeAnimator?.cancel()
        fadeAnimator = null

        player?.release()
        player = null

        val sound = SoundCatalog.getByKey(soundKey) ?: return
        val resId = sound.rawResId

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()

        val uri = RawResourceDataSource.buildRawResourceUri(resId)
        val mediaItem = MediaItem.fromUri(uri)

        val exo = ExoPlayer.Builder(this).build().apply {
            setAudioAttributes(audioAttributes, true)
            repeatMode = Player.REPEAT_MODE_ONE
            volume = 0f
            setMediaItem(mediaItem)
            prepare()
        }

        player = exo
        currentSoundKey = soundKey
    }

    private fun startFadeIn() {
        val exo = player ?: return

        fadeAnimator?.cancel()
        isPlaying = true

        mediaSession.setPlaybackState(
            playbackState.setState(PlaybackStateCompat.STATE_PLAYING, 0, 1f).build()
        )

        fadeAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = FADE_DURATION_MS
            interpolator = AccelerateDecelerateInterpolator()

            addUpdateListener { animator ->
                val v = animator.animatedValue as Float
                exo.volume = v
            }

            addListener(object : AnimatorListenerAdapter() {

                override fun onAnimationStart(animation: Animator) {
                    if (!exo.isPlaying) {
                        exo.play()
                    }

                    startForeground(
                        NotificationHelper.NOTIFICATION_ID,
                        NotificationHelper.buildPlaybackNotification(
                            this@SoundPlaybackService,
                            true,
                            currentSoundKey,
                            mediaSession.sessionToken
                        )
                    )

                    broadcastPlaybackState(true, currentSoundKey)
                }

                override fun onAnimationEnd(animation: Animator) {
                    updateNotification(true)
                }
            })

            start()
        }
    }

    private fun pauseWithFade() {
        val exo = player ?: return

        fadeAnimator?.cancel()
        isPlaying = false

        mediaSession.setPlaybackState(
            playbackState.setState(PlaybackStateCompat.STATE_PAUSED, 0, 1f).build()
        )

        broadcastPlaybackState(false, currentSoundKey)

        fadeAnimator = ValueAnimator.ofFloat(1f, 0f).apply {
            duration = FADE_DURATION_MS
            interpolator = AccelerateDecelerateInterpolator()

            addUpdateListener { animator ->
                val v = animator.animatedValue as Float
                exo.volume = v
            }

            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    exo.pause()

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

        mediaSession.setPlaybackState(
            playbackState.setState(PlaybackStateCompat.STATE_STOPPED, 0, 1f).build()
        )

        val previousKey = currentSoundKey

        player?.release()
        player = null
        currentSoundKey = null

        stopForeground(true)
        NotificationManagerCompat.from(this).cancel(NotificationHelper.NOTIFICATION_ID)

        broadcastPlaybackState(false, previousKey)
        stopSelf()
    }

    private fun updateNotification(isPlayingNow: Boolean) {
        val notification = NotificationHelper.buildPlaybackNotification(
            this,
            isPlayingNow,
            currentSoundKey,
            mediaSession.sessionToken
        )

        NotificationManagerCompat.from(this).notify(
            NotificationHelper.NOTIFICATION_ID,
            notification
        )
    }

    private fun broadcastPlaybackState(isPlayingNow: Boolean, soundKey: String?) {
        val intent = Intent(ACTION_PLAYBACK_STATE).apply {
            putExtra(EXTRA_IS_PLAYING, isPlayingNow)
            putExtra(EXTRA_CURRENT_SOUND_KEY, soundKey)
        }
        sendBroadcast(intent)
    }

    companion object {
        const val ACTION_PLAY = "top.thinapps.relaxingsounds.action.PLAY"
        const val ACTION_PAUSE = "top.thinapps.relaxingsounds.action.PAUSE"
        const val ACTION_TOGGLE = "top.thinapps.relaxingsounds.action.TOGGLE"
        const val ACTION_STOP = "top.thinapps.relaxingsounds.action.STOP"
        const val ACTION_SET_SOUND = "top.thinapps.relaxingsounds.action.SET_SOUND"
        const val ACTION_DISMISS = "top.thinapps.relaxingsounds.action.DISMISS"

        const val EXTRA_SOUND_KEY = "extra_sound_key"

        const val ACTION_PLAYBACK_STATE =
            "top.thinapps.relaxingsounds.action.PLAYBACK_STATE"
        const val EXTRA_IS_PLAYING = "extra_is_playing"
        const val EXTRA_CURRENT_SOUND_KEY = "extra_current_sound_key"

        const val ACTION_REQUEST_STATE =
            "top.thinapps.relaxingsounds.action.REQUEST_STATE"

        private const val FADE_DURATION_MS = 800L
    }
}
