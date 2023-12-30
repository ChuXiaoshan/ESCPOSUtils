package com.cxsplay.epcu.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.cxsplay.epcu.R
import com.cxsplay.epcu.bean.BtDeviceBean

/**
 * Created by CxStation on 2021/11/10 22:41.
 */
class DeviceRvAdapter(
    val dataSet: MutableList<BtDeviceBean> = mutableListOf()
) : RecyclerView.Adapter<DeviceRvAdapter.ViewHolder>() {

    private var mItemClick: ((v: View, p: Int) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_device, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = dataSet[position]
        holder.tvName.text = item.name
        holder.tvAddress.text = item.address
        holder.itemView.setOnClickListener {
            mItemClick?.invoke(it, position)
        }
    }

    override fun getItemCount() = dataSet.size

    fun addData(data: BtDeviceBean) {
        if (!dataSet.any { it.address == data.address }) {
            dataSet.add(data)
            notifyItemInserted(dataSet.size - 1)
        }
    }

    fun itemClick(itemClick: ((v: View, p: Int) -> Unit)) {
        this.mItemClick = itemClick
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tv_name)
        val tvAddress: TextView = view.findViewById(R.id.tv_address)
    }
}