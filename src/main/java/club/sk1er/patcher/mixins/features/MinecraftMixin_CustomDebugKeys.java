package club.sk1er.patcher.mixins.features;

import club.sk1er.patcher.Patcher;
import net.minecraft.client.Minecraft;
import org.lwjgl.input.Keyboard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(Minecraft.class)
public class MinecraftMixin_CustomDebugKeys {
    //#if MC==10809
    @ModifyConstant(method = "runTick", constant = @Constant(intValue = Keyboard.KEY_F3))
    private int patcher$replaceF3KeyChecks(int original) {
        try {
            int customDebugKeycode = Patcher.instance.getCustomDebug().getKeyCode();
            if (customDebugKeycode != Keyboard.KEY_NONE) {
                return customDebugKeycode;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return original;
    }

    @ModifyConstant(method = "runTick", constant = @Constant(intValue = Keyboard.KEY_F1))
    private int patcher$replaceF1KeyCheck(int original) {
        try {
            int hideScreenKeycode = Patcher.instance.getHideScreen().getKeyCode();
            if (hideScreenKeycode != Keyboard.KEY_NONE) {
                return hideScreenKeycode;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return original;
    }

    @ModifyConstant(method = "runTick", constant = @Constant(intValue = Keyboard.KEY_F4))
    private int patcher$replaceF4KeyCheck(int original) {
        try {
            int clearShadersKeycode = Patcher.instance.getClearShaders().getKeyCode();
            if (clearShadersKeycode != Keyboard.KEY_NONE) {
                return clearShadersKeycode;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return original;
    }
    //#endif
}
