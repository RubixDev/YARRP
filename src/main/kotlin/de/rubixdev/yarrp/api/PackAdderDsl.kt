package de.rubixdev.yarrp.api

import net.minecraft.server.packs.PackResources

class PackAdderDsl(private val adder: PackAdder) {
    fun add(vararg packs: PackResources) {
        packs.forEach(adder)
    }

    fun addAll(pack: Collection<PackResources>) {
        pack.forEach(adder)
    }
}
