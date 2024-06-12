package dora.widget.pull

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Message
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.view.animation.LinearInterpolator
import android.view.animation.RotateAnimation
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import dora.widget.swipelayout.R
import java.util.Timer
import java.util.TimerTask
import kotlin.math.tan

class SwipeLayout : RelativeLayout {

    private var state = INIT
    private var onSwipeListener: OnSwipeListener? = null
    private var downY = 0f
    private var lastY = 0f
    var pullDownY = 0f
    private var pullUpY = 0f
    private var refreshDist = 200f
    private var loadMoreDist = 200f
    private var timer: RefreshTimer? = null
    private var layout = false
    private var touch = false
    private var ratio = 2f
    private var rotateAnimation: RotateAnimation? = null
    private var refreshingAnimation: RotateAnimation? = null
    private var refreshView: View? = null
    private var refreshStateImageView: ImageView? = null
    private var refreshStateTextView: TextView? = null
    private var loadMoreView: View? = null
    private var loadStateImageView: ImageView? = null
    private var loadStateTextView: TextView? = null
    private var pullableView: View? = null
    private var events = 0
    private var canPullDown = true
    private var canPullUp = true

    private var updateHandler: Handler = @SuppressLint("HandlerLeak")
    object : Handler() {
        override fun handleMessage(msg: Message) {
            MOVE_SPEED = (8 + 5 * tan(
                (Math.PI / 2
                        / measuredHeight) * (pullDownY + Math.abs(pullUpY))
            )).toFloat()
            if (!touch) {
                if (state == REFRESHING && pullDownY <= refreshDist) {
                    pullDownY = refreshDist
                    timer!!.cancel()
                } else if (state == LOADING && -pullUpY <= loadMoreDist) {
                    pullUpY = -loadMoreDist
                    timer!!.cancel()
                }
            }
            if (pullDownY > 0) {
                pullDownY -= MOVE_SPEED
            } else if (pullUpY < 0) {
                pullUpY += MOVE_SPEED
            }
            if (pullDownY < 0) {
                pullDownY = 0f
                if (state != REFRESHING && state != LOADING) {
                    changeState(INIT)
                }
                timer!!.cancel()
                requestLayout()
            }
            if (pullUpY > 0) {
                pullUpY = 0f
                if (state != REFRESHING && state != LOADING) {
                    changeState(INIT)
                }
                timer!!.cancel()
                requestLayout()
            }
            requestLayout()
            if (pullDownY + Math.abs(pullUpY) == 0f) {
                timer!!.cancel()
            }
        }
    }

    fun setOnSwipeListener(l: OnSwipeListener) {
        onSwipeListener = l
    }

    constructor(context: Context) : super(context) {
        initView(context)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        initView(context)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyle: Int) : super(
        context,
        attrs,
        defStyle
    ) {
        initView(context)
    }

    private fun initView(context: Context) {
        if (isInEditMode) {
            return
        }
        timer = RefreshTimer(updateHandler) //循环UI检查
        rotateAnimation = AnimationUtils.loadAnimation(
            context, R.anim.anim_reverse
        ) as RotateAnimation
        refreshingAnimation = AnimationUtils.loadAnimation(
            context, R.anim.anim_rotating
        ) as RotateAnimation
        val lir = LinearInterpolator()
        rotateAnimation!!.interpolator = lir
        refreshingAnimation!!.interpolator = lir
    }

    private fun hide() {
        timer!!.schedule(5)
    }

    fun refreshFinish(refreshResult: Int) {
        refreshStateImageView!!.clearAnimation()
        when (refreshResult) {
            SUCCEED -> {
                refreshStateTextView!!.setText(R.string.sl_refresh_succeed)
                refreshStateImageView!!.setImageResource(R.drawable.ic_swipe_layout_refresh_succeed)
            }

            FAIL -> {
                refreshStateTextView!!.setText(R.string.sl_refresh_fail)
                refreshStateImageView!!.setImageResource(R.drawable.ic_swipe_layout_refresh_failed)
            }

            else -> {
                refreshStateTextView!!.setText(R.string.sl_refresh_fail)
                refreshStateImageView!!.setImageResource(R.drawable.ic_swipe_layout_refresh_failed)
            }
        }
        if (pullDownY > 0) {
            object : Handler() {
                override fun handleMessage(msg: Message) {
                    changeState(DONE)
                    hide()
                }
            }.sendEmptyMessageDelayed(0, 1000)
        } else {
            changeState(DONE)
            hide()
        }
    }

