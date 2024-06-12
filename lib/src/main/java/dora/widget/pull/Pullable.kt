package dora.widget.pull

interface Pullable {

    fun canPullDown(): Boolean
    fun canPullUp(): Boolean
}