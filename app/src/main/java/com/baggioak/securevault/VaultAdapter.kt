package com.baggioak.securevault

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class VaultAdapter(
    private var fullList: List<DecryptedVaultItem>,
    private val onItemClick: (DecryptedVaultItem) -> Unit

) : RecyclerView.Adapter<VaultAdapter.VaultViewHolder>() {


    var onDataChanged: ((Int) -> Unit)? = null
    private var filteredList: List<DecryptedVaultItem> = fullList

    class VaultViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvPlatform: TextView = view.findViewById(R.id.tvItemPlatform)
        val tvUsername: TextView = view.findViewById(R.id.tvItemUsername)
        val tvItemTag: TextView = itemView.findViewById(R.id.tvItemTag)

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VaultViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_vault, parent, false)
        return VaultViewHolder(view)
    }

    override fun onBindViewHolder(holder: VaultViewHolder, position: Int) {
        val item = filteredList[position]

        holder.tvPlatform.text = if (item.platform.isNullOrBlank()) "Senza Titolo" else item.platform
        holder.tvUsername.text = item.username

        holder.itemView.setOnClickListener {
            onItemClick(item)
        }
        if (!item.tags.isNullOrBlank()) {
            holder.tvItemTag.visibility = View.VISIBLE
            holder.tvItemTag.text = item.tags
        } else {
            // Se non c'è nessun tag, scompare e non occupa spazio!
            holder.tvItemTag.visibility = View.GONE
        }
    }

    override fun getItemCount() = filteredList.size

    fun filter(query: String) {
        filteredList = if (query.isEmpty()) {
            fullList
        } else {
            fullList.filter {
                // Se platform non è nullo cerca la query, altrimenti restituisci false
                (it.platform?.contains(query, ignoreCase = true) ?: false) ||
                // Stessa cosa per lo username
                (it.username?.contains(query, ignoreCase = true) ?: false) ||
                // Stessa cosa per i tags
                (it.tags?.contains(query, ignoreCase = true) ?: false)

            }
        }
        notifyDataSetChanged()
        onDataChanged?.invoke(filteredList.size)
    }

    fun updateData(newList: List<DecryptedVaultItem>) {
        fullList = newList
        filteredList = newList
        notifyDataSetChanged()
        onDataChanged?.invoke(filteredList.size)
    }
}