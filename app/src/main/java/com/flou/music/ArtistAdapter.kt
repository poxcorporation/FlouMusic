package com.flou.music

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.flou.music.data.repository.Artist

class ArtistAdapter(private var items: List<Artist>, private val onClick: (Artist) -> Unit) : RecyclerView.Adapter<ArtistAdapter.VH>() {
    class VH(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.tvFolderName)
        val count: TextView = view.findViewById(R.id.tvSongCount)
    }
    override fun onCreateViewHolder(p: ViewGroup, t: Int) = VH(LayoutInflater.from(p.context).inflate(R.layout.item_folder, p, false))
    override fun onBindViewHolder(h: VH, pos: Int) {
        val a = items[pos]
        h.name.text = a.name
        h.count.text = "${a.songCount} canciones"
        h.itemView.setOnClickListener { onClick(a) }
    }
    override fun getItemCount() = items.size
    fun update(l: List<Artist>) { items = l; notifyDataSetChanged() }
}