package de.rubixdev.yarrp

import de.rubixdev.yarrp.api.PackPosition
import de.rubixdev.yarrp.api.RuntimeResourcePack
import de.rubixdev.yarrp.api.YarrpCallbacks
import net.fabricmc.api.ModInitializer
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.advancements.critereon.PlayerTrigger
import net.minecraft.data.recipes.RecipeCategory
import net.minecraft.data.recipes.ShapelessRecipeBuilder
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.packs.PackType
import net.minecraft.world.item.Items

class Mod : ModInitializer {
    //#if MC >= 12101
    private val String.id get() = ResourceLocation.fromNamespaceAndPath("yarrp", this)
    //#else
    //$$ private val String.id: ResourceLocation get() = ResourceLocation("yarrp", this)
    //#endif

    private fun makeTestPack(i: Int) = RuntimeResourcePack(
        RuntimeResourcePack.createInfo(
            "test_pack_$i".id,
            Component.literal("Test Pack $i"),
            FabricLoader.getInstance().getModContainer(MOD_ID).orElse(null)?.metadata?.version?.friendlyString!!,
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

    override fun onInitialize() {
        LOGGER.info("Loading $MOD_ID")

        val loader = FabricLoader.getInstance()
        if (loader.isDevelopmentEnvironment && loader.configDir.resolve("yarrp-test").toFile().exists()) {
            val pack1 = makeTestPack(1)
            val pack2 = makeTestPack(2)
            val pack3 = makeTestPack(3)
            val pack4 = makeTestPack(4)
            val pack5 = makeTestPack(5)
            val pack6 = makeTestPack(6)
            val pack7 = makeTestPack(7)
            YarrpCallbacks.register(PackPosition.BEFORE_ALL, PackType.SERVER_DATA) { add(pack1) }
            YarrpCallbacks.register(PackPosition.BEFORE_ALL, PackType.SERVER_DATA) { add(pack2) }
            YarrpCallbacks.register(PackPosition.AFTER_VANILLA, PackType.SERVER_DATA) { add(pack3) }
            YarrpCallbacks.register(PackPosition.AFTER_VANILLA, PackType.SERVER_DATA) { add(pack4) }
            YarrpCallbacks.register(PackPosition.BEFORE_USER, PackType.SERVER_DATA) { add(pack5) }
            YarrpCallbacks.register(PackPosition.AFTER_USER, PackType.SERVER_DATA) { add(pack6) }
            YarrpCallbacks.register(PackPosition.AFTER_ALL, PackType.SERVER_DATA) { add(pack7) }
        }
    }
}
