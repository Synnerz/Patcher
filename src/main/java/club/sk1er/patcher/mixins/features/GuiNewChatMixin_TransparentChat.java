package club.sk1er.patcher.mixins.features;

import club.sk1er.patcher.config.PatcherConfig;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiNewChat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(GuiNewChat.class)
public abstract class GuiNewChatMixin_TransparentChat extends Gui {

    @Shadow
    public abstract boolean getChatOpen();

    @WrapWithCondition(method = "drawChat", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiNewChat;drawRect(IIIII)V", ordinal = 0))
    private boolean patcher$transparentChat(int left, int top, int right, int bottom, int color) {
        if (PatcherConfig.transparentChat) {
            return PatcherConfig.transparentChatOnlyWhenClosed && getChatOpen();
        }
        return true;
    }
}
