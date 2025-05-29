package club.sk1er.patcher;

import club.sk1er.patcher.asm.render.screen.GuiChatTransformer;
import club.sk1er.patcher.commands.*;
import club.sk1er.patcher.config.PatcherConfig;
import club.sk1er.patcher.config.PatcherSoundConfig;
import club.sk1er.patcher.ducks.FontRendererExt;
import club.sk1er.patcher.hooks.EntityRendererHook;
import club.sk1er.patcher.hooks.MinecraftHook;
import club.sk1er.patcher.mixins.features.network.packet.C01PacketChatMessageMixin_ExtendedChatLength;
import club.sk1er.patcher.render.ScreenshotPreview;
import club.sk1er.patcher.screen.PatcherMenuEditor;
import club.sk1er.patcher.screen.render.caching.HUDCaching;
import club.sk1er.patcher.screen.render.overlay.ArmorStatusRenderer;
import club.sk1er.patcher.screen.render.overlay.GlanceRenderer;
import club.sk1er.patcher.screen.render.overlay.ImagePreview;
import club.sk1er.patcher.screen.render.overlay.OverlayHandler;
import club.sk1er.patcher.screen.render.overlay.metrics.MetricsRenderer;
import club.sk1er.patcher.screen.render.title.TitleFix;
import club.sk1er.patcher.tweaker.PatcherTweaker;
import club.sk1er.patcher.util.chat.ChatHandler;
import club.sk1er.patcher.util.enhancement.EnhancementManager;
import club.sk1er.patcher.util.enhancement.ReloadListener;
import club.sk1er.patcher.util.fov.FovHandler;
import club.sk1er.patcher.util.keybind.FunctionKeyChanger;
import club.sk1er.patcher.util.keybind.KeybindDropModifier;
import club.sk1er.patcher.util.keybind.MousePerspectiveKeybindHandler;
import club.sk1er.patcher.util.keybind.linux.LinuxKeybindFix;
import club.sk1er.patcher.util.screenshot.AsyncScreenshots;
import club.sk1er.patcher.util.status.ProtocolVersionDetector;
import club.sk1er.patcher.util.world.SavesWatcher;
import club.sk1er.patcher.util.world.render.culling.EntityCulling;
import club.sk1er.patcher.util.world.render.entity.EntityRendering;
import club.sk1er.patcher.util.world.sound.SoundHandler;
import club.sk1er.patcher.util.world.sound.audioswitcher.AudioSwitcher;
import club.sk1er.patcher.utils.GuiHandler;
import club.sk1er.patcher.utils.Notifications;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.resources.IReloadableResourceManager;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.common.ForgeVersion;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.ModContainer;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLLoadCompleteEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

@Mod(modid = "patcher", name = "Patcher", version = Patcher.VERSION, clientSideOnly = true)
public class Patcher {

    @Mod.Instance("patcher")
    public static Patcher instance;

    // normal versions will be "1.x.x"
    // betas will be "1.x.x+beta-y" / "1.x.x+branch_beta-y"
    // rcs will be 1.x.x+rc-y
    // extra branches will be 1.x.x+branch-y
    public static final String VERSION = "1.8.9";

    private final Logger logger = LogManager.getLogger("Patcher");
    private final File logsDirectory = new File(Minecraft.getMinecraft().mcDataDir + File.separator + "logs" + File.separator);

    /**
     * Create a set of blacklisted servers, used for when a specific server doesn't allow for 1.8 clients to use
     * our 1.11 text length modifier (bring message length from 100 to 256, as done in 1.11 and above) {@link Patcher#addOrRemoveBlacklist(String)}.
     */
    private final Set<String> blacklistedServers = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
    private final File blacklistedServersFile = new File("./config/blacklisted_servers.txt");

    private final SavesWatcher savesWatcher = new SavesWatcher();
    private final AudioSwitcher audioSwitcher = new AudioSwitcher();

    private KeyBinding dropModifier, hideScreen, customDebug, clearShaders;

    private PatcherConfig patcherConfig;
    private PatcherSoundConfig patcherSoundConfig;

    private boolean loadedGalacticFontRenderer;

