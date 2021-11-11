package uz.gita.demoplayer.utils.extensions

import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import timber.log.Timber
import uz.gita.demoplayer.R
import uz.gita.demoplayer.app.App
import uz.gita.demoplayer.data.Music
import java.io.ByteArrayInputStream
import java.io.InputStream

const val ID = 0
const val ARTIST = 1
const val TITLE = 2
const val DATA = 3
const val DISPLAY_NAME = 4
const val DURATION = 5
const val ALBUM_ID = 6

val projection = arrayOf(
    MediaStore.Audio.Media._ID, //0
    MediaStore.Audio.Media.ARTIST, //1
    MediaStore.Audio.Media.TITLE, //2
    MediaStore.Audio.Media.DATA, //3
    MediaStore.Audio.Media.DISPLAY_NAME, //4
    MediaStore.Audio.Media.DURATION, //5
    MediaStore.Audio.Media.ALBUM_ID //6
)

fun Context.getPlayListCursor(): Flow<Cursor> = flow {
    //Some audio may be explicitly marked as not being music
    val selection = MediaStore.Audio.Media.IS_MUSIC + " != 0"

    val cursor: Cursor = contentResolver.query(
        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
        projection,
        selection,
        null,
        null
    ) ?: return@flow

    emit(cursor)
}.flowOn(Dispatchers.IO)

fun Context.getAudioInfo(path: String): Music? {
    val selection = MediaStore.Audio.Media.DATA + " = ?"
    val cursor: Cursor = contentResolver.query(
        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
        projection,
        selection,
        arrayOf(path),
        ""
    ) ?: return null

    var music: Music? = null
    while (cursor.moveToNext()) {
        music = cursor.toMusicData()
    }
    cursor.close()
    return music
}

fun Cursor.toMusicData(): Music {
    return Music(
        id = getLong(ID),
        artist = getString(ARTIST),
        title = getString(TITLE),
        data = getString(DATA),
        displayName = getString(DISPLAY_NAME),
        duration = getLong(DURATION),
        imageUri = App.instance.songArt(getLong(ALBUM_ID))
    )
}

fun Context.songArt(path: String): Bitmap? {
    val retriever = MediaMetadataRetriever()
    val inputStream: InputStream
    retriever.setDataSource(path)
    return if (retriever.embeddedPicture != null) {
        inputStream = ByteArrayInputStream(retriever.embeddedPicture)
        val bitmap = BitmapFactory.decodeStream(inputStream)
        retriever.release()
        bitmap
    } else {
        retriever.release()
        getLargeIcon(this)
    }
}

fun Context.songArt(albumId: Long): Uri? {
    try {
        val sArtworkUri: Uri = Uri
            .parse("content://media/external/audio/albumart")
        val uri = ContentUris.withAppendedId(sArtworkUri, albumId)
        val pfd: ParcelFileDescriptor? = this.contentResolver
            .openFileDescriptor(uri, "r")
        if (pfd != null) {
            return uri
        }
    } catch (e: Exception) {
        Timber.e(e.message.toString())
    }
    return null
}

private fun getLargeIcon(context: Context): Bitmap? {
    return BitmapFactory.decodeResource(context.resources, R.drawable.ic_music)
}