    fun loadMoreFinish(refreshResult: Int) {
        loadStateImageView!!.clearAnimation()
        when (refreshResult) {
            SUCCEED -> {
                loadStateTextView!!.setText(R.string.sl_load_succeed)
                loadStateImageView!!.setImageResource(R.drawable.ic_swipe_layout_load_succeed)
            }

            FAIL -> {
                loadStateTextView!!.setText(R.string.sl_load_fail)
                loadStateImageView!!.setImageResource(R.drawable.ic_swipe_layout_load_failed)
            }

            else -> {
                loadStateTextView!!.setText(R.string.sl_load_fail)
                loadStateImageView!!.setImageResource(R.drawable.ic_swipe_layout_load_failed)
            }
        }
        if (pullUpY < 0) {
            object : Handler() {
                override fun handleMessage(msg: Message) {
                    changeState(DONE)
                    hide()
                }
            }.sendEmptyMessageDelayed(0, 1000)
        } else {
            changeState(DONE)
            hide()
        }
    }

    private fun changeState(to: Int) {
        state = to
        when (state) {
            INIT -> {
                refreshStateImageView!!.setImageResource(R.drawable.ic_swipe_layout_logo)
                refreshStateTextView!!.setText(R.string.sl_pull_to_refresh)
                loadStateImageView!!.setImageResource(R.drawable.ic_swipe_layout_logo)
                loadStateTextView!!.setText(R.string.sl_pullup_to_load)
            }

            RELEASE_TO_REFRESH -> refreshStateTextView!!.setText(R.string.sl_release_to_refresh)
            REFRESHING -> {
                refreshStateTextView!!.setText(R.string.sl_refreshing)
                refreshStateImageView!!.setImageResource(R.drawable.ic_swipe_layout_refreshing)
                refreshStateImageView!!.startAnimation(refreshingAnimation)
            }

            RELEASE_TO_LOAD -> loadStateTextView!!.setText(R.string.sl_release_to_load)
            LOADING -> {
                loadStateTextView!!.setText(R.string.sl_loading)
                loadStateImageView!!.setImageResource(R.drawable.ic_swipe_layout_loading)
                loadStateImageView!!.startAnimation(refreshingAnimation)
            }

            DONE -> {}
        }
    }

    private fun releasePull() {
        canPullDown = true
        canPullUp = true
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downY = ev.y
                lastY = downY
                timer!!.cancel()
                events = 0
                releasePull()
            }

            MotionEvent.ACTION_POINTER_DOWN, MotionEvent.ACTION_POINTER_UP -> events = -1
            MotionEvent.ACTION_MOVE -> {
                if (events == 0) {
                    if (pullDownY > 0
                        || ((pullableView as Pullable?)!!.canPullDown()
                                && canPullDown && state != LOADING)
                    ) {
                        pullDownY = pullDownY + (ev.y - lastY) / ratio
                        if (pullDownY < 0) {
                            pullDownY = 0f
                            canPullDown = false
                            canPullUp = true
                        }
                        if (pullDownY > measuredHeight) {
                            pullDownY = measuredHeight.toFloat()
                        }
                        if (state == REFRESHING) {
                            touch = true
                        }
                    } else if (pullUpY < 0 || (pullableView as Pullable?)!!.canPullUp() && canPullUp && state != REFRESHING) {
                        pullUpY = pullUpY + (ev.y - lastY) / ratio
                        pullUpY = pullUpY + ev.y - lastY
                        if (pullUpY > 0) {
                            pullUpY = 0f
                            canPullDown = true
                            canPullUp = false
                        }
                        if (pullUpY < -measuredHeight) {
                            pullUpY = -measuredHeight.toFloat()
                        }
                        if (state == LOADING) {
                            touch = true
                        }
                    } else {
                        releasePull()
                    }
                } else {
                    events = 0
                }
                lastY = ev.y
                ratio = (2 + 2 * Math.tan(
                    Math.PI / 2 / measuredHeight
                            * (pullDownY + Math.abs(pullUpY))
                )).toFloat()
                if (pullDownY > 0 || pullUpY < 0) {
                    requestLayout()
                }
                if (pullDownY > 0) {
                    if (pullDownY <= refreshDist
                        && (state == RELEASE_TO_REFRESH || state == DONE)
                    ) {
                        changeState(INIT)
                    }
                    if (pullDownY >= refreshDist && state == INIT) {
                        changeState(RELEASE_TO_REFRESH)
                    }
                } else if (pullUpY < 0) {
                    if (-pullUpY <= loadMoreDist
                        && (state == RELEASE_TO_LOAD || state == DONE)
                    ) {
                        changeState(INIT)
                    }
                    if (-pullUpY >= loadMoreDist && state == INIT) {
                        changeState(RELEASE_TO_LOAD)
                    }
                }
                if (pullDownY + Math.abs(pullUpY) > 8) {
                    ev.action = MotionEvent.ACTION_CANCEL
                }
            }

