package club.sk1er.patcher.utils

import net.minecraftforge.client.event.GuiScreenEvent.DrawScreenEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class Notification @JvmOverloads constructor(
    val text: String,
    val time: Double = 5.0, // seconds
    var startedAt: Long = 0L
)

// As opposite to essential's notification system this will not attempt to put them ontop of each other
// done this way for an easier implementation since it does not matter here
object Notifications {
    val children = mutableListOf<Notification>()
    var currentItem: Notification? = null

    @SubscribeEvent
    fun onPostDraw(event: DrawScreenEvent.Post) {
        val child = children.last()
        if (currentItem != null && currentItem !== child) {
            if ((System.currentTimeMillis() - child.startedAt) / 1000 >= child.time) {
                currentItem = null
                children.remove(child)
            }
            return
        }

        if (child.startedAt == 0L) child.startedAt = System.currentTimeMillis()
        if ((System.currentTimeMillis() - child.startedAt) / 1000 >= child.time) {
            currentItem = null
            children.remove(child)
            return
        }

        Utils.drawRect(0.0, 0.0, Utils.fontRenderer.getStringWidth(child.text) + 0.0, 20.0)
        Utils.fontRenderer.drawStringWithShadow(child.text, 1f, 1f, 0xFFFFF)

        currentItem = child
    }

    @JvmOverloads
    fun push(text: String, time: Double = 5.0) {
        children.add(Notification(text, time))
    }
}