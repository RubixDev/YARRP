package de.rubixdev.yarrp.api

import net.minecraft.server.packs.PackResources

/**
 * A DSL for adding packs with a [PackAdder].
 */
class PackAdderDsl(private val adder: PackAdder) {
    /**
     * Add one or more packs.
     *
     * @param[packs] the packs to add
     */
    fun add(vararg packs: PackResources) {
        packs.forEach(adder)
    }

    /**
     * Add a collection of packs.
     *
     * @param[packs] the packs to add
     */
    fun addAll(packs: Collection<PackResources>) {
        packs.forEach(adder)
    }
}
