package uz.gita.demoplayer.ui.screens

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import by.kirich1409.viewbindingdelegate.viewBinding
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import timber.log.Timber
import uz.gita.demoplayer.R
import uz.gita.demoplayer.data.Music
import uz.gita.demoplayer.data.MusicState
import uz.gita.demoplayer.data.ServiceCommand
import uz.gita.demoplayer.data.local.LocalStorage
import uz.gita.demoplayer.databinding.FragmentInfoBinding
import uz.gita.demoplayer.service.EventBus
import uz.gita.demoplayer.service.MusicService
import uz.gita.demoplayer.utils.Constants
import uz.gita.demoplayer.utils.extensions.getPlayListCursor
import uz.gita.demoplayer.utils.extensions.loadImage
import uz.gita.demoplayer.utils.extensions.time
import uz.gita.demoplayer.utils.extensions.toMusicData
import androidx.appcompat.app.AppCompatActivity




class InfoScreen : Fragment(R.layout.fragment_info) {
    private val binding by viewBinding(FragmentInfoBinding::bind)
    private val storage: LocalStorage by lazy { LocalStorage(requireContext()) }
    private var first = true
    private var liked = false
    private var shuffled = false
    private var repeated = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        Timber.d("Hello Wolrd")
        loadViews()
        loadData()
        loadObservers()
    }

    private fun loadObservers() {
        EventBus.musicStateLiveData.observe(viewLifecycleOwner) {
            when (it) {
                is MusicState.PAUSE -> {
                    binding.playPause.setImageResource(R.drawable.ic_play)
                }
                is MusicState.PLAYING -> {
                    binding.playPause.setImageResource(R.drawable.ic_pause_circle)
                    it.music?.let { it1 -> loadPlayingData(it1) }
                }
                is MusicState.STOP -> {
                    binding.seekBar.progress = 0
                    binding.playPause.setImageResource(R.drawable.ic_play)
                }
                is MusicState.NEXT_PREV -> {
                    binding.playPause.setImageResource(R.drawable.ic_pause_circle)
                    it.music?.let { it1 -> loadPlayingData(it1) }
                }
            }
        }
        EventBus.currentValueLiveData.observe(viewLifecycleOwner) {
            binding.seekBar.progress = it
            binding.goneTime.text = it.toLong().time
        }
    }

    private fun loadData() {
        requireActivity().getPlayListCursor()
            .onEach {
                if (storage.isPlaying && EventBus.musicStateLiveData.value != null) {
                    EventBus.musicStateLiveData.value?.music?.let { it1 -> loadPlayingData(it1) }
                } else {
                    storage.lastPlayedPosition.let { pos ->
                        if (it.moveToPosition(pos)) {
                            loadPlayingData(it.toMusicData())
                            startMusicService(ServiceCommand.STOP)
                        }
                    }
                }
            }
            .catch { Timber.e(this.toString()) }
            .launchIn(lifecycleScope)
    }

    private fun loadPlayingData(music: Music) {
        binding.apply {
            name.text = music.title
            artist.text = music.artist
            music.imageUri.let { uri -> photo.loadImage(uri) }
            music.duration?.let {
                seekBar.max = it.toInt()
                fullTime.text = it.time
                storage.lastPlayedDuration.let { progress ->
                    seekBar.progress = progress
                    goneTime.text = progress.toLong().time
                }
            }

        }
    }

    private fun loadViews() {
        (activity as AppCompatActivity?)!!.supportActionBar!!.hide()
//        requireActivity().actionBar?.hide()
        binding.apply {
            playPause.setOnClickListener {
                if (first) {
                    if (storage.isPlaying) {
                        startMusicService(ServiceCommand.PLAY_PAUSE)
                    } else {
                        startMusicService(ServiceCommand.PLAY_NEW)
                    }
                    first = false
                } else {
                    startMusicService(ServiceCommand.PLAY_PAUSE)
                }
            }
            next.setOnClickListener {
                startMusicService(ServiceCommand.NEXT)
            }
            prev.setOnClickListener {
                startMusicService(ServiceCommand.PREV)
            }
            heart.setOnClickListener {
                liked = if (liked) {
                    heart.setImageResource(R.drawable.ic_like)
                    view?.let { it1 ->
                        Snackbar.make(it1, "Removed from liked songs", Snackbar.LENGTH_SHORT)
                            .setAction("Action", null).show()
                    }
                    false
                } else {
                    heart.setImageResource(R.drawable.ic_heart)
                    view?.let { it1 ->
                        Snackbar.make(it1, "Added to liked songs", Snackbar.LENGTH_SHORT)
                            .setAction("Action", null).show()
                    }
                    true
                }
            }
            shuffle.setOnClickListener {
                shuffled = if (shuffled) {
                    shuffle.setImageResource(R.drawable.ic_shuffle_off)
                    false
                } else {
                    shuffle.setImageResource(R.drawable.ic_shuffle_on)
                    true
                }
            }
            repeat.setOnClickListener {
                repeated = if (repeated) {
                    repeat.setImageResource(R.drawable.ic_repeat_off)
                    false
                } else {
                    repeat.setImageResource(R.drawable.ic_repeat_on)
                    true
                }
            }
            btnBack.setOnClickListener { findNavController().navigateUp() }
            seekBar.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                    if (fromUser) {
                        seekBar.progress = progress
                        goneTime.text = progress.toLong().time
                        startMusicService(ServiceCommand.SEEK_BAR, progress)
//                        EventBus.progressChangeLiveData.postValue(progress)
                    }
                }

                override fun onStopTrackingTouch(seekBar: SeekBar) {}
                override fun onStartTrackingTouch(seekBar: SeekBar) {}
            })

            /**
             * Its used to make author name and title textview s scrollable(horizontally)
             */
            name.isSelected = true
            artist.isSelected = true
        }
    }

    private fun startMusicService(serviceCommand: ServiceCommand, progress: Int? = null) {
        val intent = Intent(context, MusicService::class.java)
        intent.putExtra(Constants.COMMAND_DATA, serviceCommand)
        intent.putExtra(Constants.SEEK_BAR, progress)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            requireContext().startForegroundService(intent)
        } else {
            requireContext().startService(intent)
        }
    }
}
