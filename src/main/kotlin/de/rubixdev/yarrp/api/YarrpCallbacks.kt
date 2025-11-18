package de.rubixdev.yarrp.api

import de.rubixdev.yarrp.LOGGER
import java.util.function.Consumer
import net.minecraft.server.packs.PackLocationInfo
import net.minecraft.server.packs.PackResources
import net.minecraft.server.packs.PackSelectionConfig
import net.minecraft.server.packs.PackType
import net.minecraft.server.packs.repository.Pack
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
    @JvmStatic
    inline fun register(pos: PackPosition, type: PackType, crossinline callback: PackAdderDsl.() -> Unit) {
        register(pos, type, { adder -> PackAdderDsl(adder).callback() })
    }

    /**
     * Register a callback for adding [RuntimeResourcePack]s using Java [Consumer]s.
     *
     * @param[pos] where to place the packs
     * @param[type] the type of the packs to add
     * @param[callback] the callback to register
     */
    @JvmStatic
    fun javaRegister(pos: PackPosition, type: PackType, callback: Consumer<Consumer<PackResources>>) {
        register(pos, type, { adder -> callback.accept(adder) })
    }

    /** @suppress not part of the public API */
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

    /** @suppress not part of the public API */
    @JvmStatic
    @ApiStatus.Internal
    fun logPackList(packs: Collection<PackResources>) {
        LOGGER.debug("Full list of packs is now:{}", packs.joinToString("\n- ", "\n- ") { it.packId() })
    }

    /** @suppress not part of the public API */
    @JvmStatic
    @ApiStatus.Internal
    fun wrapPackConsumer(consumer: Consumer<Pack>, type: PackType, position: Pack.Position): PackAdder = { pack ->
        Pack.readMetaAndCreate(
            pack.location(),
            object : Pack.ResourcesSupplier {
                override fun openPrimary(location: PackLocationInfo) = pack
                override fun openFull(location: PackLocationInfo, metadata: Pack.Metadata) = pack
            },
            type,
            PackSelectionConfig(false, position, false),
        )?.let(consumer::accept)
    }

    /** @suppress not part of the public API */
    @JvmStatic
    @ApiStatus.Internal
    fun addAfterVanilla(packs: MutableList<PackResources>, type: PackType) {
        val vanillaIdx = packs.indexOfFirst { it.packId() == "vanilla" }
        if (vanillaIdx != -1) {
            LOGGER.debug("Registering AFTER_VANILLA packs with type {}", type)
            val newPacks = mutableListOf<PackResources>()
            run(PackPosition.AFTER_VANILLA, type, newPacks::add)
            packs.addAll(vanillaIdx + 1, newPacks)
        }
    }
}