    @Mod.EventHandler
    public void onInit(FMLInitializationEvent event) {
        registerEvents(Notifications.INSTANCE, GuiHandler.INSTANCE);
        registerKeybinds(
            dropModifier = new KeybindDropModifier(),
            hideScreen = new FunctionKeyChanger.KeybindHideScreen(),
            customDebug = new FunctionKeyChanger.KeybindCustomDebug(),
            clearShaders = new FunctionKeyChanger.KeybindClearShaders()
        );

        patcherConfig = PatcherConfig.INSTANCE;
        patcherSoundConfig = new PatcherSoundConfig();

        SoundHandler soundHandler = new SoundHandler();
        IReloadableResourceManager resourceManager = (IReloadableResourceManager) Minecraft.getMinecraft().getResourceManager();
        resourceManager.registerReloadListener(soundHandler);
        resourceManager.registerReloadListener(new ReloadListener());

        ClientCommandHandler.instance.registerCommand(new PatcherCommand());
        ClientCommandHandler.instance.registerCommand(new AsyncScreenshots.FavoriteScreenshot());
        ClientCommandHandler.instance.registerCommand(new AsyncScreenshots.DeleteScreenshot());
        ClientCommandHandler.instance.registerCommand(new AsyncScreenshots.UploadScreenshot());
        ClientCommandHandler.instance.registerCommand(new AsyncScreenshots.CopyScreenshot());
        ClientCommandHandler.instance.registerCommand(new AsyncScreenshots.ScreenshotsFolder());

        registerEvents(
            this, soundHandler, dropModifier, audioSwitcher,
            new OverlayHandler(), new EntityRendering(), new FovHandler(),
            new ChatHandler(), new GlanceRenderer(), new EntityCulling(),
            new ArmorStatusRenderer(), new PatcherMenuEditor(), new ImagePreview(),
            new TitleFix(), new LinuxKeybindFix(),
            new MetricsRenderer(), new HUDCaching(), new EntityRendererHook(),
            MinecraftHook.INSTANCE, ScreenshotPreview.INSTANCE,
            new MousePerspectiveKeybindHandler()
        );

        checkLogs();
        loadBlacklistedServers();
        fixSettings();

        this.savesWatcher.watch();
    }

    @EventHandler
    public void onPostInit(FMLPostInitializationEvent event) {
        if (!loadedGalacticFontRenderer) {
            loadedGalacticFontRenderer = true;
            FontRenderer galacticFontRenderer = Minecraft.getMinecraft().standardGalacticFontRenderer;
            if (galacticFontRenderer instanceof FontRendererExt) {
                ((FontRendererExt) galacticFontRenderer).patcher$getFontRendererHook().create();
            }
        }
    }

    @EventHandler
    public void onLoadComplete(FMLLoadCompleteEvent event) {
        List<ModContainer> activeModList = Loader.instance().getActiveModList();
        Notifications notifications = Notifications.INSTANCE;
        this.detectIncompatibilities(activeModList, notifications);

        long time = (System.currentTimeMillis() - PatcherTweaker.clientLoadTime) / 1000L;
        if (PatcherConfig.startupNotification) {
            notifications.push("Minecraft started in " + time + " seconds.");
        }

        logger.info("Minecraft started in {} seconds.", time);

        //noinspection ConstantConditions
        if (!ForgeVersion.mcVersion.equals("1.8.9") || ForgeVersion.getVersion().contains("2318")) return;
        notifications.push("Outdated Forge has been detected (" + ForgeVersion.getVersion() + "). " +
            "Click to open the Forge website to download the latest version.", 30);
    }

    /**
     * Runs when the user connects to a server.
     * Goes through the process of checking the current state of the server.
     * <p>
     * If the server is local, return and set the chat length to 256, as we modify the client to allow for
     * 256 message length in singleplayer through Mixins in {@link C01PacketChatMessageMixin_ExtendedChatLength}.
     * <p>
     * If the server is blacklisted, return and set the chat length to 100, as that server does not support 256 long
     * chat messages, and was manually blacklisted by the player.
     * <p>
     * If the server is not local nor blacklisted, check the servers protocol and see if it supports 315, aka 1.11.
     * If it does, then set the message length max to 256, otherwise return to 100.
     *
     * @param event {@link FMLNetworkEvent.ClientConnectedToServerEvent}
     */
    //#if MC==10809
    @SubscribeEvent
    public void connectToServer(FMLNetworkEvent.ClientConnectedToServerEvent event) {
        if (event.isLocal) {
            GuiChatTransformer.maxChatLength = 256;
            return;
        }

        String serverIP = Minecraft.getMinecraft().getCurrentServerData().serverIP;
        if (serverIP == null || blacklistedServers.contains(serverIP)) {
            GuiChatTransformer.maxChatLength = 100;
            return;
        }

        boolean compatible = ProtocolVersionDetector.instance.isCompatibleWithVersion(
            serverIP,
            315 // 1.11
        );

        GuiChatTransformer.maxChatLength = compatible ? 256 : 100;
    }
    //#endif

