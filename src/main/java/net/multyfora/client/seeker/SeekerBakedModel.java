package net.multyfora.client.seeker;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.ChunkRenderTypeSet;
import net.neoforged.neoforge.client.model.IDynamicBakedModel;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.ModelProperty;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class SeekerBakedModel implements IDynamicBakedModel {

    public static final ModelProperty<Integer> TEXTURE_VARIANT = new ModelProperty<>();

    private static final int DEFAULT = 0;
    private static final int ACTIVE = 1;
    private static final int D2    = 2;
    private static final int D2_ACTIVE = 3;

    private final BakedModel defaultModel;
    private final BakedModel activeModel;
    private final BakedModel d2Model;
    private final BakedModel d2ActiveModel;

    private final boolean hasAmbientOcclusion;
    private final boolean isGui3d;
    private final boolean usesBlockLight;
    private final TextureAtlasSprite particleIcon;
    private final ItemOverrides overrides;
    private final ItemTransforms transforms;

    public SeekerBakedModel(
            BakedModel defaultModel, BakedModel activeModel,
            BakedModel d2Model, BakedModel d2ActiveModel,
            BakedModel originalVariant
    ) {
        this.defaultModel = defaultModel;
        this.activeModel = activeModel;
        this.d2Model = d2Model;
        this.d2ActiveModel = d2ActiveModel;

        this.hasAmbientOcclusion = originalVariant.useAmbientOcclusion();
        this.isGui3d = originalVariant.isGui3d();
        this.usesBlockLight = originalVariant.usesBlockLight();
        this.particleIcon = originalVariant.getParticleIcon();
        this.overrides = originalVariant.getOverrides();
        this.transforms = originalVariant.getTransforms();
    }

    @Override
    public List<BakedQuad> getQuads(
            @Nullable BlockState state, @Nullable Direction side,
            RandomSource rand, ModelData data, @Nullable RenderType type
    ) {
        Integer variant = data.get(TEXTURE_VARIANT);
        BakedModel selected = switch (variant == null ? DEFAULT : variant) {
            case ACTIVE -> activeModel;
            case D2 -> d2Model;
            case D2_ACTIVE -> d2ActiveModel;
            default -> defaultModel;
        };
        return selected.getQuads(state, side, rand);
    }

    @Override
    public boolean useAmbientOcclusion() { return hasAmbientOcclusion; }

    @Override
    public boolean isGui3d() { return isGui3d; }

    @Override
    public boolean usesBlockLight() { return usesBlockLight; }

    @Override
    public boolean isCustomRenderer() { return false; }

    @Override
    public TextureAtlasSprite getParticleIcon() { return particleIcon; }

    @Override
    public TextureAtlasSprite getParticleIcon(ModelData data) { return particleIcon; }

    @Override
    public ItemOverrides getOverrides() { return overrides; }

    @Override
    public ItemTransforms getTransforms() { return transforms; }

    @Override
    public ChunkRenderTypeSet getRenderTypes(
            @Nullable BlockState state, RandomSource rand, ModelData data
    ) {
        return ChunkRenderTypeSet.of(RenderType.cutoutMipped());
    }
}
