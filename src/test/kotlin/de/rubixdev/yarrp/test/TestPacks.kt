package de.rubixdev.yarrp.test

import de.rubixdev.yarrp.LOGGER
import de.rubixdev.yarrp.api.PackPosition
import de.rubixdev.yarrp.api.RuntimeResourcePack
import de.rubixdev.yarrp.api.YarrpCallbacks
import net.minecraft.advancements.critereon.PlayerTrigger
import net.minecraft.data.recipes.RecipeCategory
import net.minecraft.data.recipes.ShapelessRecipeBuilder
import net.minecraft.network.chat.Component
import net.minecraft.server.packs.PackType
import net.minecraft.world.item.Items

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
    }
}
