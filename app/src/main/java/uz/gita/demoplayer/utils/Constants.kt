package uz.gita.demoplayer.utils

class Constants {

    companion object {

        // Intent Constants
        const val MUSIC_POSITION = "MUSIC_POSITION"
        const val SEEK_BAR = "SEEK_BAR"
        const val COMMAND_DATA: String = "COMMAND_DATA"
        const val ACTION_PLAYER = "uz.gita.demoplayer.utils.ACTION_PLAYER"
        const val NOTIFICATION_ACTION_PLAYER = "uz.gita.demoplayer.utils.NOTIFICATION_ACTION_PLAYER"

        // Notification Constants
        const val channelID = "music_player_notification_channel_id"
        const val foregroundServiceNotificationTitle = "Music Player"
        const val notificationId = 1
        const val foregroundIntentServiceNotificationTitle = "My Foreground Intent Service"
        const val notificationChannelName = "Music Player Service Channel"
    }

}