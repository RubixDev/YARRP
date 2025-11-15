package de.rubixdev.yarrp.api

import net.minecraft.server.packs.PackResources
import org.jetbrains.annotations.ApiStatus

internal typealias PackAdder = (PackResources) -> Unit
private typealias PackCallback = (PackAdder) -> Unit

object YarrpCallbacks {
    private val callbacks = mutableMapOf<PackPosition, MutableList<PackCallback>>()

    /**
     * Register callbacks for adding [RuntimeResourcePack]s.
     *
     * @param[pos] where to place the packs
     * @param[callbacks] the callbacks to register
     */
    @JvmStatic
    fun register(pos: PackPosition, vararg callbacks: PackCallback) {
        callbacks.forEach(this.callbacks.getOrPut(pos, ::mutableListOf)::add)
    }

    /**
     * Register a callback for adding [RuntimeResourcePack]s using the [PackAdderDsl].
     *
     * @param[pos] where to place the packs
     * @param[callback] the callback to register
     */
    inline fun register(pos: PackPosition, crossinline callback: PackAdderDsl.() -> Unit) {
        register(pos, { adder -> PackAdderDsl(adder).callback() })
    }

    /**
     * @suppress Not part of the public API
     */
    @JvmStatic
    @ApiStatus.Internal
    fun run(pos: PackPosition, adder: PackAdder) {
        callbacks[pos]?.forEach { it(adder) }
    }
}
