package net.multyfora.index;

import net.minecraft.core.BlockPos;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.core.UUIDUtil;

import org.jetbrains.annotations.Nullable;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Optional;
import java.util.UUID;

public record SeekerCapturedTarget(BlockPos pos, @Nullable UUID subLevelId, double localX, double localY, double localZ) {

    public static final Codec<SeekerCapturedTarget> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        BlockPos.CODEC.fieldOf("pos").forGetter(SeekerCapturedTarget::pos),
        UUIDUtil.CODEC.optionalFieldOf("sub_level").forGetter(t -> Optional.ofNullable(t.subLevelId())),
        Codec.DOUBLE.optionalFieldOf("local_x", 0.0).forGetter(SeekerCapturedTarget::localX),
        Codec.DOUBLE.optionalFieldOf("local_y", 0.0).forGetter(SeekerCapturedTarget::localY),
        Codec.DOUBLE.optionalFieldOf("local_z", 0.0).forGetter(SeekerCapturedTarget::localZ)
    ).apply(instance, (pos, uuidOpt, lx, ly, lz) -> new SeekerCapturedTarget(pos, uuidOpt.orElse(null), lx, ly, lz)));

    public static final StreamCodec<ByteBuf, SeekerCapturedTarget> STREAM_CODEC = StreamCodec.composite(
        BlockPos.STREAM_CODEC, SeekerCapturedTarget::pos,
        ByteBufCodecs.optional(ByteBufCodecs.fromCodec(UUIDUtil.CODEC)), t -> Optional.ofNullable(t.subLevelId()),
        ByteBufCodecs.DOUBLE, SeekerCapturedTarget::localX,
        ByteBufCodecs.DOUBLE, SeekerCapturedTarget::localY,
        ByteBufCodecs.DOUBLE, SeekerCapturedTarget::localZ,
        (pos, uuidOpt, lx, ly, lz) -> new SeekerCapturedTarget(pos, uuidOpt.orElse(null), lx, ly, lz)
    );
}
