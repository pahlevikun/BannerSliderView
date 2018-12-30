package id.pahlevikun.highlightbannerslider.extension

import android.view.View

fun View.makeGone() {
    if (this != null) {
        this.visibility = View.GONE
    }
}

fun View.makeVisible() {
    if (this != null) {
        this.visibility = View.VISIBLE
    }
}

fun View.makeInvisible() {
    if (this != null) {
        this.visibility = View.INVISIBLE
    }
}