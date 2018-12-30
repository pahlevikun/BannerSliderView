package id.pahlevikun.highlightbannerslider.utils

import android.content.res.Resources
import android.support.v7.widget.LinearSnapHelper
import android.support.v7.widget.RecyclerView
import android.util.TypedValue

object BannerHelper {

    class SnapOneByOnePagerHelper : LinearSnapHelper() {

        override fun findTargetSnapPosition(layoutManager: RecyclerView.LayoutManager?, velocityX: Int, velocityY: Int): Int {

            if (layoutManager !is RecyclerView.SmoothScroller.ScrollVectorProvider) {
                return RecyclerView.NO_POSITION
            }

            val currentView = findSnapView(layoutManager) ?: return RecyclerView.NO_POSITION
            val currentPosition = layoutManager.getPosition(currentView)

            return if (currentPosition == RecyclerView.NO_POSITION) {
                RecyclerView.NO_POSITION
            } else {
                currentPosition
            }
        }
    }

    fun dp2px(dp: Int): Int {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp.toFloat(),
            Resources.getSystem().displayMetrics).toInt()
    }
}