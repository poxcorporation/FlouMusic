package com.flou.music

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.flou.music.data.repository.Album

class AlbumAdapter(private var items: List<Album>, private val onClick: (Album) -> Unit) : RecyclerView.Adapter<AlbumAdapter.VH>() {
    class VH(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.tvFolderName)
        val count: TextView = view.findViewById(R.id.tvSongCount)
        val icon: ImageView = view.findViewById(R.id.ivFolderIcon)
    }
    
    override fun onCreateViewHolder(p: ViewGroup, t: Int) = 
        VH(LayoutInflater.from(p.context).inflate(R.layout.item_folder, p, false))
    
    override fun onBindViewHolder(h: VH, pos: Int) {
        val a = items[pos]
        h.name.text = a.name
        h.count.text = "${a.artist} • ${a.songCount} canciones"
        a.artUri?.let { h.icon.setImageURI(it) }
        h.itemView.setOnClickListener { onClick(a) }
    }
    
    override fun getItemCount() = items.size
    
    fun update(l: List<Album>) { 
        items = l
        notifyDataSetChanged() 
    }
}