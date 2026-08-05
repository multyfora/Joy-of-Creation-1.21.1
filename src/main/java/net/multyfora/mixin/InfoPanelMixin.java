package net.multyfora.mixin;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.multyfora.client.JocAnimatedLogo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.List;
@Mixin(targets = "net.neoforged.neoforge.client.gui.ModListScreen$InfoPanel")
public abstract class InfoPanelMixin {

    @Shadow
    private ResourceLocation logoPath;

    @Inject(method = "setInfo", at = @At("TAIL"))
    private void joc$animateOurLogo(List<String> lines, ResourceLocation logoPath, net.neoforged.neoforge.common.util.Size2i logoDims, CallbackInfo ci) {
        if (!JocAnimatedLogo.isJocSelected() || this.logoPath == null)
            return;
        JocAnimatedLogo.install(Minecraft.getInstance().getTextureManager(), this.logoPath);
    }
}
