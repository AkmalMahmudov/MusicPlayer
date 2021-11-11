package uz.gita.demoplayer.service

import android.annotation.SuppressLint
import android.app.*
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.graphics.BitmapFactory
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import timber.log.Timber
import uz.gita.demoplayer.R
import uz.gita.demoplayer.data.Music
import uz.gita.demoplayer.data.MusicState
import uz.gita.demoplayer.data.ServiceCommand
import uz.gita.demoplayer.data.local.LocalStorage
import uz.gita.demoplayer.utils.Constants
import uz.gita.demoplayer.utils.extensions.getPlayListCursor
import uz.gita.demoplayer.utils.extensions.timberErrorLog
import uz.gita.demoplayer.utils.extensions.timberLog
import uz.gita.demoplayer.utils.extensions.toMusicData
import java.io.File

class MusicService : Service() {
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var mediaPlayer: MediaPlayer? = null
    private var cursor: Cursor? = null
    private val storage: LocalStorage by lazy { LocalStorage(applicationContext) }
    private var currentMusic: Music? = null
    private var finish = false
    private var progress = 0
    private val notificationBuilder by lazy {
        NotificationCompat.Builder(this, getString(R.string.default_notification_channel_id))
            .setContentTitle("MyPlayer")
            .setSmallIcon(R.drawable.ic_music)
            .setLargeIcon(BitmapFactory.decodeResource(resources, R.drawable.ic_music))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCustomContentView(createView())
    }

