package de.rubixdev.yarrp.api

import de.rubixdev.yarrp.LOGGER
import net.minecraft.server.packs.PackResources
import net.minecraft.server.packs.PackType
import org.jetbrains.annotations.ApiStatus

internal typealias PackAdder = (PackResources) -> Unit
private typealias PackCallback = (PackAdder) -> Unit

object YarrpCallbacks {
    private val callbacks = mutableMapOf<Pair<PackPosition, PackType>, MutableList<PackCallback>>()

    /**
     * Register callbacks for adding [RuntimeResourcePack]s.
     *
     * @param[pos] where to place the packs
     * @param[type] the type of the packs to add
     * @param[callbacks] the callbacks to register
     */
    @JvmStatic
    fun register(pos: PackPosition, type: PackType, vararg callbacks: PackCallback) {
        callbacks.forEach(this.callbacks.getOrPut(pos to type, ::mutableListOf)::add)
    }

    /**
     * Register a callback for adding [RuntimeResourcePack]s using the [PackAdderDsl].
     *
     * @param[pos] where to place the packs
     * @param[type] the type of the packs to add
     * @param[callback] the callback to register
     */
    inline fun register(pos: PackPosition, type: PackType, crossinline callback: PackAdderDsl.() -> Unit) {
        register(pos, type, { adder -> PackAdderDsl(adder).callback() })
    }

    /**
     * @suppress Not part of the public API
     */
    @JvmStatic
    @ApiStatus.Internal
    fun run(pos: PackPosition, type: PackType, adder: PackAdder) {
        callbacks[pos to type]?.forEach {
            it { pack ->
                LOGGER.debug("adding pack '{}' with known pack info: {}", pack.packId(), pack.knownPackInfo())
                adder(pack)
            }
        }
    }
}
