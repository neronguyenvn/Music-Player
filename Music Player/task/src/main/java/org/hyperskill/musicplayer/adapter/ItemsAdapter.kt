package org.hyperskill.musicplayer.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import org.hyperskill.musicplayer.R
import org.hyperskill.musicplayer.helper.formatMilliseconds
import org.hyperskill.musicplayer.model.Item


class ItemsAdapter(
    private val items: MutableList<Item>,
    private val onTrackPlayOrPause: (Int) -> Unit,
    private val onTrackLongClick: (Int) -> Unit,
    private val onSongSelectorClick: (Boolean, Int) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    fun update(newItems: List<Item>) {
        val diffCallback = ItemsDiffCallback(items, newItems)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        this.items.clear()
        this.items.addAll(newItems)
        diffResult.dispatchUpdatesTo(this)
    }

    fun updateCurrentTrack(track: Item.Track?) {
        val newItems = if (track == null) {
            (items as List<Item.Track>).map { it.copy(state = Item.Track.TrackState.PAUSED) }
        } else {
            (items as List<Item.Track>).map {
                if (it.song.id == track.song.id) it.copy(state = Item.Track.TrackState.PLAYING)
                else it.copy(state = Item.Track.TrackState.PAUSED)
            }
        }
        val diffCallback = ItemsDiffCallback(items, newItems)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        this.items.clear()
        this.items.addAll(newItems)
        diffResult.dispatchUpdatesTo(this)
    }

    override fun getItemCount(): Int {
        return items.size
    }

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is Item.Track -> 0
            is Item.SongSelector -> 1
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            0 -> {
                TrackViewHolder(
                    LayoutInflater.from(parent.context)
                        .inflate(R.layout.list_item_song, parent, false)
                ).apply {
                    songItem.setOnLongClickListener { _ ->
                        val pos = bindingAdapterPosition
                        if (pos >= 0) {
                            onTrackLongClick(pos)
                            true
                        } else false
                    }
                    songItemImgBtnPlayPause.setOnClickListener {
                        val pos = bindingAdapterPosition
                        if (pos >= 0) {
                            onTrackPlayOrPause(pos)
                        }
                    }
                }
            }

            1 -> {
                SongSelectorViewHolder(
                    LayoutInflater.from(parent.context)
                        .inflate(R.layout.list_item_song_selector, parent, false)
                ).apply {
                    songSelectorItem.setOnClickListener {
                        songSelectorItemCheckBox.apply { isChecked = !isChecked }
                    }
                    songSelectorItemCheckBox.setOnCheckedChangeListener { _, isChecked ->
                        val pos = bindingAdapterPosition
                        if (pos >= 0) {
                            onSongSelectorClick(isChecked, pos)
                        }
                    }
                }
            }

            else -> throw AssertionError()
        }
    }


    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is Item.Track -> {
                (holder as TrackViewHolder).apply {
                    songItemTvArtist.text = item.song.artist
                    songItemTvTitle.text = item.song.title
                    songItemTvDuration.text = formatMilliseconds(item.song.duration)
                    songItemImgBtnPlayPause.setImageResource(
                        when (item.state) {
                            Item.Track.TrackState.PLAYING -> R.drawable.ic_pause
                            else -> R.drawable.ic_play
                        }
                    )
                }
            }

            is Item.SongSelector -> {
                (holder as SongSelectorViewHolder).apply {
                    songSelectorItemTvArtist.text = item.song.artist
                    songSelectorItemTvTitle.text = item.song.title
                    songSelectorItemTvDuration.text = formatMilliseconds(item.song.duration)
                    songSelectorItemCheckBox.isChecked = item.isSelected
                    if (songSelectorItemCheckBox.isChecked) {
                        holder.songSelectorItem.setBackgroundColor(Color.LTGRAY)
                    } else {
                        holder.songSelectorItem.setBackgroundColor(Color.WHITE)
                    }
                }
            }
        }
    }

    class TrackViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val songItem: ConstraintLayout = view.findViewById(R.id.songItem)
        val songItemImgBtnPlayPause: ImageView = view.findViewById(R.id.songItemImgBtnPlayPause)
        val songItemTvArtist: TextView = view.findViewById(R.id.songItemTvArtist)
        val songItemTvTitle: TextView = view.findViewById(R.id.songItemTvTitle)
        val songItemTvDuration: TextView = view.findViewById(R.id.songItemTvDuration)
    }

    class SongSelectorViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val songSelectorItem: ConstraintLayout = view.findViewById(R.id.songSelectorItem)
        val songSelectorItemCheckBox: CheckBox = view.findViewById(R.id.songSelectorItemCheckBox)
        val songSelectorItemTvArtist: TextView = view.findViewById(R.id.songSelectorItemTvArtist)
        val songSelectorItemTvTitle: TextView = view.findViewById(R.id.songSelectorItemTvTitle)
        val songSelectorItemTvDuration: TextView =
            view.findViewById(R.id.songSelectorItemTvDuration)
    }
}

class ItemsDiffCallback(
    private val oldItems: List<Item>,
    private val newItems: List<Item>
) : DiffUtil.Callback() {

    override fun getOldListSize(): Int = oldItems.size

    override fun getNewListSize(): Int = newItems.size

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        val oldItem = oldItems[oldItemPosition]
        val newItem = newItems[newItemPosition]
        return oldItem::class == newItem::class && when (oldItem) {
            is Item.Track -> {
                newItem as Item.Track
                oldItem.song.id == newItem.song.id
            }

            is Item.SongSelector -> {
                newItem as Item.SongSelector
                oldItem.song.id == newItem.song.id
            }
        }
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        val oldItem = oldItems[oldItemPosition]
        val newItem = newItems[newItemPosition]
        return oldItem == newItem
    }
}
