package uz.gita.demoplayer.data

sealed class MusicState(val position: Int, val music: Music?) {
    class PLAYING(position: Int, data: Music?) : MusicState(position, data)
    class PAUSE(position: Int, data: Music?) : MusicState(position, data)
    class STOP(position: Int, data: Music?) : MusicState(position, data)
    class NEXT_PREV(position: Int, data: Music?) : MusicState(position, data)
}