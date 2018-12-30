package id.pahlevikun.highlightbannerslider.widget

import android.content.Context
import android.os.Handler
import android.support.v4.view.GravityCompat
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.OrientationHelper
import android.support.v7.widget.RecyclerView
import android.util.AttributeSet
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.AbsListView.OnScrollListener.SCROLL_STATE_IDLE
import android.widget.FrameLayout
import com.example.library.R
import id.pahlevikun.highlightbannerslider.adapter.IndicatorSliderAdapter
import id.pahlevikun.highlightbannerslider.utils.BannerHelper.SnapOneByOnePagerHelper
import id.pahlevikun.highlightbannerslider.utils.BannerHelper.dp2px
import id.pahlevikun.highlightbannerslider.utils.BannerLayoutManager

class BannerSliderView @JvmOverloads constructor(context: Context,
                                                 attrs: AttributeSet? = null,
                                                 defStyleAttr: Int = 0) : FrameLayout(context, attrs, defStyleAttr) {

    companion object {
        private const val WHAT_AUTO_PLAY = 1000
    }

    private var indicatorContainer: RecyclerView? = null
    private var indicatorAdapter: IndicatorSliderAdapter? = null
    private var mRecyclerView: RecyclerView? = null
    private var adapter: RecyclerView.Adapter<*>? = null
    private var mLayoutManager: BannerLayoutManager? = null

    private var autoPlayDuration = 0
    private var bannerSize = 1
    private var currentIndex = 0
    private var itemSpace = 0
    private var centerScale = 0f
    private var moveSpeed = 0f
    private var isAutoPlaying = true
    private val isViewTouched = false
    private var showSliderIndicator = false
    private var hasInit = false

    private val bannerScrollListener: RecyclerView.OnScrollListener by lazy {
        object : RecyclerView.OnScrollListener() {

            override fun onScrolled(recyclerView: RecyclerView?, dx: Int, dy: Int) {
                if (dx != 0 || dy != 0) {
                    isPlaying = false
                }
            }

            override fun onScrollStateChanged(recyclerView: RecyclerView?, newState: Int) {
                val first = mLayoutManager?.currentPosition
                if (currentIndex != first) {
                    currentIndex = first ?: 0
                }
                if (newState == SCROLL_STATE_IDLE) {
                    isPlaying = true
                }
                refreshIndicator()
            }
        }
    }

    private val mHandler: Handler by lazy {
        Handler(Handler.Callback { msg ->
            if (msg.what == WHAT_AUTO_PLAY) {
                if (currentIndex == mLayoutManager?.currentPosition) {
                    ++currentIndex
                    mRecyclerView?.smoothScrollToPosition(currentIndex)
                    mHandler.sendEmptyMessageDelayed(WHAT_AUTO_PLAY, autoPlayDuration.toLong())
                    refreshIndicator()
                }
            }
            false
        })
    }

    private var isPlaying = false
        @Synchronized private set(playing) {
            if (isAutoPlaying && hasInit) {
                if (!this.isPlaying && playing) {
                    mHandler.sendEmptyMessageDelayed(WHAT_AUTO_PLAY, autoPlayDuration.toLong())
                    field = true
                } else if (this.isPlaying && !playing) {
                    mHandler.removeMessages(WHAT_AUTO_PLAY)
                    field = false
                }
            }
        }

    init {
        initView(context, attrs)
    }

    private fun initView(context: Context, attrs: AttributeSet?) {
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.BannerSliderView)
        showSliderIndicator = typedArray.getBoolean(R.styleable.BannerSliderView_showSliderIndicator, true)
        autoPlayDuration = typedArray.getInt(R.styleable.BannerSliderView_interval, 4000)
        isAutoPlaying = typedArray.getBoolean(R.styleable.BannerSliderView_autoStartMovement, true)
        itemSpace = typedArray.getInt(R.styleable.BannerSliderView_bannerSpace, 20)
        centerScale = typedArray.getFloat(R.styleable.BannerSliderView_scaleCenterBanner, 1.2f)
        moveSpeed = typedArray.getFloat(R.styleable.BannerSliderView_movementSpeed, 1.0f)
        val orientationFromAttrs = typedArray.getInt(R.styleable.BannerSliderView_orientation, 0)
        val orientation = when (orientationFromAttrs) {
            0 -> OrientationHelper.HORIZONTAL
            1 -> OrientationHelper.VERTICAL
            else -> 0
        }
        typedArray.recycle()

        initBannerSlider(orientation)
        initIndicatorSlider(orientation)
    }

    private fun initBannerSlider(orientation: Int) {
        mRecyclerView = RecyclerView(context)
        val vpLayoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        addView(mRecyclerView, vpLayoutParams)
        mLayoutManager = BannerLayoutManager(orientation, false)
        mLayoutManager?.setItemSpace(itemSpace)
        mLayoutManager?.setCenterScale(centerScale)
        mLayoutManager?.setMoveSpeed(moveSpeed)
        mRecyclerView?.layoutManager = mLayoutManager
        val snapHelper = SnapOneByOnePagerHelper()
        snapHelper.attachToRecyclerView(mRecyclerView)
    }

    private fun initIndicatorSlider(orientation: Int) {
        val indicatorMargin = dp2px(4)
        val marginLeft = dp2px(16)
        val marginRight = dp2px(0)
        val marginBottom = dp2px(11)
        val gravity = GravityCompat.START
        indicatorContainer = RecyclerView(context)
        val indicatorLayoutManager = LinearLayoutManager(context, orientation, false)
        indicatorContainer?.layoutManager = indicatorLayoutManager
        indicatorAdapter = IndicatorSliderAdapter(bannerSize, indicatorMargin)
        indicatorContainer?.adapter = indicatorAdapter
        val params = FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        params.gravity = Gravity.BOTTOM or gravity
        params.setMargins(marginLeft, 0, marginRight, marginBottom)
        addView(indicatorContainer, params)
        if (!showSliderIndicator) {
            indicatorContainer?.visibility = View.GONE
        }
    }

    fun setAutoPlaying(isAutoPlaying: Boolean) {
        this.isAutoPlaying = isAutoPlaying
        isPlaying = this.isAutoPlaying
    }

    fun setShowSliderIndicator(showSliderIndicator: Boolean) {
        this.showSliderIndicator = showSliderIndicator
        indicatorContainer?.visibility = if (showSliderIndicator) View.VISIBLE else View.GONE
    }

    fun setCenterScale(centerScale: Float) {
        this.centerScale = centerScale
        mLayoutManager?.setCenterScale(centerScale)
    }

    fun setMoveSpeed(moveSpeed: Float) {
        this.moveSpeed = moveSpeed
        mLayoutManager?.setMoveSpeed(moveSpeed)
    }

    fun setItemSpace(itemSpace: Int) {
        this.itemSpace = itemSpace
        mLayoutManager?.setItemSpace(itemSpace)
    }

    fun setAutoPlayDuration(autoPlayDuration: Int) {
        this.autoPlayDuration = autoPlayDuration
    }

    fun setOrientation(orientation: Int) {
        mLayoutManager?.orientation = orientation
    }

    fun invalidateBanner() {
        bannerSize = adapter?.itemCount ?: 0
        refreshIndicator()
        refreshDrawableState()
    }

    @Synchronized
    private fun refreshIndicator() {
        if (showSliderIndicator && bannerSize > 1) {
            indicatorAdapter?.setPosition(currentIndex % bannerSize)
            indicatorAdapter?.notifyDataSetChanged()
        }
    }

    fun setAdapter(adapter: RecyclerView.Adapter<*>) {
        this.adapter = adapter
        hasInit = false
        mRecyclerView?.adapter = adapter
        bannerSize = adapter.itemCount
        mLayoutManager?.infinite = bannerSize >= 3
        isPlaying = true
        mRecyclerView?.addOnScrollListener(bannerScrollListener)
        mRecyclerView?.isNestedScrollingEnabled = false
        indicatorAdapter?.setItemSize(bannerSize)
        hasInit = true
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        when (ev.action) {
            MotionEvent.ACTION_DOWN -> isPlaying = false
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> isPlaying = true
        }
        return super.dispatchTouchEvent(ev)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        isPlaying = true
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        isPlaying = false
    }

    override fun onWindowVisibilityChanged(visibility: Int) {
        super.onWindowVisibilityChanged(visibility)
        isPlaying = visibility == View.VISIBLE
    }
}