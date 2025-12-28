package top.thinapps.relaxingsounds.notifications

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import android.support.v4.media.session.MediaSessionCompat
import top.thinapps.relaxingsounds.R
import top.thinapps.relaxingsounds.playback.SoundPlaybackService
import top.thinapps.relaxingsounds.ui.SoundDetailActivity
import top.thinapps.relaxingsounds.core.SoundCatalog

object NotificationHelper {

    const val CHANNEL_ID = "relaxing_sounds_playback"
    const val NOTIFICATION_ID = 1001

    fun createChannelIfNeeded(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val mgr = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (mgr.getNotificationChannel(CHANNEL_ID) == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    context.getString(R.string.notification_channel_playback),
                    NotificationManager.IMPORTANCE_LOW
                )
                mgr.createNotificationChannel(channel)
            }
        }
    }

    fun buildPlaybackNotification(
        context: Context,
        isPlaying: Boolean,
        soundKey: String?,
        sessionToken: MediaSessionCompat.Token
    ): Notification {

        val title = getSoundTitle(context, soundKey)
        val statusText = if (isPlaying) {
            context.getString(R.string.notification_status_playing)
        } else {
            context.getString(R.string.notification_status_paused)
        }

        // Open detail screen without triggering playback
        val contentIntent = PendingIntent.getActivity(
            context,
            991,
            Intent(context, SoundDetailActivity::class.java).apply {
                putExtra(SoundDetailActivity.EXTRA_SOUND_KEY, soundKey)
                putExtra(
                    SoundDetailActivity.EXTRA_LAUNCH_SOURCE,
                    SoundDetailActivity.SOURCE_NOTIFICATION
                )
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Clean toggle intent. Debounce is handled INSIDE the service.
        val togglePendingIntent = PendingIntent.getService(
            context,
            1,
            Intent(context, SoundPlaybackService::class.java).apply {
                action = SoundPlaybackService.ACTION_TOGGLE
                putExtra(SoundPlaybackService.EXTRA_SOUND_KEY, soundKey)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val dismissPendingIntent = PendingIntent.getService(
            context,
            2,
            Intent(context, SoundPlaybackService::class.java).apply {
                action = SoundPlaybackService.ACTION_DISMISS
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val toggleAction = NotificationCompat.Action(
            if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play,
            if (isPlaying)
                context.getString(R.string.notification_action_pause)
            else
                context.getString(R.string.notification_action_play),
            togglePendingIntent
        )

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_small)
            .setContentTitle(title)
            .setContentText(statusText)
            .setContentIntent(contentIntent)
            .setDeleteIntent(dismissPendingIntent)
            .setOngoing(isPlaying)
            .setOnlyAlertOnce(true)
            .setStyle(
                androidx.media.app.NotificationCompat.MediaStyle()
                    .setMediaSession(sessionToken)
                    .setShowActionsInCompactView(0)
            )
            .addAction(toggleAction)
            .build()
    }

    private fun getSoundTitle(context: Context, soundKey: String?): String {
        val sound = soundKey?.let { SoundCatalog.getByKey(it) }
        return if (sound != null) {
            context.getString(sound.titleResId)
        } else {
            context.getString(R.string.app_name)
        }
    }

    fun cancel(context: Context) {
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
    }
}
