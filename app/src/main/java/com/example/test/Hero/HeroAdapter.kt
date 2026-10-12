package com.example.test.Hero

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.test.R

/** Rendering only: navigation and dialogs belong to the Activity lifecycle. */
class HeroAdapter(
    private val onHeroClick: (Hero) -> Unit,
    private val onHeroLongClick: (Hero) -> Unit
) : ListAdapter<Hero, HeroAdapter.HeroViewHolder>(HeroDiff) {

    class HeroViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val image: ImageView = view.findViewById(R.id.ivHero)
        val name: TextView = view.findViewById(R.id.tvHeroName)
        val field: TextView = view.findViewById(R.id.tvHeroField)
        val info: TextView = view.findViewById(R.id.tvHeroInfo)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HeroViewHolder =
        HeroViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_hero_card, parent, false))

    override fun onBindViewHolder(holder: HeroViewHolder, position: Int) {
        val hero = getItem(position)
        holder.image.setImageResource(hero.imageResId)
        holder.name.text = hero.name
        holder.field.text = hero.field
        holder.info.text = hero.info
        holder.itemView.isFocusable = true
        holder.itemView.contentDescription = holder.itemView.context.getString(
            R.string.hero_card_action, hero.name, hero.field, hero.info
        )
        holder.itemView.setOnClickListener { onHeroClick(hero) }
        holder.itemView.setOnLongClickListener {
            onHeroLongClick(hero)
            true
        }
    }

    private object HeroDiff : DiffUtil.ItemCallback<Hero>() {
        override fun areItemsTheSame(oldItem: Hero, newItem: Hero): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Hero, newItem: Hero): Boolean = oldItem == newItem
    }
}
