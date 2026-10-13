package com.example.test.Routine

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView

/** The header scrolls with the timeline; rows still recycle at maximum font size. */
class TimelineHeaderAdapter(private val header: View) : RecyclerView.Adapter<TimelineHeaderAdapter.Holder>() {
    class Holder(view: View) : RecyclerView.ViewHolder(view)
    override fun getItemCount() = 1
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = Holder(header)
    override fun onBindViewHolder(holder: Holder, position: Int) = Unit
}
