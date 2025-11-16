package de.rubixdev.yarrp.api

/**
 * An insertion position in the final resource pack list.
 */
enum class PackPosition {
    /**
     * Registered before all other packs.
     *
     * Even the vanilla pack can override resources of the packs added here.
     */
    BEFORE_ALL,

    /**
     * Registered after all other packs.
     *
     * Packs can override everything.
     */
    AFTER_ALL,

    /**
     * Registered after the vanilla pack.
     *
     * Packs can override vanilla resources, but not user provided packs.
     */
    AFTER_VANILLA,

    /**
     * Registered together with user provided packs using [Pack.Position.BOTTOM][net.minecraft.server.packs.repository.Pack.Position.BOTTOM]
     * for insertion.
     *
     * **The packs registered here are also visible to users and can be disabled.**
     */
    BEFORE_USER,

    /**
     * Registered together with user provided packs using [Pack.Position.TOP][net.minecraft.server.packs.repository.Pack.Position.TOP]
     * for insertion.
     *
     * **The packs registered here are also visible to users and can be disabled.**
     */
    AFTER_USER,
}
