package id.pahlevikun.highlightbannerslider.utils

import android.os.Parcel
import android.os.Parcelable
import android.support.v4.view.ViewCompat
import android.support.v7.widget.OrientationHelper
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.RecyclerView.NO_POSITION
import android.util.SparseArray
import android.view.View
import android.view.ViewGroup
import android.view.animation.Interpolator
import id.pahlevikun.highlightbannerslider.base.callback.OnPageChangeListener
import java.util.*

/**
 * An implementation of [RecyclerView.LayoutManager] which behaves like view pager.
 * Please make sure your child view have the same size.
 */

class BannerLayoutManager(orientation: Int = HORIZONTAL,
                          reverseLayout: Boolean = false) : RecyclerView.LayoutManager() {

    companion object {
        private const val DETERMINE_BY_MAX_AND_MIN = -1
        private const val HORIZONTAL = OrientationHelper.HORIZONTAL
        private const val VERTICAL = OrientationHelper.VERTICAL
        private const val DIRECTION_NO_WHERE = -1
        private const val DIRECTION_FORWARD = 0
        private const val DIRECTION_BACKWARD = 1
        private const val INVALID_SIZE = Integer.MAX_VALUE
    }

    private val positionCache = SparseArray<View>()
    private var decoratedMeasurement = 0
    private var decoratedMeasurementInOther = 0
    private var leftItems = 0
    private var rightItems = 0
    private var itemSpace = 20
    private var centerScale = 1.2f
    private var moveSpeed = 1.0f
    private var nextOffset = 0f
    private var slideInterval = 0f
    private var distancesToBottom = INVALID_SIZE
    private var currentOrientation = 0
    private var currentSpaceMain = 0
    private var currentSpaceInOther = 0
    private var isReverseLayout = false
    private var shouldReverseLayout = false
    private var recycleChildrenOnDetach = false
    private var smoothScrollbarEnabled = true
    private var pendingScrollPosition = NO_POSITION
    private var pendingSavedState: SavedState? = null
    private var currentFocusView: View? = null
    private var onPageChangeListener: OnPageChangeListener? = null
    private var smoothScrollInterpolator: Interpolator? = null
    private var orientationHelper: OrientationHelper? = null


    var infinite = true
        set(enable) {
            assertNotInLayoutOrScroll(null)
            if (enable == infinite) {
                return
            }
            field = enable
            requestLayout()
        }

    private var enableBringCenterToFront: Boolean = false
        set(bringCenterToTop) {
            assertNotInLayoutOrScroll(null)
            if (enableBringCenterToFront == bringCenterToTop) {
                return
            }
            field = bringCenterToTop
            requestLayout()
        }

    private var maxVisibleItemCount = DETERMINE_BY_MAX_AND_MIN
        set(mMaxVisibleItemCount) {
            assertNotInLayoutOrScroll(null)
            if (this.maxVisibleItemCount == mMaxVisibleItemCount) return
            field = mMaxVisibleItemCount
            removeAllViews()
        }


    private val distanceRatio: Float
        get() = if (moveSpeed == 0f) java.lang.Float.MAX_VALUE else 1 / moveSpeed

    /**
     * Returns the current orientation of the layout.
     *
     * @return Current orientation,  either [.HORIZONTAL] or [.VERTICAL]
     * @see .setOrientation
     */
    /**
     * Sets the orientation of the layout. [BannerLayoutManager]
     * will do its best to keep scroll position.
     */
    var orientation
        get() = currentOrientation
        set(orientation) {
            if (orientation != HORIZONTAL && orientation != VERTICAL) {
                throw IllegalArgumentException("invalid orientation:$orientation")
            }
            assertNotInLayoutOrScroll(null)
            if (orientation == currentOrientation) {
                return
            }
            currentOrientation = orientation
            orientationHelper = null
            distancesToBottom = INVALID_SIZE
            removeAllViews()
        }

    /**
     * Returns if views are laid out from the opposite direction of the layout.
     *
     * @return If layout is reversed or not.
     * @see .setReverseLayout
     */
    /**
     * Used to reverse item traversal and layout order.
     * This behaves similar to the layout change for RTL views. When set to true, first item is
     * laid out at the end of the UI, second item is laid out before it etc.
     *
     *
     * For horizontal layouts, it depends on the layout direction.
     * When set to true, If [RecyclerView] is LTR, than it will
     * layout from RTL, if [RecyclerView]} is RTL, it will layout
     * from LTR.
     */
    var reverseLayout: Boolean
        get() = isReverseLayout
        set(reverseLayout) {
            assertNotInLayoutOrScroll(null)
            if (reverseLayout == isReverseLayout) {
                return
            }
            isReverseLayout = reverseLayout
            removeAllViews()
        }

    private val totalSpaceInOther: Int
        get() = if (currentOrientation == HORIZONTAL) {
            (height - paddingTop
                - paddingBottom)
        } else {
            (width - paddingLeft
                - paddingRight)
        }

    private val maxOffset: Float
        get() = if (!shouldReverseLayout) (itemCount - 1) * slideInterval else 0f

    private val minOffset: Float
        get() = if (!shouldReverseLayout) 0f else -(itemCount - 1) * slideInterval


    val currentPosition: Int
        get() {
            if (itemCount == 0) return 0

            var position = currentPositionOffset
            if (!infinite) return Math.abs(position)

            position = if (!shouldReverseLayout)
                if (position >= 0)
                    position % itemCount
                else
                    itemCount + position % itemCount
            else
                if (position > 0)
                    itemCount - position % itemCount
                else
                    -position % itemCount
            return if (position == itemCount) 0 else position
        }

    private val currentPositionOffset: Int
        get() = Math.round(nextOffset / slideInterval)

    private val offsetOfRightAdapterPosition: Float
        get() = if (shouldReverseLayout)
            if (infinite)
                if (nextOffset <= 0)
                    nextOffset % (slideInterval * itemCount)
                else
                    itemCount * -slideInterval + nextOffset % (slideInterval * itemCount)
            else
                nextOffset
        else
            if (infinite)
                if (nextOffset >= 0)
                    nextOffset % (slideInterval * itemCount)
                else
                    itemCount * slideInterval + nextOffset % (slideInterval * itemCount)
            else
                nextOffset

    val offsetToCenter: Int
        get() = if (infinite) ((currentPositionOffset * slideInterval - nextOffset) * distanceRatio).toInt() else ((currentPosition * if (!shouldReverseLayout) slideInterval else -slideInterval - nextOffset) * distanceRatio).toInt()

    var distanceToBottom: Int
        get() = if (distancesToBottom == INVALID_SIZE)
            (totalSpaceInOther - decoratedMeasurementInOther) / 2
        else
            distancesToBottom
        set(mDistanceToBottom) {
            assertNotInLayoutOrScroll(null)
            if (this.distancesToBottom == mDistanceToBottom) return
            this.distancesToBottom = mDistanceToBottom
            removeAllViews()
        }

    init {
        enableBringCenterToFront = true
        maxVisibleItemCount = 3
        this@BannerLayoutManager.orientation = orientation
        this@BannerLayoutManager.reverseLayout = reverseLayout
        isAutoMeasureEnabled = true
        isItemPrefetchEnabled = false
    }

    fun setItemSpace(itemSpace: Int) {
        this.itemSpace = itemSpace
    }

    fun setCenterScale(centerScale: Float) {
        this.centerScale = centerScale
    }

    fun setMoveSpeed(moveSpeed: Float) {
        assertNotInLayoutOrScroll(null)
        if (this.moveSpeed == moveSpeed) return
        this.moveSpeed = moveSpeed
    }

    private fun setInterval(): Float {
        return decoratedMeasurement * ((centerScale - 1) / 2 + 1) + itemSpace
    }

    private fun setItemViewProperty(itemView: View, targetOffset: Float) {
        val scale = calculateScale(targetOffset + currentSpaceMain)
        itemView.scaleX = scale
        itemView.scaleY = scale
    }

    /**
     * @param x start positon of the view you want scale
     * @return the scale rate of current scroll nextOffset
     */
    private fun calculateScale(x: Float): Float {
        val deltaX = Math.abs(x - (orientationHelper!!.totalSpace - decoratedMeasurement) / 2f)
        var diff = 0f
        if (decoratedMeasurement - deltaX > 0) diff = decoratedMeasurement - deltaX
        return (centerScale - 1f) / decoratedMeasurement * diff + 1
    }

    /**
     * cause elevation is not support below api 21,
     * so you can set your elevation here for supporting it below api 21
     * or you can just setElevation in [.setItemViewProperty]
     */
    private fun setViewElevation(itemView: View, targetOffset: Float): Float {
        return 0f
    }

    override fun generateDefaultLayoutParams(): RecyclerView.LayoutParams {
        return RecyclerView.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    override fun onDetachedFromWindow(view: RecyclerView, recycler: RecyclerView.Recycler?) {
        super.onDetachedFromWindow(view, recycler)
        if (recycleChildrenOnDetach) {
            removeAndRecycleAllViews(recycler)
            recycler!!.clear()
        }
    }

    override fun onSaveInstanceState(): Parcelable {
        if (pendingSavedState != null) {
            return SavedState(pendingSavedState!!)
        }
        val savedState = SavedState()
        savedState.position = pendingScrollPosition
        savedState.offset = nextOffset
        savedState.isReverseLayout = shouldReverseLayout
        return savedState
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        if (state is SavedState) {
            pendingSavedState = SavedState((state as SavedState?)!!)
            requestLayout()
        }
    }

    override fun canScrollHorizontally(): Boolean {
        return currentOrientation == HORIZONTAL
    }

    override fun canScrollVertically(): Boolean {
        return currentOrientation == VERTICAL
    }

    override fun smoothScrollToPosition(recyclerView: RecyclerView?, state: RecyclerView.State?, position: Int) {
        val offsetPosition = getOffsetToPosition(position)
        if (currentOrientation == VERTICAL) {
            recyclerView!!.smoothScrollBy(0, offsetPosition, smoothScrollInterpolator)
        } else {
            recyclerView!!.smoothScrollBy(offsetPosition, 0, smoothScrollInterpolator)
        }
    }

    override fun scrollToPosition(position: Int) {
        if (!infinite && (position < 0 || position >= itemCount)) return
        pendingScrollPosition = position
        nextOffset = if (shouldReverseLayout) position * -slideInterval else position * slideInterval
        requestLayout()
    }

    override fun onLayoutChildren(recycler: RecyclerView.Recycler?, state: RecyclerView.State?) {
        if (state!!.itemCount == 0) {
            removeAndRecycleAllViews(recycler)
            nextOffset = 0f
            return
        }

        ensureLayoutState()
        resolveShouldLayoutReverse()

        //make sure properties are correct while measure more than once
        val scrap = recycler!!.getViewForPosition(0)
        measureChildWithMargins(scrap, 0, 0)
        decoratedMeasurement = orientationHelper!!.getDecoratedMeasurement(scrap)
        decoratedMeasurementInOther = orientationHelper!!.getDecoratedMeasurementInOther(scrap)
        currentSpaceMain = (orientationHelper!!.totalSpace - decoratedMeasurement) / 2
        currentSpaceInOther = if (distancesToBottom == INVALID_SIZE) {
            (totalSpaceInOther - decoratedMeasurementInOther) / 2
        } else {
            totalSpaceInOther - decoratedMeasurementInOther - distancesToBottom
        }

        slideInterval = setInterval()
        leftItems = Math.abs(minRemoveOffset() / slideInterval).toInt() + 1
        rightItems = Math.abs(maxRemoveOffset() / slideInterval).toInt() + 1

        if (pendingSavedState != null) {
            shouldReverseLayout = pendingSavedState!!.isReverseLayout
            pendingScrollPosition = pendingSavedState!!.position
            nextOffset = pendingSavedState!!.offset
        }

        if (pendingScrollPosition != NO_POSITION) {
            nextOffset = if (shouldReverseLayout) {
                pendingScrollPosition * -slideInterval
            } else {
                pendingScrollPosition * slideInterval
            }
        }

        detachAndScrapAttachedViews(recycler)
        layoutItems(recycler)
    }

    override fun onLayoutCompleted(state: RecyclerView.State?) {
        super.onLayoutCompleted(state)
        pendingSavedState = null
        pendingScrollPosition = NO_POSITION
    }

    override fun onAddFocusables(recyclerView: RecyclerView?, views: ArrayList<View>?, direction: Int, focusableMode: Int): Boolean {
        val currentPosition = currentPosition
        val currentView = findViewByPosition(currentPosition) ?: return true
        if (recyclerView!!.hasFocus()) {
            val movement = getMovement(direction)
            if (movement != DIRECTION_NO_WHERE) {
                val targetPosition = if (movement == DIRECTION_BACKWARD)
                    currentPosition - 1
                else
                    currentPosition + 1
                recyclerView.smoothScrollToPosition(targetPosition)
            }
        } else {
            currentView.addFocusables(views, direction, focusableMode)
        }
        return true
    }

    override fun onFocusSearchFailed(focused: View?, focusDirection: Int, recycler: RecyclerView.Recycler?, state: RecyclerView.State?): View? {
        return null
    }

    override fun onAdapterChanged(oldAdapter: RecyclerView.Adapter<*>?, newAdapter: RecyclerView.Adapter<*>?) {
        removeAllViews()
        nextOffset = 0f
    }

    override fun computeHorizontalScrollOffset(state: RecyclerView.State?): Int {
        return computeScrollOffset()
    }

    override fun computeVerticalScrollOffset(state: RecyclerView.State?): Int {
        return computeScrollOffset()
    }

    override fun computeHorizontalScrollExtent(state: RecyclerView.State?): Int {
        return computeScrollExtent()
    }

    override fun computeVerticalScrollExtent(state: RecyclerView.State?): Int {
        return computeScrollExtent()
    }

    override fun computeHorizontalScrollRange(state: RecyclerView.State?): Int {
        return computeScrollRange()
    }

    override fun computeVerticalScrollRange(state: RecyclerView.State?): Int {
        return computeScrollRange()
    }

    override fun scrollHorizontallyBy(dx: Int, recycler: RecyclerView.Recycler?, state: RecyclerView.State?): Int {
        return if (currentOrientation == VERTICAL) 0 else scrollBy(dx, recycler, state)
    }

    override fun scrollVerticallyBy(dy: Int, recycler: RecyclerView.Recycler?, state: RecyclerView.State?): Int {
        return if (currentOrientation == HORIZONTAL) 0 else scrollBy(dy, recycler, state)
    }

    override fun findViewByPosition(position: Int): View? {
        val itemCount = itemCount
        if (itemCount == 0) return null
        for (i in 0 until positionCache.size()) {
            val key = positionCache.keyAt(i)
            if (key >= 0) {
                if (position == key % itemCount) return positionCache.valueAt(i)
            } else {
                var delta = key % itemCount
                if (delta == 0) delta = -itemCount
                if (itemCount + delta == position) return positionCache.valueAt(i)
            }
        }
        return null
    }

    private fun setSmoothScrollInterpolator(smoothScrollInterpolator: Interpolator) {
        this.smoothScrollInterpolator = smoothScrollInterpolator
    }

    private fun resolveShouldLayoutReverse() {
        if (currentOrientation == HORIZONTAL && layoutDirection == ViewCompat.LAYOUT_DIRECTION_RTL) {
            isReverseLayout = !isReverseLayout
        }
    }

    private fun getMovement(direction: Int): Int {
        return if (currentOrientation == VERTICAL) {
            if (direction == View.FOCUS_UP) {
                if (shouldReverseLayout) DIRECTION_FORWARD else DIRECTION_BACKWARD
            } else if (direction == View.FOCUS_DOWN) {
                if (shouldReverseLayout) DIRECTION_BACKWARD else DIRECTION_FORWARD
            } else {
                DIRECTION_NO_WHERE
            }
        } else {
            if (direction == View.FOCUS_LEFT) {
                if (shouldReverseLayout) DIRECTION_FORWARD else DIRECTION_BACKWARD
            } else if (direction == View.FOCUS_RIGHT) {
                if (shouldReverseLayout) DIRECTION_BACKWARD else DIRECTION_FORWARD
            } else {
                DIRECTION_NO_WHERE
            }
        }
    }

    private fun ensureLayoutState() {
        if (orientationHelper == null) {
            orientationHelper = OrientationHelper.createOrientationHelper(this, currentOrientation)
        }
    }

    private fun getProperty(position: Int): Float {
        return if (shouldReverseLayout) position * -slideInterval else position * slideInterval
    }

    private fun computeScrollOffset(): Int {
        if (childCount == 0) {
            return 0
        }
        if (!smoothScrollbarEnabled) {
            return if (!shouldReverseLayout)
                currentPosition
            else
                itemCount - currentPosition - 1
        }
        val realOffset = offsetOfRightAdapterPosition
        return if (!shouldReverseLayout) realOffset.toInt() else ((itemCount - 1) * slideInterval + realOffset).toInt()
    }

    private fun computeScrollExtent(): Int {
        if (childCount == 0) {
            return 0
        }
        return if (!smoothScrollbarEnabled) 1 else slideInterval.toInt()
    }

    private fun computeScrollRange(): Int {
        if (childCount == 0) {
            return 0
        }
        return if (!smoothScrollbarEnabled) itemCount else (itemCount * slideInterval).toInt()
    }

    private fun scrollBy(dy: Int, recycler: RecyclerView.Recycler?, state: RecyclerView.State?): Int {
        if (childCount == 0 || dy == 0) {
            return 0
        }
        ensureLayoutState()
        var willScroll = dy

        var realDx = dy / distanceRatio
        if (Math.abs(realDx) < 0.00000001f) {
            return 0
        }
        val targetOffset = nextOffset + realDx
        if (!infinite && targetOffset < minOffset) {
            willScroll -= ((targetOffset - minOffset) * distanceRatio).toInt()
        } else if (!infinite && targetOffset > maxOffset) {
            willScroll = ((maxOffset - nextOffset) * distanceRatio).toInt()
        }
        realDx = willScroll / distanceRatio
        nextOffset += realDx
        layoutItems(recycler)
        return willScroll
    }

    private fun layoutItems(recycler: RecyclerView.Recycler?) {
        detachAndScrapAttachedViews(recycler)
        positionCache.clear()

        val itemCount = itemCount
        if (itemCount == 0) return

        // make sure that current position start from 0 to 1
        val currentPos = if (shouldReverseLayout) -currentPositionOffset else currentPositionOffset
        var start = currentPos - leftItems
        var end = currentPos + rightItems

        // handle max visible count
        if (useMaxVisibleCount()) {
            val isEven = maxVisibleItemCount % 2 == 0
            if (isEven) {
                val offset = maxVisibleItemCount / 2
                start = currentPos - offset + 1
                end = currentPos + offset + 1
            } else {
                val offset = (maxVisibleItemCount - 1) / 2
                start = currentPos - offset
                end = currentPos + offset + 1
            }
        }

        if (!infinite) {
            if (start < 0) {
                start = 0
                if (useMaxVisibleCount()) end = maxVisibleItemCount
            }
            if (end > itemCount) end = itemCount
        }
        var lastOrderWeight = java.lang.Float.MIN_VALUE
        for (position in start until end) {
            if (useMaxVisibleCount() || !removeCondition(getProperty(position) - nextOffset)) {
                var adapterPosition = position
                if (position >= itemCount) {
                    adapterPosition %= itemCount
                } else if (position < 0) {
                    var delta = -adapterPosition % itemCount
                    if (delta == 0) delta = itemCount
                    adapterPosition = itemCount - delta
                }
                val scrap = recycler!!.getViewForPosition(adapterPosition)
                measureChildWithMargins(scrap, 0, 0)
                resetViewProperty(scrap)
                val targetOffset = getProperty(position) - nextOffset
                layoutScrap(scrap, targetOffset)
                val orderWeight = if (enableBringCenterToFront) {
                    setViewElevation(scrap, targetOffset)
                } else {
                    adapterPosition.toFloat()
                }
                if (orderWeight > lastOrderWeight) {
                    addView(scrap)
                } else {
                    addView(scrap, 0)
                }
                if (position == currentPos) currentFocusView = scrap
                lastOrderWeight = orderWeight
                positionCache.put(position, scrap)
            }
        }
        currentFocusView?.requestFocus()
    }

    private fun useMaxVisibleCount(): Boolean {
        return maxVisibleItemCount != DETERMINE_BY_MAX_AND_MIN
    }

    private fun removeCondition(targetOffset: Float): Boolean {
        return targetOffset > maxRemoveOffset() || targetOffset < minRemoveOffset()
    }

    private fun resetViewProperty(view: View) {
        view.rotation = 0f
        view.rotationY = 0f
        view.rotationX = 0f
        view.scaleX = 1f
        view.scaleY = 1f
        view.alpha = 1f
    }

    private fun layoutScrap(scrap: View, targetOffset: Float) {

        val left = calItemLeft(scrap, targetOffset)
        val top = calItemTop(scrap, targetOffset)
        if (currentOrientation == VERTICAL) {
            layoutDecorated(scrap, currentSpaceInOther + left, currentSpaceMain + top,
                currentSpaceInOther + left + decoratedMeasurementInOther, currentSpaceMain + top + decoratedMeasurement)
        } else {
            layoutDecorated(scrap, currentSpaceMain + left, currentSpaceInOther + top,
                currentSpaceMain + left + decoratedMeasurement, currentSpaceInOther + top + decoratedMeasurementInOther)
        }
        setItemViewProperty(scrap, targetOffset)
    }

    private fun calItemLeft(itemView: View, targetOffset: Float): Int {
        return if (currentOrientation == VERTICAL) 0 else targetOffset.toInt()
    }

    private fun calItemTop(itemView: View, targetOffset: Float): Int {
        return if (currentOrientation == VERTICAL) targetOffset.toInt() else 0
    }

    private fun maxRemoveOffset(): Float {
        return (orientationHelper!!.totalSpace - currentSpaceMain).toFloat()
    }

    private fun minRemoveOffset(): Float {
        return (-decoratedMeasurement - orientationHelper!!.startAfterPadding - currentSpaceMain).toFloat()
    }

    private fun getOffsetToPosition(position: Int): Int {
        return if (infinite) (((currentPositionOffset + if (!shouldReverseLayout) position - currentPosition else currentPosition - position) * slideInterval - nextOffset) * distanceRatio).toInt() else ((position * if (!shouldReverseLayout) slideInterval else -slideInterval - nextOffset) * distanceRatio).toInt()
    }

    private fun setOnPageChangeListener(onPageChangeListener: OnPageChangeListener) {
        this.onPageChangeListener = onPageChangeListener
    }

    private class SavedState : Parcelable {
        internal var position: Int = 0
        internal var offset: Float = 0.toFloat()
        internal var isReverseLayout: Boolean = false

        internal constructor(`in`: Parcel) {
            position = `in`.readInt()
            offset = `in`.readFloat()
            isReverseLayout = `in`.readInt() == 1
        }

        constructor(other: SavedState) {
            position = other.position
            offset = other.offset
            isReverseLayout = other.isReverseLayout
        }

        constructor()

        override fun describeContents(): Int {
            return 0
        }

        override fun writeToParcel(dest: Parcel, flags: Int) {
            dest.writeInt(position)
            dest.writeFloat(offset)
            dest.writeInt(if (isReverseLayout) 1 else 0)
        }

        companion object CREATOR : Parcelable.Creator<SavedState> {
            override fun createFromParcel(parcel: Parcel): SavedState {
                return SavedState(parcel)
            }

            override fun newArray(size: Int): Array<SavedState?> {
                return arrayOfNulls(size)
            }
        }
    }

}