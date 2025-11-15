package de.rubixdev.yarrp.api

import net.minecraft.server.packs.PackResources
import org.jetbrains.annotations.ApiStatus

internal typealias PackAdder = (PackResources) -> Unit
private typealias PackCallback = (PackAdder) -> Unit

object YarrpCallbacks {
    private val callbacks = mutableMapOf<RegistrationTime, MutableList<PackCallback>>()

    @JvmStatic
    fun register(time: RegistrationTime, vararg callbacks: PackCallback) {
        callbacks.forEach(this.callbacks.getOrPut(time, ::mutableListOf)::add)
    }

    fun register(time: RegistrationTime, callback: PackAdderDsl.() -> Unit) {
        register(time, { adder -> PackAdderDsl(adder).callback() })
    }

    @JvmStatic
    @ApiStatus.Internal
    fun run(time: RegistrationTime, adder: PackAdder) {
        callbacks[time]?.forEach { it(adder) }
    }
}
