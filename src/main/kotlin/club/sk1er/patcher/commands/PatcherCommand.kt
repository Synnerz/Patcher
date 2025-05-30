package club.sk1er.patcher.commands

import club.sk1er.patcher.Patcher
import club.sk1er.patcher.config.PatcherConfig
import club.sk1er.patcher.util.chat.ChatUtilities
import club.sk1er.patcher.utils.GuiHandler
import net.minecraft.client.Minecraft
import net.minecraft.client.entity.EntityPlayerSP

class PatcherCommand : BaseCommand("patcher") {
    val scaleTypes = mapOf(
        "off" to 0,
        "small" to 1,
        "normal" to 2,
        "large" to 3,
        "auto" to 5
    )

    override fun processCommand(player: EntityPlayerSP, args: Array<String>) {
        if (args.isEmpty()) {
            Patcher.instance.patcherConfig.gui()?.let { GuiHandler.openGui(it) }
            return
        }

        // TODO: find out if these are the correct implementation for the commands
        //  too lazy to look at essential's implementation
        val par = args.getOrNull(1)

        when (args[0].lowercase()) {
            "blacklist" -> par?.let { blacklist(it) }
            "fov" -> par?.let { fov(it.toFloat()) }
            "scale" -> par?.let { scale(it) }
            "sendcoords" -> sendCoords(par)
            "sounds" -> sounds()
            "fps" -> par?.let { fps(it.toInt()) }
            "help" -> help()
        }
    }

    fun help() {
        ChatUtilities.sendMessage("&c/patcher blacklist <ip> &7Tell the client that you don't want to use the 1.11+ chat length on the specified server IP.", false)
        ChatUtilities.sendMessage("&c/patcher fov <amount> &7Change your FOV to a custom value.", false)
        ChatUtilities.sendMessage("&c/patcher scale <type> &7Change the scale of your inventory independent of your GUI scale. &8types: help, off, none, small, normal, large, auto", false)
        ChatUtilities.sendMessage("&c/patcher sendcoords <additional information> &7Send your current coordinates in chat. Anything after 'sendcoords' will be put at the end of the message.", false)
        ChatUtilities.sendMessage("&c/patcher sounds &7Open the Sound Configuration GUI.", false)
        ChatUtilities.sendMessage("&c/patcher fps <amount> &7Choose what to limit the game's framerate to outside of Minecraft's options. 0 will use your normal framerate.", false)
    }

    fun blacklist(ip: String) {
        val status = if (Patcher.instance.addOrRemoveBlacklist(ip)) "&cnow" else "&ano longer"
        ChatUtilities.sendMessage("Server &e$ip &ris $status &rblacklisted from chat length extension.")
        Patcher.instance.saveBlacklistedServers()
    }

    fun fov(amount: Float) {
        if (amount <= 0) return ChatUtilities.sendMessage("Changing your FOV to or below 0 is disabled due to game-breaking visual bugs.")
        else if (amount > 110) return ChatUtilities.sendMessage("Changing your FOV above 110 is disabled due to game-breaking visual bugs.")

        val mc = Minecraft.getMinecraft()
        ChatUtilities.sendMessage("FOV changed from &e${mc.gameSettings.fovSetting} &rto &a$amount&r.")
        mc.gameSettings.fovSetting = amount
        mc.gameSettings.saveOptions()
    }

    fun scale(type: String) {
        if (type == "help") {
            ChatUtilities.sendMessage("             &eInventory Scale", false)
            ChatUtilities.sendMessage("&7Usage: /patcher inventoryscale <scaling>", false)
            ChatUtilities.sendMessage("&7Scaling may be a number between 1-5, or", false)
            ChatUtilities.sendMessage("&7small/normal/large/auto", false)
            ChatUtilities.sendMessage("&7Use '/patcher inventoryscale off' to disable scaling.", false)
            return
        }

        if (type == "off" || type == "none") {
            ChatUtilities.sendMessage("Disabled inventory scaling.")
            PatcherConfig.inventoryScale = 0
            Patcher.instance.forceSaveConfig()
            return
        }

        val scaling = scaleTypes[type] ?: 0

        if (scaling < 1) {
            ChatUtilities.sendMessage("Disabled inventory scaling.")
            PatcherConfig.inventoryScale = 0
            Patcher.instance.forceSaveConfig()
            return
        }

        ChatUtilities.sendMessage("Set inventory scaling to &a$scaling&r")
        PatcherConfig.inventoryScale = scaling
        Patcher.instance.forceSaveConfig()
    }

    fun sendCoords(str: String?) {
        val playerSP = Minecraft.getMinecraft().thePlayer
        playerSP.sendChatMessage("x: ${playerSP.posX.toInt()}, y: ${playerSP.posY.toInt()}, z: ${playerSP.posZ.toInt()} ${str ?: ""}")
    }

    fun sounds() {
        Patcher.instance.patcherSoundConfig.gui()?.let { GuiHandler.openGui(it) }
    }

    fun fps(amount: Int) {
        if (amount < 0) return ChatUtilities.sendMessage("You cannot set your framerate to a negative number.")
        else if (amount == PatcherConfig.customFpsLimit) return ChatUtilities.sendMessage("Custom framerate is already set to this value.")

        PatcherConfig.customFpsLimit = amount
        Patcher.instance.forceSaveConfig()

        ChatUtilities.sendMessage(if (amount == 0) "Custom framerate was reset." else "Custom framerate set to &a$amount")
    }
}