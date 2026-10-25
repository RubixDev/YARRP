package de.rubixdev.yarrp

import net.fabricmc.api.ModInitializer

class Mod : ModInitializer {
    override fun onInitialize() {
        LOGGER.info("Loading $MOD_ID")
    }
}
