package de.rubixdev.yarrp.api

/**
 * An insertion position in the final resource pack list.
 */
enum class PackPosition {
    /**
     * Registered before the vanilla pack.
     *
     * The vanilla pack can override data of the packs added here.
     */
    BEFORE_VANILLA,

    /**
     * Registered after the vanilla pack.
     *
     * Packs can override vanilla data, but not user-added packs.
     */
    AFTER_VANILLA,
}
