package club.sk1er.patcher.utils

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.FontRenderer
import net.minecraft.client.renderer.Tessellator
import net.minecraft.client.renderer.WorldRenderer
import net.minecraft.client.renderer.vertex.DefaultVertexFormats
import java.util.*
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadFactory
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

object Utils {
    val threadPool = ThreadPoolExecutor(
        5,
        10,
        0L,
        TimeUnit.SECONDS,
        LinkedBlockingQueue(),
        ThreadFactory { Thread(it) })
    val mc = Minecraft.getMinecraft()
    val tessellator: Tessellator by lazy { Tessellator.getInstance() }
    val worldRenderer: WorldRenderer by lazy { tessellator.worldRenderer }
    val fontRenderer: FontRenderer by lazy { mc.fontRendererObj }

    @JvmOverloads
    fun drawRect(x: Double, y: Double, width: Double, height: Double, solid: Boolean = true) {
        worldRenderer.begin(if (solid) 6 else 2, DefaultVertexFormats.POSITION)
        worldRenderer.pos(x, y + height, 0.0).endVertex()
        worldRenderer.pos(x + width, y + height, 0.0).endVertex()
        worldRenderer.pos(x + width, y, 0.0).endVertex()
        worldRenderer.pos(x, y, 0.0).endVertex()
        tessellator.draw()
    }

    fun runAsync(runnable: Runnable) {
        threadPool.execute(runnable)
    }

    fun inHypixel(): Boolean {
        if (mc.thePlayer == null || mc.theWorld == null || mc.isSingleplayer) return false

        val player = mc.thePlayer
        val serverName = player.clientBrand ?: return false

        return "hypixel" in serverName.lowercase(Locale.ENGLISH)
    }
}