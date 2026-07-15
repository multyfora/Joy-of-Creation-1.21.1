package net.multyfora.content.portable_throttle;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.util.Mth;
import com.simibubi.create.content.equipment.potatoCannon.PotatoProjectileEntity;
import com.simibubi.create.content.equipment.zapper.ShootableGadgetRenderHandler;
import dev.engine_room.flywheel.lib.transform.TransformStack;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import net.multyfora.client.portable_throttle.PortableThrottleClientHandler;
import net.multyfora.client.portable_throttle.PortableThrottleStrengthScreen;

public class PortableThrottleRenderHandler extends ShootableGadgetRenderHandler {
    private float nextPitch;

    @Override
    protected void playSound(InteractionHand hand, Vec3 position) {
        PotatoProjectileEntity.playLaunchSound(Minecraft.getInstance().level, position, nextPitch);
    }

    @Override
    protected boolean appliesTo(ItemStack stack) {
        return stack.getItem() instanceof PortableThrottleItem;
    }

    @Override
    protected void transformTool(PoseStack ms, float flip, float equipProgress, float recoil, float pt) {
        float grip = PortableThrottleClientHandler.getGripProgress();
        
        double x = Mth.lerp(grip, flip * -0.1f, 0.0f);
        double y = Mth.lerp(grip, 0, 0.03f); // slightly raised when two-handed
        double z = Mth.lerp(grip, 0.14f, 0.03f); // pulled back slightly
        
        ms.translate(x, y, z);
        ms.scale(1.25f, 1.25f, 1.25f);
        
        float rotationX = Mth.lerp(grip, recoil * 80, recoil * 80 - 15);
        TransformStack.of(ms).rotateXDegrees(rotationX);
    }

    @Override
    protected void transformHand(PoseStack ms, float flip, float equipProgress, float recoil, float pt) {
        float grip = PortableThrottleClientHandler.getGripProgress();
        
        if (grip <= 0.01f) {
            ms.translate(flip * -0.09, -0.275, -0.25);
            TransformStack.of(ms).rotateZDegrees(flip * -10);
            return;
        }
        
        float knobProgress = PortableThrottleClientHandler.getLastStrength() / 15f;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && mc.screen instanceof PortableThrottleStrengthScreen) {
            knobProgress = PortableThrottleClientHandler.getLiveValue();
        }
        
        float effectiveProgress = 1.0f - knobProgress;
        
        double targetY = Mth.lerp(effectiveProgress, -0.25, -0.10);
        
        double targetZ = Mth.lerp(effectiveProgress, -0.25, -0.05);
        
        double targetX = flip * -0.02;
        
        double x = Mth.lerp(grip, flip * -0.09, targetX);
        double y = Mth.lerp(grip, -0.275, targetY);
        double z = Mth.lerp(grip, -0.25, targetZ);
        
        ms.translate(x, y, z);
        TransformStack.of(ms).rotateZDegrees(Mth.lerp(grip, flip * -10, flip * -35));
    }
}