package com.azurlize.team.core

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.azurlize.team.R

class CountryAdapter(
    private var countryList: List<Country>,
    private val onItemClick: (Country) -> Unit
) : RecyclerView.Adapter<CountryAdapter.CountryViewHolder>() {

    class CountryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvFlag: TextView = itemView.findViewById(R.id.tv_flag)
        val tvName: TextView = itemView.findViewById(R.id.tv_name)
        val tvDial: TextView = itemView.findViewById(R.id.tv_dial)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CountryViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_country, parent, false)
        return CountryViewHolder(view)
    }

    override fun onBindViewHolder(holder: CountryViewHolder, position: Int) {
        val country = countryList[position]
        holder.tvFlag.text = country.flagEmoji
        holder.tvName.text = country.name
        holder.tvDial.text = country.dialCode
        
        holder.itemView.setOnClickListener {
            onItemClick(country)
        }
    }

    override fun getItemCount(): Int = countryList.size

    fun updateCountries(newCountries: List<Country>) {
        this.countryList = newCountries
        notifyDataSetChanged()
    }
}
