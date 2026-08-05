package net.multyfora.mixin;
import net.multyfora.client.JocAnimatedLogo;
import net.neoforged.neoforge.client.gui.ModListScreen;
import net.neoforged.neoforge.client.gui.widget.ModListWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(ModListScreen.class)
public abstract class ModListScreenMixin {
    @Inject(method = "setSelected", at = @At("HEAD"))
    private void joc$trackSelectedMod(ModListWidget.ModEntry entry, CallbackInfo ci) {
        JocAnimatedLogo.setSelectedModId(entry != null ? entry.getInfo().getModId() : null);
    }
}
