package id.pahlevikun.highlightbannerslider.adapter

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bumptech.glide.Glide
import com.example.library.R
import id.pahlevikun.highlightbannerslider.base.adapter.BaseAdapter
import id.pahlevikun.highlightbannerslider.base.callback.BannerListener
import kotlinx.android.synthetic.main.layout_banner_slider_view.view.*

open class BannerSliderAdapter(imageUrlList: MutableList<String>,
                          private val cardHeight: Int,
                          private val cardWidth: Int,
                          private val bannerListener: BannerListener<String>) : BaseAdapter<RecyclerView.ViewHolder, String>() {

    init {
        setHasStableIds(true)
    }

    constructor(imageUrlList: MutableList<String>,
                bannerListener: BannerListener<String>) : this(imageUrlList, 0, 0, bannerListener)

    private var data: MutableList<String> = imageUrlList

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val view = layoutInflater.inflate(R.layout.layout_banner_slider_view, parent, false)
        return ItemViewHolder(view)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is ItemViewHolder -> holder
                .bindData(data[position],
                    bannerListener,
                    cardHeight,
                    cardWidth)
        }
    }

    override fun addAllData(data: MutableList<String>) {
        this.data.addAll(data)
        this.notifyDataSetChanged()
    }

    override fun addData(data: String) {
        this.data.add(data)
        this.notifyDataSetChanged()
    }

    override fun getDataAt(position: Int): String {
        return data[position]
    }

    override fun getAllData(): MutableList<String> {
        return data
    }

    override fun setData(data: MutableList<String>) {
        this.data = data
        this.notifyDataSetChanged()
    }

    override fun updateData(position: Int, data: String) {
        this.data[position] = data
        this.notifyDataSetChanged()
    }

    override fun getItemCount(): Int {
        return data.size
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }
}

class ItemViewHolder(viewItem: View) : RecyclerView.ViewHolder(viewItem) {

    fun bindData(data: String,
                 bannerListener: BannerListener<String>,
                 cardHeight: Int,
                 cardWidth: Int) {

        if (cardHeight != 0 && cardWidth != 0) {
            itemView.card_view.layoutParams = ViewGroup.LayoutParams(cardWidth, cardHeight)
        }

        Glide
            .with(itemView.context)
            .load(data)
            .into(itemView.image_view)
        itemView.image_view.setOnClickListener {
            bannerListener.onItemClick(adapterPosition, data)
        }
    }
}