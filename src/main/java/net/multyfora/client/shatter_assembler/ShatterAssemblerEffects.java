package net.multyfora.client.shatter_assembler;

import net.minecraft.client.Minecraft;
import net.minecraft.client.ParticleStatus;
import net.minecraft.client.particle.Particle;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.multyfora.content.shatter_assembler.ShatterAssemblerBlock;
import net.multyfora.content.shatter_assembler.ShatterAssemblerBlockEntity;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public class ShatterAssemblerEffects {
    private static final int DEBRIS_PER_SIDE = 3;

    public static void spawnSawDebris(ShatterAssemblerBlockEntity be) {
        Minecraft mc = Minecraft.getInstance();
        Level level = be.getLevel();
        if (level == null || mc.options.particles().get() == ParticleStatus.MINIMAL) return;

        BlockState state = be.getBlockState();
        BlockPos pos = be.getBlockPos();
        BlockState ground = level.getBlockState(pos.relative(ShatterAssemblerBlock.getStickyFacing(state)));
        if (ground.isAir() || ground.getRenderShape() == RenderShape.INVISIBLE) return;

        Matrix4f toBlock = ShatterAssemblerBlock.localToBlockSpace(state);
        RandomSource random = level.random;
        BlockParticleOption option = new BlockParticleOption(ParticleTypes.BLOCK, ground);
        int count = mc.options.particles().get() == ParticleStatus.DECREASED ? 1 : DEBRIS_PER_SIDE;
        float[] rowZ = ShatterAssemblerBlockEntity.SAW_ROW_Z;

        for (int side = -1; side <= 1; side += 2) {
            for (int i = 0; i < count; i++) {
                float z = rowZ[random.nextInt(rowZ.length)];
                Vector3f contact = new Vector3f(
                        ShatterAssemblerBlockEntity.getSawCenterX(side) + side * random.nextFloat() * 0.1F,
                        ShatterAssemblerBlockEntity.SAW_CENTER_Y - ShatterAssemblerBlockEntity.SAW_RADIUS + 0.05F,
                        z + (random.nextFloat() - 0.5F) * 0.06F);
                Vector3f velocity = new Vector3f(
                        side * (0.08F + random.nextFloat() * 0.2F),
                        0.1F + random.nextFloat() * 0.15F,
                        (random.nextFloat() - 0.5F) * 0.08F);
                toBlock.transformPosition(contact);
                toBlock.transformDirection(velocity);

                Particle particle = mc.particleEngine.createParticle(option,
                        pos.getX() + contact.x, pos.getY() + contact.y, pos.getZ() + contact.z, 0, 0, 0);
                if (particle != null) {
                    particle.setParticleSpeed(velocity.x, velocity.y, velocity.z);
                    particle.scale(0.6F + random.nextFloat() * 0.4F);
                }
            }
        }
    }
}
