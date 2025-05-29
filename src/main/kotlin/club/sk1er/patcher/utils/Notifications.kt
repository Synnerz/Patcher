package club.sk1er.patcher.utils

import net.minecraft.client.renderer.GlStateManager
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
        if (children.isEmpty()) return
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

        GlStateManager.enableBlend()
        GlStateManager.disableTexture2D()
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0)
        GlStateManager.disableCull()
        GlStateManager.color(50f / 505f, 50f / 505f, 50f / 255f)
        Utils.drawRect(4.0, 4.0, (Utils.fontRenderer.getStringWidth(child.text) + 2.0) + 2, 22.0, false)
        GlStateManager.color(15f / 155f, 15f / 155f, 15f / 255f)
        Utils.drawRect(5.0, 5.0, Utils.fontRenderer.getStringWidth(child.text) + 2.0, 20.0)
        GlStateManager.enableTexture2D()
        GlStateManager.disableBlend()
        GlStateManager.enableCull()
        Utils.fontRenderer.drawStringWithShadow(child.text, 6f, 20f / 2f, 0xFFFFFFFF.toInt())

        currentItem = child
    }

    @JvmOverloads
    fun push(text: String, time: Double = 5.0) {
        children.add(Notification(text, time))
    }
}