            MotionEvent.ACTION_UP -> {
                if (pullDownY > refreshDist || -pullUpY > loadMoreDist) {
                    touch = false
                }
                if (state == RELEASE_TO_REFRESH) {
                    changeState(REFRESHING)
                    if (onSwipeListener != null) {
                        onSwipeListener!!.onRefresh(this)
                    }
                } else if (state == RELEASE_TO_LOAD) {
                    changeState(LOADING)
                    if (onSwipeListener != null) {
                        onSwipeListener!!.onLoadMore(this)
                    }
                }
                hide()
            }

            else -> {}
        }
        super.dispatchTouchEvent(ev)
        return true
    }

    private fun initView() {
        refreshStateTextView = refreshView!!
            .findViewById<View>(R.id.tv_refresh_state) as TextView
        refreshStateImageView = refreshView!!.findViewById<View>(R.id.iv_refresh_state) as ImageView
        loadStateTextView = loadMoreView!!
            .findViewById<View>(R.id.tv_load_state) as TextView
        loadStateImageView = loadMoreView!!.findViewById<View>(R.id.iv_load_state) as ImageView
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        if (!layout) {
            refreshView = getChildAt(0)
            pullableView = getChildAt(1)
            loadMoreView = getChildAt(2)
            layout = true
            initView()
            refreshDist = (refreshView as ViewGroup?)!!.getChildAt(0)
                .measuredHeight.toFloat()
            loadMoreDist = (loadMoreView as ViewGroup?)!!.getChildAt(0)
                .measuredHeight.toFloat()
        }
        refreshView!!.layout(
            0,
            (pullDownY + pullUpY).toInt() - refreshView!!.measuredHeight,
            refreshView!!.measuredWidth, (pullDownY + pullUpY).toInt()
        )
        pullableView!!.layout(
            0,
            (pullDownY + pullUpY).toInt(),
            pullableView!!.measuredWidth,
            (pullDownY + pullUpY).toInt() + pullableView!!.measuredHeight
        )
        loadMoreView!!.layout(
            0,
            (pullDownY + pullUpY).toInt() + pullableView!!.measuredHeight,
            loadMoreView!!.measuredWidth,
            (pullDownY + pullUpY).toInt() + pullableView!!.measuredHeight
                    + loadMoreView!!.measuredHeight
        )
    }

    internal inner class RefreshTimer(private val handler: Handler) {

        private val timer: Timer = Timer()
        private var task: RefreshTask? = null

        fun schedule(period: Long) {
            if (task != null) {
                task!!.cancel()
                task = null
            }
            task = RefreshTask(handler)
            timer.schedule(task, 0, period)
        }

        fun cancel() {
            if (task != null) {
                task!!.cancel()
                task = null
            }
        }

        private inner class RefreshTask(private val handler: Handler) : TimerTask() {
            override fun run() {
                handler.obtainMessage().sendToTarget()
            }
        }
    }

    interface OnSwipeListener {

        fun onRefresh(swipeLayout: SwipeLayout)
        fun onLoadMore(swipeLayout: SwipeLayout)
    }

    companion object {
        const val INIT = 0
        const val RELEASE_TO_REFRESH = 1
        const val REFRESHING = 2
        const val RELEASE_TO_LOAD = 3
        const val LOADING = 4
        const val DONE = 5
        const val SUCCEED = 0
        const val FAIL = 1
        var MOVE_SPEED = 8f
    }
}