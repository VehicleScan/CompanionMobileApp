// DtcAdapter.kt
package com.ities45.vehiclescan_companion.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.ities45.vehiclescan_companion.R
import com.ities45.vehiclescan_companion.model.DtcItem

class DtcAdapter(private val dtcList: List<DtcItem>) : RecyclerView.Adapter<DtcAdapter.DtcViewHolder>() {

    class DtcViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val nameText: TextView = view.findViewById(R.id.dtc_name)
        val moduleText: TextView = view.findViewById(R.id.dtc_module)
        val icon: ImageView = view.findViewById(R.id.dtc_icon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DtcViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_dtc, parent, false)
        return DtcViewHolder(view)
    }

    override fun onBindViewHolder(holder: DtcViewHolder, position: Int) {
        val item = dtcList[position]
        holder.moduleText.text = item.module
        holder.nameText.text = item.name
        holder.icon.setImageResource(item.iconResId)
    }

    override fun getItemCount(): Int = dtcList.size
}
