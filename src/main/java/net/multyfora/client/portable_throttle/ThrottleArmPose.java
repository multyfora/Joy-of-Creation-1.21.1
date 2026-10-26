package net.multyfora.client.portable_throttle;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.fml.common.asm.enumextension.EnumProxy;
import net.neoforged.neoforge.client.IArmPoseTransformer;

public final class ThrottleArmPose {
    private static final float HOLD_PITCH = -0.95F;
    private static final float HOLD_INWARD = 0.15F;
    private static final float REACH_PITCH = -1.0F;
    private static final float REACH_RAISE = 0.35F;
    private static final float REACH_ACROSS = 0.6F;

    public static final EnumProxy<HumanoidModel.ArmPose> SLIDING = new EnumProxy<>(
        HumanoidModel.ArmPose.class, true, (IArmPoseTransformer) ThrottleArmPose::apply
    );

    private ThrottleArmPose() {}

    private static void apply(HumanoidModel<?> model, LivingEntity entity, HumanoidArm arm) {
        boolean right = arm == HumanoidArm.RIGHT;
        ModelPart holding = right ? model.rightArm : model.leftArm;
        ModelPart reaching = right ? model.leftArm : model.rightArm;
        float side = right ? 1.0F : -1.0F;
        float knob = ThrottleVisualState.getKnob(entity);

        holding.xRot = HOLD_PITCH + model.head.xRot * 0.4F;
        holding.yRot = -side * HOLD_INWARD;
        holding.zRot = 0.0F;
        reaching.xRot = REACH_PITCH - knob * REACH_RAISE + model.head.xRot * 0.4F;
        reaching.yRot = side * REACH_ACROSS;
        reaching.zRot = 0.0F;
    }

    public static HumanoidModel.ArmPose get() {
        return SLIDING.getValue();
    }
}
