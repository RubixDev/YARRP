package de.rubixdev.yarrp.test

import de.rubixdev.yarrp.LOGGER
import de.rubixdev.yarrp.MOD_ID
import net.fabricmc.api.ModInitializer
import net.fabricmc.loader.api.FabricLoader

class TestMod : ModInitializer {
    override fun onInitialize() {
        LOGGER.info("initializing test mod")
        TestPacks(FabricLoader.getInstance().getModContainer(MOD_ID).orElseThrow().metadata.version.friendlyString)
    }
}
