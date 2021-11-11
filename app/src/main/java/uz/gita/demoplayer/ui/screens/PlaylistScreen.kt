package uz.gita.demoplayer.ui.screens

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import by.kirich1409.viewbindingdelegate.viewBinding
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import timber.log.Timber
import uz.gita.demoplayer.R
import uz.gita.demoplayer.data.Music
import uz.gita.demoplayer.data.MusicState
import uz.gita.demoplayer.data.ServiceCommand
import uz.gita.demoplayer.data.local.LocalStorage
import uz.gita.demoplayer.databinding.FragmentPlaylistBinding
import uz.gita.demoplayer.service.EventBus
import uz.gita.demoplayer.service.MusicService
import uz.gita.demoplayer.ui.adapters.MusicAdapter
import uz.gita.demoplayer.utils.Constants
import uz.gita.demoplayer.utils.extensions.checkPermission
import uz.gita.demoplayer.utils.extensions.getPlayListCursor
import uz.gita.demoplayer.utils.extensions.loadImage
import uz.gita.demoplayer.utils.extensions.toMusicData

class PlaylistScreen : Fragment(R.layout.fragment_playlist) {
    private val binding by viewBinding(FragmentPlaylistBinding::bind)
    private val storage: LocalStorage by lazy { LocalStorage(requireContext()) }
    private val adapter by lazy { MusicAdapter() }
    private var first = true
    private var one = true

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        (activity as AppCompatActivity?)!!.supportActionBar!!.show()

        if (one) {
            storage.isPlaying = false
            one = false
        }
        checkPermission(Manifest.permission.READ_EXTERNAL_STORAGE) {
            loadViews()
            loadData()
            loadObservers()
        }
    }

    private fun loadObservers() {
        EventBus.musicStateLiveData.observe(viewLifecycleOwner) {
            when (it) {
                is MusicState.PAUSE -> {
                    binding.btnPlayPause.setImageResource(R.drawable.ic_play_arrow)
                }
                is MusicState.PLAYING -> {
                    binding.btnPlayPause.setImageResource(R.drawable.ic_pause)
                    it.music?.let { it1 -> loadPlayingData(it1) }
                    adapter.notifyItemChanged(it.position)
                    adapter.notifyItemChanged(adapter.lastSelected)
                }
                is MusicState.STOP -> {
                    binding.btnPlayPause.setImageResource(R.drawable.ic_play_arrow)
                }
                is MusicState.NEXT_PREV -> {
                    binding.btnPlayPause.setImageResource(R.drawable.ic_pause)
                    it.music?.let { it1 -> loadPlayingData(it1) }
                }
            }
        }
    }

    private fun loadData() {
        requireActivity().getPlayListCursor()
            .onEach {
                adapter.swapCursor(it)
                if (/*storage.isPlaying &&*/ EventBus.musicStateLiveData.value != null) {
                    EventBus.musicStateLiveData.value?.music?.let { it1 -> loadPlayingData(it1) }
                } else {
                    storage.lastPlayedPosition.let { pos ->
                        if (it.moveToPosition(pos)) {
                            loadPlayingData(it.toMusicData())
                            startMusicService(ServiceCommand.INIT)
                        }
                    }
                }
            }
            .catch { Timber.e(this.toString()) }
            .launchIn(lifecycleScope)
    }

    private fun loadPlayingData(it: Music) {
        binding.apply {
            textName.text = it.title
            textAuthorName.text = it.artist
            if (it.imageUri != null) {
                musicPhoto.loadImage(it.imageUri)
            } else {
                musicPhoto.setImageResource(R.drawable.ic_music)
            }
        }
    }

    private fun loadViews() {
        binding.apply {
            list.layoutManager =
                LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
            list.adapter = adapter
            adapter.setOnItemClickListener {
                storage.lastPlayedPosition = it
                storage.lastPlayedDuration = 0
                startMusicService(ServiceCommand.PLAY_NEW)
                first = false
            }
            btnPrev.setOnClickListener {
                startMusicService(ServiceCommand.PREV)
            }
            btnNext.setOnClickListener {
                startMusicService(ServiceCommand.NEXT)
            }
            btnPlayPause.setOnClickListener {
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
            bottomBar.setOnClickListener {
                findNavController().navigate(PlaylistScreenDirections.openPlayScreen())
            }
            /**
             * Its used to make author name and title textview s scrollable(horizontally)
             */
            textName.isSelected = true
            textAuthorName.isSelected = true
        }
    }

    private fun startMusicService(serviceCommand: ServiceCommand) {
        val intent = Intent(context, MusicService::class.java)
        intent.putExtra(Constants.COMMAND_DATA, serviceCommand)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            requireContext().startForegroundService(intent)
        } else {
            requireContext().startService(intent)
        }
    }
}