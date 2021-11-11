package uz.gita.demoplayer.ui.adapters

import android.database.Cursor
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import uz.gita.demoplayer.R
import uz.gita.demoplayer.databinding.ItemTracklistBinding
import uz.gita.demoplayer.utils.CursorAdapter
import uz.gita.demoplayer.utils.extensions.loadImage
import uz.gita.demoplayer.utils.extensions.toMusicData

class MusicAdapter : CursorAdapter<MusicAdapter.MusicViewHolder>() {
    private var itemClickListener: OnItemClick? = null
    var lastSelected = 0

    inner class MusicViewHolder(private val binding: ItemTracklistBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(position: Int) {
            binding.apply {
                val data = cursor.toMusicData()

                musicName.text = data.title
                musicArtist.text = data.artist

                root.setOnClickListener { itemClickListener?.onClick(position) }
                if (data.imageUri == null) {
                    musicPhoto.setImageResource(R.drawable.ic_music)
                } else {
                    musicPhoto.loadImage(data.imageUri!!)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MusicViewHolder {
        return MusicViewHolder(
            ItemTracklistBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    fun setOnItemClickListener(block: OnItemClick) {
        this.itemClickListener = block
    }

    fun interface OnItemClick {
        fun onClick(itemPosition: Int)
    }

    override fun onBindViewHolder(holder: MusicViewHolder, cursor: Cursor, position: Int) {
        holder.bind(position)
    }
}