    override fun onCreate() {
        super.onCreate()
        prepareMediaPlayer()
        startForeground()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "My Player"
            val descriptionText = "GITA"
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(
                getString(R.string.default_notification_channel_id),
                name,
                importance
            ).apply {
                description = descriptionText
            }
            // Register the channel with the system
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    @SuppressLint("UnspecifiedImmutableFlag")
    private fun createPendingIntent(serviceCommand: ServiceCommand): PendingIntent {
        val intent = Intent(this, MusicService::class.java)
        intent.putExtra(Constants.COMMAND_DATA, serviceCommand)
        intent.putExtra(Constants.MUSIC_POSITION, storage.lastPlayedPosition)
        return PendingIntent.getService(
            this, serviceCommand.ordinal, intent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private fun createView(): RemoteViews {
        val remote = RemoteViews(packageName, R.layout.notifcation_view)
        cursor?.apply {
            if (moveToPosition(storage.lastPlayedPosition)) {
                val data = toMusicData()
                remote.setTextViewText(R.id.text_name, data.title)
                remote.setTextViewText(R.id.text_author_name, data.artist)
                if (data.imageUri == null) {
                    remote.setImageViewResource(R.id.image, R.drawable.ic_music)
                } else {
                    data.imageUri.let { remote.setImageViewUri(R.id.image, it) }
                }
                when {
                    mediaPlayer?.isPlaying == true -> {
                        remote.setImageViewResource(R.id.btn_play_pause, R.drawable.ic_pause)
                    }
                    mediaPlayer?.isPlaying != true -> {
                        remote.setImageViewResource(R.id.btn_play_pause, R.drawable.ic_play_arrow)
                    }
                }

                remote.setOnClickPendingIntent(
                    R.id.btn_prev,
                    createPendingIntent(ServiceCommand.PREV)
                )
                remote.setOnClickPendingIntent(
                    R.id.btn_play_pause,
                    createPendingIntent(ServiceCommand.PLAY_PAUSE)
                )
                remote.setOnClickPendingIntent(
                    R.id.btn_next,
                    createPendingIntent(ServiceCommand.NEXT)
                )
                remote.setOnClickPendingIntent(
                    R.id.close, createPendingIntent(ServiceCommand.CLOSE)
                )
            }
        }
        return remote
    }

    private fun startForeground() {
        createNotificationChannel()
        startForeground(1, getNotification())
    }

    private fun prepareMediaPlayer() {
        cursor?.let { c ->
            val b = storage.lastPlayedPosition.let { c.moveToPosition(it) }
            timberLog(b.toString())
            if (b) {
                currentMusic = c.toMusicData()
                mediaPlayer?.apply {
                    stop()
                    prepare()
                }
                mediaPlayer =
                    MediaPlayer.create(this, Uri.fromFile(File(currentMusic?.data ?: ""))).apply {
                        setOnCompletionListener {
                            timberErrorLog("onComplete")
                            nextMusic()
                        }
                        if (storage.lastPlayedDuration != 0) {
                            seekTo(storage.lastPlayedDuration)
                        }
                    }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val command =
            intent?.extras?.getSerializable(Constants.COMMAND_DATA) as? ServiceCommand
        progress = intent?.extras?.getInt(Constants.SEEK_BAR) ?: 0
        if (cursor == null) {
            getPlayListCursor()
                .onEach { cursor ->
                    this.cursor = cursor
                    doCommand(command)
                }
                .catch { timberErrorLog("Error on getPlayList: $this") }
                .launchIn(serviceScope)
        } else {
            doCommand(command)
        }
        return START_NOT_STICKY
    }

    private fun doCommand(serviceCommand: ServiceCommand?) {

        when (serviceCommand) {
            ServiceCommand.PLAY_NEW -> {
                if (finish) {
                    startForeground()
                    finish = false
                }
                prepareMediaPlayer()
                mediaPlayer?.start()
                runSeekBar()
                notifyNotification()

                EventBus.musicStateLiveData.postValue(
                    MusicState.PLAYING(
                        storage.lastPlayedPosition,
                        currentMusic
                    )
                )
                storage.isPlaying = true
            }
            ServiceCommand.PLAY_PAUSE -> {
                if (finish) {
                    startForeground()
                    finish = false
                }
                if (mediaPlayer?.isPlaying == true) {
                    mediaPlayer?.pause()
                    notifyNotification()
                    EventBus.musicStateLiveData.postValue(
                        MusicState.PAUSE(
                            storage.lastPlayedPosition,
                            currentMusic
                        )
                    )
                    storage.isPlaying = false
                } else {
                    mediaPlayer?.start()
                    notifyNotification()
                    runSeekBar()
                    EventBus.musicStateLiveData.postValue(
                        MusicState.PLAYING(
                            storage.lastPlayedPosition,
                            currentMusic
                        )
                    )
                    storage.isPlaying = true
                }
            }
            ServiceCommand.PREV -> {
                prevMusic()
            }
            ServiceCommand.NEXT -> {
                nextMusic()
            }
            ServiceCommand.STOP -> {
                mediaPlayer?.pause()
                EventBus.musicStateLiveData.postValue(
                    MusicState.STOP(
                        storage.lastPlayedPosition,
                        currentMusic
                    )
                )
                storage.isPlaying = false
            }
            ServiceCommand.CLOSE -> {
                mediaPlayer?.pause()
                notifyNotification()
                EventBus.musicStateLiveData.postValue(
                    MusicState.PAUSE(
                        storage.lastPlayedPosition,
                        currentMusic
                    )
                )
                storage.isPlaying = false
                stopForeground(false)
                finish = true
                notifyNotification()
            }
            ServiceCommand.SEEK_BAR -> {
                mediaPlayer?.seekTo(progress)
            }
            ServiceCommand.INIT -> {
                if (mediaPlayer == null) {
                    prepareMediaPlayer()
                }
                notifyNotification()
                EventBus.musicStateLiveData.postValue(
                    MusicState.STOP(
                        storage.lastPlayedPosition,
                        currentMusic
                    )
                )
                storage.isPlaying = false
            }
            else -> {
            }
        }
    }

    private fun prevMusic() {
        cursor?.let { c ->
            if (c.moveToPrevious()) {
                storage.lastPlayedPosition = c.position
                currentMusic = c.toMusicData()
            } else {
                if (c.moveToLast()) {
                    storage.lastPlayedPosition = c.position
                    currentMusic = c.toMusicData()
                }
            }

            try {
                mediaPlayer?.apply {
                    stop()
                    prepare()
                }

                mediaPlayer =
                    MediaPlayer.create(this, Uri.fromFile(File(currentMusic?.data ?: ""))).apply {
                        storage.lastPlayedDuration = 0

                        setOnCompletionListener {
                            timberErrorLog("onComplete")
                            nextMusic()
                        }
                        start()
                        runSeekBar()
                    }
                startForeground(Constants.notificationId, getNotification())

                EventBus.musicStateLiveData.postValue(
                    MusicState.NEXT_PREV(
                        storage.lastPlayedPosition,
                        currentMusic
                    )
                )
                storage.isPlaying = true
            } catch (e: Exception) {
                timberErrorLog(e.message.toString())
            }
        }

    }

    private fun nextMusic() {
        cursor?.let { c ->
            if (c.moveToNext()) {
                storage.lastPlayedPosition = c.position
                currentMusic = c.toMusicData()
            } else {
                if (c.moveToFirst()) {
                    storage.lastPlayedPosition = c.position
                    currentMusic = c.toMusicData()
                }
            }

            try {
                mediaPlayer?.apply {
                    stop()
                    prepare()
                }
                mediaPlayer =
                    MediaPlayer.create(this, Uri.fromFile(File(currentMusic?.data ?: ""))).apply {
                        storage.lastPlayedDuration = 0
                        setOnCompletionListener {
                            timberErrorLog("onComplete")
                            nextMusic()
                        }
                        start()
                        runSeekBar()
                    }
                notifyNotification()
                startForeground(Constants.notificationId, getNotification())
                EventBus.musicStateLiveData.postValue(
                    MusicState.NEXT_PREV(
                        storage.lastPlayedPosition,
                        currentMusic
                    )
                )
                storage.isPlaying = true
            } catch (e: Exception) {

                /*EventBus.musicStateLiveData.postValue(
                    MusicState.NEXT_PREV(
                        storage.lastPlayedPosition,
                        currentMusic
                    )
                )*/
                Timber.d("fucked up")
                timberErrorLog(e.message.toString())
            }
        }
    }

    private fun getNotification(builder: NotificationCompat.Builder? = null): Notification =
        (builder ?: notificationBuilder)
            .setCustomContentView(createView())
            .build()

    private fun notifyNotification() {
        val mNotificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        mNotificationManager?.notify(Constants.notificationId, getNotification())
        if (finish) {
            mNotificationManager?.cancel(1)
        }
    }

    private fun runSeekBar() {
        serviceScope.launch {
            while (mediaPlayer?.currentPosition?.div(1000) ?: 0 <= mediaPlayer?.duration?.div(1000)!!) {
                EventBus.currentValueLiveData.postValue(mediaPlayer?.currentPosition)
                delay(500)
            }
        }
    }

    override fun onBind(p0: Intent): IBinder? = null
}