package de.rubixdev.yarrp.test

import de.rubixdev.yarrp.LOGGER
import de.rubixdev.yarrp.api.DummyHolderSet
import de.rubixdev.yarrp.api.PackPosition
import de.rubixdev.yarrp.api.RuntimeResourcePack
import de.rubixdev.yarrp.api.YarrpCallbacks
import net.minecraft.advancements.critereon.PlayerTrigger
import net.minecraft.core.registries.Registries
import net.minecraft.data.recipes.RecipeCategory
import net.minecraft.data.recipes.ShapelessRecipeBuilder
import net.minecraft.data.recipes.SmithingTransformRecipeBuilder
import net.minecraft.data.recipes.SmithingTrimRecipeBuilder
import net.minecraft.data.recipes.SpecialRecipeBuilder
import net.minecraft.network.chat.Component
import net.minecraft.server.packs.PackType
import net.minecraft.tags.BlockTags
import net.minecraft.tags.EnchantmentTags
import net.minecraft.tags.EntityTypeTags
import net.minecraft.tags.FluidTags
import net.minecraft.tags.ItemTags
import net.minecraft.tags.TagEntry
import net.minecraft.tags.TagKey
import net.minecraft.world.entity.EntityType
import net.minecraft.world.item.Items
import net.minecraft.world.item.crafting.ArmorDyeRecipe
import net.minecraft.world.item.crafting.Ingredient
import net.minecraft.world.item.enchantment.Enchantment
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.material.Fluids

@Suppress("unused")
class TestPacks(private val modVersion: String) {
    private fun makeTestPack(i: Int) = RuntimeResourcePack(
        RuntimeResourcePack.createInfo(
            "test_pack_$i".id,
            Component.literal("Test Pack $i"),
            modVersion,
        ),
        RuntimeResourcePack.createMetadata(
            Component.literal("Description of test runtime pack number $i"),
        ),
    ).apply {
        addRecipeAndAdvancement(
            "test_recipe_$i".id,
            ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, Items.DIAMOND)
                .unlockedBy("tick", PlayerTrigger.TriggerInstance.tick())
                .requires(Items.STICK, i),
        )
    }

    init {
        LOGGER.info("creating test packs")
        val pack1 = makeTestPack(1)
        val pack2 = makeTestPack(2)
        val pack3 = makeTestPack(3)
        val pack4 = makeTestPack(4)
        val pack5 = makeTestPack(5)
        val pack6 = makeTestPack(6)
        val pack7 = makeTestPack(7)
        YarrpCallbacks.register(PackPosition.BEFORE_ALL, PackType.SERVER_DATA) { addAll(listOf(pack1, pack2)) }
        YarrpCallbacks.register(PackPosition.AFTER_VANILLA, PackType.SERVER_DATA) { add(pack3, pack4) }
        YarrpCallbacks.register(PackPosition.BEFORE_USER, PackType.SERVER_DATA) { add(pack5) }
        YarrpCallbacks.register(PackPosition.AFTER_USER, PackType.SERVER_DATA) { add(pack6) }
        YarrpCallbacks.register(PackPosition.AFTER_ALL, PackType.SERVER_DATA) { add(pack7) }

        pack7.addEntityTypeTag(EntityTypeTags.ARROWS) { add(EntityType.BAT) }
        pack7.addFluidTag(FluidTags.WATER) { add(Fluids.LAVA) }
        pack7.addItemTag(ItemTags.CAT_FOOD) {
            add(Items.DIAMOND)
            addOptional(Items.NETHERITE_BLOCK)
            addTag(ItemTags.PARROT_POISONOUS_FOOD)
            addOptionalTag(ItemTags.ARROWS)
            add(TagEntry.tag(ItemTags.DIAMOND_ORES.location))
        }
        pack7.addBlockTag(BlockTags.DIAMOND_ORES) { add(Blocks.DIAMOND_BLOCK) }
        val enchantable = pack7.addTag(
            TagKey.create(Registries.ITEM, "enchantable".id),
            listOf(TagEntry.tag(ItemTags.FISHES.location)),
        )

        val genericBuilder = ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, Items.BLUE_ICE)
            .requires(Items.ICE)
            .requires(Items.BLUE_DYE)
            .unlockedBy("tick", PlayerTrigger.TriggerInstance.tick())
        val smithingTransformBuilder = SmithingTransformRecipeBuilder.smithing(
            Ingredient.of(Items.NETHERITE_BLOCK),
            Ingredient.of(Items.STICK),
            Ingredient.of(Items.BLAZE_POWDER),
            RecipeCategory.BREWING,
            Items.BLAZE_ROD,
        ).unlocks("tick", PlayerTrigger.TriggerInstance.tick())
        val smithingTrimBuilder =
            SmithingTrimRecipeBuilder.smithingTrim(
                Ingredient.of(Items.STICK),
                Ingredient.of(Items.STICK),
                Ingredient.of(Items.STICK),
                RecipeCategory.MISC,
            ).unlocks("tick", PlayerTrigger.TriggerInstance.tick())
        val specialBuilder = SpecialRecipeBuilder.special(::ArmorDyeRecipe)

        pack7.addRecipeAndAdvancement("test_generic_1".id, genericBuilder)
        pack7.addRecipeAndAdvancement("test_smithing_transform_1".id, smithingTransformBuilder)
        pack7.addRecipeAndAdvancement("test_smithing_trim_1".id, smithingTrimBuilder)
        pack7.addRecipeAndAdvancement("test_special_1".id, specialBuilder)

        pack7.addRecipe("test_generic_2".id, genericBuilder)
        pack7.addRecipe("test_smithing_transform_2".id, smithingTransformBuilder)
        pack7.addRecipe("test_smithing_trim_2".id, smithingTrimBuilder)
        val testRecipe = pack7.addRecipe("test_special_2".id, specialBuilder)

        pack7.addAdvancement("test_generic_3".id, genericBuilder)
        pack7.addAdvancement("test_smithing_transform_3".id, smithingTransformBuilder)
        pack7.addAdvancement("test_smithing_trim_3".id, smithingTrimBuilder)
        pack7.addAdvancement("test_special_3".id, specialBuilder)

        pack7.addAdvancement("test".id, pack7.advancementBuilderForRecipe(testRecipe))

        //#if MC >= 12101
        val testEnchantment = pack7.addEnchantment(
            "test".id,
            Enchantment.definition(
                DummyHolderSet(enchantable),
                1,
                1,
                Enchantment.constantCost(1),
                Enchantment.constantCost(1),
                1,
            ),
        )
        pack7.addTag(EnchantmentTags.TREASURE) { add(testEnchantment) }
        //#endif

        JavaTestPacks(modVersion)
    }
}
