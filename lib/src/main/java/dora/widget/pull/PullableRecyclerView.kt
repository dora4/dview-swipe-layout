package dora.widget.pull

import android.content.Context
import android.util.AttributeSet
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import dora.widget.swipelayout.R

class PullableRecyclerView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null,
                                                     defStyle: Int = 0)
                                                : RecyclerView(context, attrs, defStyle), Pullable {

    private var canPullDown = true
    private var canPullUp = true

    init {
        val a = context.obtainStyledAttributes(attrs, R.styleable.PullableRecyclerView, defStyle, 0)
        canPullDown = a.getBoolean(R.styleable.PullableRecyclerView_dview_canPullDown, canPullDown)
        canPullUp = a.getBoolean(R.styleable.PullableRecyclerView_dview_canPullUp, canPullUp)
        a.recycle()
    }

    fun setCanPullDown(canPullDown: Boolean) {
        this.canPullDown = canPullDown
    }

    fun setCanPullUp(canPullUp: Boolean) {
        this.canPullUp = canPullUp
    }

    override fun canPullDown(): Boolean {
        return if (canPullDown) {
            val layoutManager = layoutManager as LinearLayoutManager?
            val adapter = adapter
            if (adapter != null) {
                return if (adapter.itemCount == 0) {
                    false
                } else layoutManager!!.findFirstVisibleItemPosition() == 0
                    && getChildAt(0).top >= 0
            }
            false
        } else {
            false
        }
    }

    override fun canPullUp(): Boolean {
        if (canPullUp) {
            val layoutManager = layoutManager as LinearLayoutManager
            if (adapter != null && adapter?.itemCount!! == 0) {
                return false
            } else if (layoutManager.findLastVisibleItemPosition() == ((adapter as Adapter).itemCount - 1)) {
                // 滑到底部了
                if (getChildAt(layoutManager.findLastVisibleItemPosition() - layoutManager.findFirstVisibleItemPosition()) != null
                        && getChildAt(
                        layoutManager.findLastVisibleItemPosition()
                                - layoutManager.findFirstVisibleItemPosition()).bottom <= measuredHeight
                ) {
                    return true
                }
            }
            return false
        } else {
            return false
        }
    }
}