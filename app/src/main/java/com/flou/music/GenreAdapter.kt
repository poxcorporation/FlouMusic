package com.flou.music

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.flou.music.data.repository.Genre

class GenreAdapter(private var items: List<Genre>, private val onClick: (Genre) -> Unit) : RecyclerView.Adapter<GenreAdapter.VH>() {
    class VH(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.tvFolderName)
        val count: TextView = view.findViewById(R.id.tvSongCount)
    }
    override fun onCreateViewHolder(p: ViewGroup, t: Int) = VH(LayoutInflater.from(p.context).inflate(R.layout.item_folder, p, false))
    override fun onBindViewHolder(h: VH, pos: Int) {
        val g = items[pos]
        h.name.text = g.name
        h.count.text = "${g.songCount} canciones"
        h.itemView.setOnClickListener { onClick(g) }
    }
    override fun getItemCount() = items.size
    fun update(l: List<Genre>) { items = l; notifyDataSetChanged() }
}