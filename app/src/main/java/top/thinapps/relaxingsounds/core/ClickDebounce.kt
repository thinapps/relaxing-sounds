package top.thinapps.relaxingsounds.core

object ClickDebounce {

    private var lastClickTime = 0L
    private const val delayMs = 350L

    fun allowClick(): Boolean {
        val now = System.currentTimeMillis()
        if (now - lastClickTime < delayMs) return false
        lastClickTime = now
        return true
    }

    fun reset() {
        lastClickTime = 0L
    }
}
