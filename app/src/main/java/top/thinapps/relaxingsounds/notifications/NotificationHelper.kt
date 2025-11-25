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

object NotificationHelper {

    const val CHANNEL_ID = "relaxing_sounds_playback"
    const val NOTIFICATION_ID = 1001

    fun createChannelIfNeeded(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val existing = manager.getNotificationChannel(CHANNEL_ID)
            if (existing == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    context.getString(R.string.notification_channel_playback),
                    NotificationManager.IMPORTANCE_LOW
                )
                manager.createNotificationChannel(channel)
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

        val contentIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, SoundDetailActivity::class.java).apply {
                putExtra(SoundDetailActivity.EXTRA_SOUND_KEY, soundKey)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val toggleIntent = Intent(context, SoundPlaybackService::class.java).apply {
            action = SoundPlaybackService.ACTION_TOGGLE
            putExtra(SoundPlaybackService.EXTRA_SOUND_KEY, soundKey)
        }
        val togglePendingIntent = PendingIntent.getService(
            context,
            1,
            toggleIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val dismissIntent = Intent(context, SoundPlaybackService::class.java).apply {
            action = SoundPlaybackService.ACTION_DISMISS
        }
        val dismissPendingIntent = PendingIntent.getService(
            context,
            2,
            dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val toggleActionIcon = if (isPlaying) {
            R.drawable.ic_pause
        } else {
            R.drawable.ic_play
        }
        val toggleActionTitle = if (isPlaying) {
            context.getString(R.string.notification_action_pause)
        } else {
            context.getString(R.string.notification_action_play)
        }

        val toggleAction = NotificationCompat.Action(
            toggleActionIcon,
            toggleActionTitle,
            togglePendingIntent
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
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

        return builder.build()
    }

    private fun getSoundTitle(context: Context, soundKey: String?): String {
        return when (soundKey) {
            SoundPlaybackService.SOUND_OCEAN -> context.getString(R.string.sound_ocean_title)
            SoundPlaybackService.SOUND_RAIN -> context.getString(R.string.sound_rain_title)
            SoundPlaybackService.SOUND_BROWN -> context.getString(R.string.sound_brown_title)
            else -> context.getString(R.string.app_name)
        }
    }

    fun cancel(context: Context) {
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
    }
}
