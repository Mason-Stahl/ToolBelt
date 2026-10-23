package dev.gigaherz.toolbelt.mixin.client;

import dev.gigaherz.toolbelt.client.InputHelper;
import dev.gigaherz.toolbelt.client.RadialMenuScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.player.ClientInput;
import net.minecraft.client.player.KeyboardInput;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.Vec2;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Lets the player keep moving while the radial menu is open. Opening a screen releases every
 * key mapping, so movement is rebuilt here from the physical key state instead.
 */
@Mixin(KeyboardInput.class)
public abstract class KeyboardInputMixin extends ClientInput
{
    @Shadow
    @Final
    private Options options;

    @Inject(method = "tick", at = @At("TAIL"))
    private void toolbelt$moveWhileRadialMenuOpen(CallbackInfo ci)
    {
        if (!(Minecraft.getInstance().gui.screen() instanceof RadialMenuScreen))
            return;

        this.keyPresses = new Input(
                InputHelper.isPhysicallyDown(options.keyUp),
                InputHelper.isPhysicallyDown(options.keyDown),
                InputHelper.isPhysicallyDown(options.keyLeft),
                InputHelper.isPhysicallyDown(options.keyRight),
                InputHelper.isPhysicallyDown(options.keyJump),
                InputHelper.isPhysicallyDown(options.keyShift),
                InputHelper.isPhysicallyDown(options.keySprint)
        );
        float forward = impulse(keyPresses.forward(), keyPresses.backward());
        float left = impulse(keyPresses.left(), keyPresses.right());
        this.moveVector = new Vec2(left, forward).normalized();
    }

    private static float impulse(boolean positive, boolean negative)
    {
        return positive == negative ? 0.0F : (positive ? 1.0F : -1.0F);
    }
}
