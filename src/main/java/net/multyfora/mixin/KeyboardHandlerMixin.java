package net.multyfora.mixin;

import net.minecraft.client.KeyboardHandler;

import net.multyfora.client.portable_typewriter.PortableTypewriterClientHandler;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardHandler.class)
public class KeyboardHandlerMixin {

    @Inject(method = "keyPress", at = @At("HEAD"), cancellable = true)
    private void joc$onKeyPress(long window, int key, int scancode, int action, int modifiers, CallbackInfo ci) {
        if (PortableTypewriterClientHandler.MODE != PortableTypewriterClientHandler.Mode.IDLE) {
            ci.cancel();
        }
    }
}
