package com.shorman.smartelevator.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.shorman.smartelevator.R
import com.shorman.smartelevator.models.ElevatorPanel
import kotlinx.android.synthetic.main.panel_item.view.*

//RecyclerView adapter with diff util
class PanelAdapter(private val onClickListener:(panelNumber:Int) -> Unit): RecyclerView.Adapter<PanelAdapter.PanelViewHolder>() {

    private val diffUtil= object : DiffUtil.ItemCallback<ElevatorPanel>(){
        override fun areItemsTheSame(oldItem: ElevatorPanel, newItem: ElevatorPanel): Boolean {
            return oldItem.hashCode() == newItem.hashCode()
        }

        override fun areContentsTheSame(oldItem: ElevatorPanel, newItem: ElevatorPanel): Boolean {
            return oldItem == newItem
        }

    }

    val differ = AsyncListDiffer(this,diffUtil)

    class PanelViewHolder(itemView:View) :RecyclerView.ViewHolder(itemView)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PanelViewHolder {
        return PanelViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.panel_item,parent,false))
    }

    //index to track user clicks
    private var index = -1

    override fun onBindViewHolder(holder: PanelViewHolder, position: Int) {
        val panelItem = differ.currentList[position]
        holder.itemView.apply {
            tvFloorNumber.text = panelItem.number.toString()

            //check if the user in this floor
            if(panelItem.selected){
                tvYouAreHere.visibility = View.VISIBLE
                setOnClickListener {
                    Toast.makeText(context,"You are here!",Toast.LENGTH_LONG).show()
                }
            }
            else{
                tvYouAreHere.visibility = View.INVISIBLE
                setOnClickListener {
                    index = position
                    onClickListener(panelItem.number)
                    notifyDataSetChanged()
                }
                //show a boarder if the user select specific floor
                if(index == position){
                    panelBackground.visibility = View.VISIBLE
                }
                else{
                    panelBackground.visibility = View.INVISIBLE
                }
            }

        }

    }

    override fun getItemCount(): Int {
        return differ.currentList.size
    }
}