    @SubscribeEvent
    public void clientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.START) EnhancementManager.getInstance().tick();
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private void checkLogs() {
        if (PatcherConfig.logOptimizer && logsDirectory.exists()) {
            File[] files = logsDirectory.listFiles();
            if (files == null) return;

            for (File file : files) {
                if (file.getName().endsWith("log.gz") && file.lastModified() <= (System.currentTimeMillis() - PatcherConfig.logOptimizerLength * 86400000L)) {
                    file.delete();
                }
            }
        }
    }

    private void registerKeybinds(KeyBinding... keybinds) {
        for (KeyBinding keybind : keybinds) {
            ClientRegistry.registerKeyBinding(keybind);
        }
    }

    private void registerEvents(Object... events) {
        for (Object event : events) {
            MinecraftForge.EVENT_BUS.register(event);
        }
    }

    private boolean isServerBlacklisted(String ip) {
        if (ip == null) return false;
        String trim = ip.trim();
        return !trim.isEmpty() && blacklistedServers.contains(trim);
    }

    public boolean addOrRemoveBlacklist(String input) {
        if (input == null || input.isEmpty() || input.trim().isEmpty()) {
            return false;
        } else {
            input = input.trim();

            if (isServerBlacklisted(input)) {
                blacklistedServers.remove(input);
                return false;
            } else {
                blacklistedServers.add(input);
                return true;
            }
        }
    }

    public void saveBlacklistedServers() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(blacklistedServersFile))) {
            File parentFile = blacklistedServersFile.getParentFile();
            if (!parentFile.exists() && !parentFile.mkdirs()) {
                return;
            }

            if (!blacklistedServersFile.exists() && !blacklistedServersFile.createNewFile()) {
                return;
            }

            for (String server : blacklistedServers) {
                writer.write(server + System.lineSeparator());
            }
        } catch (IOException e) {
            logger.error("Failed to save blacklisted servers.", e);
        }
    }

    private void loadBlacklistedServers() {
        if (!blacklistedServersFile.exists()) {
            return;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(blacklistedServersFile))) {
            String servers;

            while ((servers = reader.readLine()) != null) {
                blacklistedServers.add(servers);
            }
        } catch (IOException e) {
            logger.error("Failed to load blacklisted servers.", e);
        }
    }

    private void fixSettings() {
        if (PatcherConfig.customZoomSensitivity > 1.0F) PatcherConfig.customZoomSensitivity = 1.0F;
        if (PatcherConfig.tabOpacity > 1.0F) PatcherConfig.tabOpacity = 1.0F;
        if (PatcherConfig.imagePreviewWidth > 1.0F) PatcherConfig.imagePreviewWidth = 0.5F;
        if (PatcherConfig.previewScale > 1.0F) PatcherConfig.previewScale = 1.0F;
        if (PatcherConfig.unfocusedFPSAmount < 15) PatcherConfig.unfocusedFPSAmount = 15;
        if (PatcherConfig.fireOverlayHeight < -0.5F || PatcherConfig.fireOverlayHeight > 1.5F) {
            PatcherConfig.fireOverlayHeight = 0.0F;
        }

        this.forceSaveConfig();
    }

    private void detectIncompatibilities(List<ModContainer> activeModList, Notifications notifications) {
        for (ModContainer container : activeModList) {
            String modId = container.getModId();
            String baseMessage = container.getName() + " has been detected. ";
            if (PatcherConfig.entityCulling && modId.equals("enhancements")) {
                notifications.push(baseMessage + "Entity Culling is now disabled.");
                PatcherConfig.entityCulling = false;
            }

            if ((modId.equals("labymod") || modId.equals("enhancements")) || modId.equals("hychat")) {
                if (PatcherConfig.compactChat) {
                    notifications.push(baseMessage + "Compact Chat is now disabled.");
                    PatcherConfig.compactChat = false;
                }

                if (PatcherConfig.chatPosition) {
                    notifications.push(baseMessage + "Chat Position is now disabled.");
                    PatcherConfig.chatPosition = false;
                }
            }

            if (PatcherConfig.optimizedFontRenderer && modId.equals("smoothfont")) {
                notifications.push(baseMessage + "Optimized Font Renderer is now disabled.");
                PatcherConfig.optimizedFontRenderer = false;
            }
        }

        try {
            Class.forName("net.labymod.addons.resourcepacks24.Resourcepacks24", false, getClass().getClassLoader());
            notifications.push("The LabyMod addon \"Resourcepacks24\" conflicts with Patcher's resourcepack optimizations. Please remove it to make it work again.");
        } catch (ClassNotFoundException ignored) {

        }

        this.forceSaveConfig();
    }

    public PatcherConfig getPatcherConfig() {
        return patcherConfig;
    }

    public PatcherSoundConfig getPatcherSoundConfig() {
        return patcherSoundConfig;
    }

    public Logger getLogger() {
        return logger;
    }

    public KeyBinding getDropModifier() {
        return dropModifier;
    }

    public KeyBinding getHideScreen() {
        return hideScreen;
    }

    public KeyBinding getCustomDebug() {
        return customDebug;
    }

    public KeyBinding getClearShaders() {
        return clearShaders;
    }

    public AudioSwitcher getAudioSwitcher() {
        return audioSwitcher;
    }

    public void forceSaveConfig() {
        this.patcherConfig.markDirty();
        this.patcherConfig.writeData();
    }
}
