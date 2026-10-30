package com.flou.music

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.flou.music.data.model.Folder

class FolderAdapter(
    private var folders: List<Folder>,
    private val onItemClick: (Folder) -> Unit
) : RecyclerView.Adapter<FolderAdapter.FolderViewHolder>() {

    class FolderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvFolderName: TextView = view.findViewById(R.id.tvFolderName)
        val tvSongCount: TextView = view.findViewById(R.id.tvSongCount)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FolderViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_folder, parent, false)
        return FolderViewHolder(view)
    }

    override fun onBindViewHolder(holder: FolderViewHolder, position: Int) {
        val folder = folders[position]
        holder.tvFolderName.text = folder.name
        holder.tvSongCount.text = "${folder.songCount} canciones"
        holder.itemView.setOnClickListener { onItemClick(folder) }
    }

    override fun getItemCount() = folders.size

    fun update(newFolders: List<Folder>) {
        folders = newFolders
        notifyDataSetChanged()
    }
}