package net.multyfora.client.portable_typewriter;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.fml.common.asm.enumextension.EnumProxy;
import net.neoforged.neoforge.client.IArmPoseTransformer;

public final class TypewriterArmPose {
    private static final float ARM_PITCH = -0.95F;
    private static final float ARM_INWARD = 0.42F;
    private static final float TAP_AMOUNT = 0.07F;
    private static final float TAP_SPEED = 1.6F;

    public static final EnumProxy<HumanoidModel.ArmPose> TYPING = new EnumProxy<>(
        HumanoidModel.ArmPose.class, true, (IArmPoseTransformer) TypewriterArmPose::apply
    );

    private TypewriterArmPose() {}

    private static void apply(HumanoidModel<?> model, LivingEntity entity, HumanoidArm arm) {
        float time = entity.tickCount + Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false);
        float rightTap = Math.max(0.0F, Mth.sin(time * TAP_SPEED)) * TAP_AMOUNT;
        float leftTap = Math.max(0.0F, Mth.sin(time * TAP_SPEED + Mth.PI * 0.7F)) * TAP_AMOUNT;

        model.rightArm.xRot = ARM_PITCH + rightTap;
        model.rightArm.yRot = -ARM_INWARD;
        model.rightArm.zRot = 0.0F;
        model.leftArm.xRot = ARM_PITCH + leftTap;
        model.leftArm.yRot = ARM_INWARD;
        model.leftArm.zRot = 0.0F;
    }

    public static HumanoidModel.ArmPose get() {
        return TYPING.getValue();
    }
}
