package de.rubixdev.yarrp.test;

import de.rubixdev.yarrp.ModConstants;
import de.rubixdev.yarrp.api.IntrinsicHolderTagBuilder;
import de.rubixdev.yarrp.api.PackPosition;
import de.rubixdev.yarrp.api.RuntimeResourcePack;
import de.rubixdev.yarrp.api.YarrpCallbacks;
import net.minecraft.advancements.critereon.PlayerTrigger;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackType;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;

public class JavaTestPacks {
    public JavaTestPacks(String modVersion) {
        ModConstants.LOGGER.info("creating java test packs");

        @SuppressWarnings("resource")
        var pack = new RuntimeResourcePack(
            RuntimeResourcePack
                .createInfo(UtilsKt.getId("test_pack_java"), Component.literal("Test Pack (Java)"), modVersion),
            RuntimeResourcePack.createMetadata(Component.literal("Description"))
        );

        YarrpCallbacks.javaRegister(PackPosition.AFTER_USER, PackType.SERVER_DATA, adder -> adder.accept(pack));

        pack.addTag(EntityTypeTags.FROG_FOOD, IntrinsicHolderTagBuilder.entityType().add(EntityType.SQUID));
        pack.addTag(FluidTags.LAVA, IntrinsicHolderTagBuilder.fluid().add(Fluids.WATER));
        pack.addTag(
            ItemTags.BEACON_PAYMENT_ITEMS,
            IntrinsicHolderTagBuilder.item().add(Items.STICK).addTag(ItemTags.CANDLES)
        );
        pack.addTag(BlockTags.GUARDED_BY_PIGLINS, IntrinsicHolderTagBuilder.block().add(Blocks.NETHERRACK));

        pack.addRecipeAndAdvancement(
            UtilsKt.getId("trust_me_bro"),
            ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, Items.DIRT)
                .unlockedBy("tick", PlayerTrigger.TriggerInstance.tick())
                .pattern("###")
                .pattern("###")
                .pattern("###")
                .define('#', Items.NETHERITE_BLOCK)
        );
    }
}
