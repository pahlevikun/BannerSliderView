package id.pahlevikun.highlightbannerslider.adapter

import android.graphics.Color
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.support.v7.widget.RecyclerView
import android.view.ViewGroup
import android.widget.ImageView
import id.pahlevikun.highlightbannerslider.base.adapter.BaseAdapter
import id.pahlevikun.highlightbannerslider.utils.BannerHelper.dp2px

class IndicatorSliderAdapter(private var bannerSize: Int,
                             private var indicatorMargin: Int) : BaseAdapter<RecyclerView.ViewHolder, Int>() {

    private var currentPosition = 0
    private var mSelectedDrawable: Drawable? = null
    private var mUnselectedDrawable: Drawable? = null

    init {
        if (mSelectedDrawable == null) {
            val selectedGradientDrawable = GradientDrawable()
            selectedGradientDrawable.shape = GradientDrawable.OVAL
            selectedGradientDrawable.setColor(Color.RED)
            selectedGradientDrawable.setSize(dp2px(5), dp2px(5))
            selectedGradientDrawable.cornerRadius = (dp2px(5) / 2).toFloat()
            mSelectedDrawable = LayerDrawable(arrayOf<Drawable>(selectedGradientDrawable))
        }
        if (mUnselectedDrawable == null) {
            val unSelectedGradientDrawable = GradientDrawable()
            unSelectedGradientDrawable.shape = GradientDrawable.OVAL
            unSelectedGradientDrawable.setColor(Color.GRAY)
            unSelectedGradientDrawable.setSize(dp2px(5), dp2px(5))
            unSelectedGradientDrawable.cornerRadius = (dp2px(5) / 2).toFloat()
            mUnselectedDrawable = LayerDrawable(arrayOf<Drawable>(unSelectedGradientDrawable))
        }
    }

    fun setItemSize(bannerSize: Int) {
        this.bannerSize = bannerSize
        notifyDataSetChanged()
    }

    fun setPosition(currentPosition: Int) {
        this.currentPosition = currentPosition
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {

        val bannerPoint = ImageView(parent.context)
        val lp = RecyclerView.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT)
        lp.setMargins(indicatorMargin, indicatorMargin, indicatorMargin, indicatorMargin)
        bannerPoint.layoutParams = lp
        return object : RecyclerView.ViewHolder(bannerPoint) {

        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val bannerPoint = holder.itemView as ImageView
        bannerPoint.setImageDrawable(if (currentPosition == position) mSelectedDrawable else mUnselectedDrawable)

    }

    override fun getItemCount(): Int {
        return bannerSize
    }

    override fun addAllData(data: MutableList<Int>) {
    }

    override fun addData(data: Int) {
    }

    override fun getDataAt(position: Int): Int {
        return 0
    }

    override fun getAllData(): MutableList<Int> {
        return mutableListOf()
    }

    override fun setData(data: MutableList<Int>) {
    }

    override fun updateData(position: Int, data: Int) {
    }
}