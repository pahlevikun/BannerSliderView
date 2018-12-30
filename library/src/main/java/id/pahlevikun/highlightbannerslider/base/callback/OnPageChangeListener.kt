package id.pahlevikun.highlightbannerslider.base.callback

interface OnPageChangeListener {
    fun onPageSelected(position: Int)

    fun onPageScrollStateChanged(state: Int)
}