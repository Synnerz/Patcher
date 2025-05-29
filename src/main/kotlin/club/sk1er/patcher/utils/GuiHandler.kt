package club.sk1er.patcher.utils

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiScreen
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent

object GuiHandler {
    var newGui: GuiScreen? = null

    fun openGui(gui: GuiScreen) {
        newGui = gui
    }

    @SubscribeEvent
    fun onTick(event: TickEvent.ClientTickEvent) {
        if (event.phase !== TickEvent.Phase.END || newGui == null) return

        newGui?.let { Minecraft.getMinecraft().displayGuiScreen(it) }
        newGui = null
    }
}