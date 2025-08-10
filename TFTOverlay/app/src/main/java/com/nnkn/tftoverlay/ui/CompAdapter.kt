package com.nnkn.tftoverlay.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.nnkn.tftoverlay.R
import com.nnkn.tftoverlay.data.Comp

class CompAdapter(private val items: List<Comp>) : RecyclerView.Adapter<CompAdapter.VH>() {

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val title: TextView = v.findViewById(R.id.txtTitle)
        val desc: TextView = v.findViewById(R.id.txtDesc)
        val units: TextView = v.findViewById(R.id.txtUnits)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_comp, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val c = items[position]
        holder.title.text = c.name
        holder.desc.text = c.description
        holder.units.text = c.units.joinToString(" • ")
    }

    override fun getItemCount(): Int = items.size
}
