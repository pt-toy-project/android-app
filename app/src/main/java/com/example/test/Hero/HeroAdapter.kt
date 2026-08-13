package com.example.test.Hero

import android.app.Dialog
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import android.content.Intent
import com.example.test.R
import com.example.test.RoutineActivity


class HeroAdapter(
    private val heroList: List<Hero>
) : RecyclerView.Adapter<HeroAdapter.HeroViewHolder>() {

    class HeroViewHolder(view: View) : RecyclerView.ViewHolder(view) {

        val image: ImageView = view.findViewById(R.id.ivHero)
        val name: TextView = view.findViewById(R.id.tvHeroName)
        val field: TextView = view.findViewById(R.id.tvHeroField)
        val info: TextView = view.findViewById(R.id.tvHeroInfo)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): HeroViewHolder {

        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_hero_card, parent, false)

        return HeroViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: HeroViewHolder,
        position: Int
    ) {

        val hero = heroList[position]

        holder.image.setImageResource(hero.imageResId)
        holder.name.text = hero.name
        holder.field.text = hero.field
        holder.info.text = hero.info

        // 짧게 클릭
        holder.itemView.setOnClickListener {

            val intent = Intent(
                holder.itemView.context,

                // TODO(주혁): RoutineActivity::class.java를 RoutineSelected로 변경해야 함
                RoutineActivity::class.java
            )

            intent.putExtra("heroName", hero.name)

            holder.itemView.context.startActivity(intent)
        }

        // 길게 클릭
        holder.itemView.setOnLongClickListener {

            showHeroDialog(holder.itemView, hero)

            true
        }
    }

    private fun showHeroDialog(
        itemView: View,
        hero: Hero
    ) {

        val dialog = Dialog(itemView.context)

        val dialogView = LayoutInflater.from(itemView.context)
            .inflate(R.layout.dialog_hero_detail, null)

        // 팝업 안의 카드
        val image = dialogView.findViewById<ImageView>(R.id.ivHero)
        val name = dialogView.findViewById<TextView>(R.id.tvHeroName)
        val field = dialogView.findViewById<TextView>(R.id.tvHeroField)
        val info = dialogView.findViewById<TextView>(R.id.tvHeroInfo)

        image.setImageResource(hero.imageResId)
        name.text = hero.name
        field.text = hero.field
        info.text = hero.info

        dialog.setContentView(dialogView)

        dialog.window?.setBackgroundDrawableResource(
            android.R.color.transparent
        )

        dialog.show()

        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    override fun getItemCount(): Int {
        return heroList.size
    }
}