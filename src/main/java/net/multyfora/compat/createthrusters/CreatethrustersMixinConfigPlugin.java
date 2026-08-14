package net.multyfora.compat.createthrusters;

import net.neoforged.fml.ModList;

import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

/**
 * Disables the whole createthrusters compat mixin set when the createthrusters
 * mod is not loaded, so the optional [[mixins]] config never applies against
 * missing classes.
 **/
public final class CreatethrustersMixinConfigPlugin implements IMixinConfigPlugin {

    private static boolean enabled;

    @Override
    public void onLoad(String mixinPackage) {
        enabled = ModList.get() != null && ModList.get().isLoaded("createthrusters");
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        return true;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return enabled ? null : List.of();
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }
}