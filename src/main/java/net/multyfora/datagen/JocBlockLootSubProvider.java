package net.multyfora.datagen;

import net.minecraft.advancements.critereon.StatePropertiesPredicate;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.predicates.LootItemBlockStatePropertyCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;

import net.multyfora.content.gyroseat.GyroscopicSeatBlock;
import net.multyfora.index.JocBlocks;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class JocBlockLootSubProvider extends BlockLootSubProvider {

    protected JocBlockLootSubProvider(HolderLookup.Provider registries) {
        super(Set.of(), FeatureFlags.REGISTRY.allFlags(), registries);
    }

    @Override
    protected void generate() {
        for (DyeColor color : DyeColor.values()) {
            dropSelf(JocBlocks.BALLOONS.get(color).get());
        }
        for (DyeColor color : DyeColor.values()) {
            dropSelf(JocBlocks.SYMMETRIC_CROSS_SAILS.get(color).get());
        }
        dropSelf(JocBlocks.SEEKER.get());
        dropSelf(JocBlocks.SHATTER_ASSEMBLER.get());
        Block gyroscopicSeat = JocBlocks.GYROSCOPIC_SEAT.get();
        add(gyroscopicSeat, LootTable.lootTable().withPool(applyExplosionCondition(gyroscopicSeat,
                LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1.0F))
                        .add(LootItem.lootTableItem(gyroscopicSeat)
                                .when(LootItemBlockStatePropertyCondition.hasBlockStateProperties(gyroscopicSeat)
                                        .setProperties(StatePropertiesPredicate.Builder.properties()
                                                .hasProperty(GyroscopicSeatBlock.PART, 0)))))));
    }

    @Override
    protected Iterable<Block> getKnownBlocks() {
        List<Block> blocks = new ArrayList<>();
        for (DyeColor color : DyeColor.values()) {
            blocks.add(JocBlocks.BALLOONS.get(color).get());
        }
        for (DyeColor color : DyeColor.values()) {
            blocks.add(JocBlocks.SYMMETRIC_CROSS_SAILS.get(color).get());
        }
        blocks.add(JocBlocks.SEEKER.get());
        blocks.add(JocBlocks.SHATTER_ASSEMBLER.get());
        blocks.add(JocBlocks.GYROSCOPIC_SEAT.get());
        return blocks;
    }
}