package id.pahlevikun.highlightbannerslider.base.callback

interface BannerListener<T> {
    fun onItemClick(position: Int, data: T)
}