package de.rubixdev.yarrp.api

import net.minecraft.resources.ResourceKey
import net.minecraft.world.entity.EntityType
import net.minecraft.world.item.Item
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.material.Fluid

/**
 * An extension of [TagBuilder] which can add elements directly, given a resource key extractor function.
 */
class IntrinsicHolderTagBuilder<T>(private val keyExtractor: (T) -> ResourceKey<T>) : TagBuilder<T>() {
    companion object {
        /**
         * Create a tag builder for [EntityType]s.
         */
        @Suppress("DEPRECATION")
        @JvmStatic
        fun entityType() = IntrinsicHolderTagBuilder<EntityType<*>> { it.builtInRegistryHolder().key() }

        /**
         * Create a tag builder for [Fluid]s.
         */
        @Suppress("DEPRECATION")
        @JvmStatic
        fun fluid() = IntrinsicHolderTagBuilder<Fluid> { it.builtInRegistryHolder().key() }

        /**
         * Create a tag builder for [Item]s.
         */
        @Suppress("DEPRECATION")
        @JvmStatic
        fun item() = IntrinsicHolderTagBuilder<Item> { it.builtInRegistryHolder().key() }

        /**
         * Create a tag builder for [Block]s.
         */
        @Suppress("DEPRECATION")
        @JvmStatic
        fun block() = IntrinsicHolderTagBuilder<Block> { it.builtInRegistryHolder().key() }
    }

    /**
     * Add an element to this tag.
     *
     * @param[element] the element to add
     * @return this builder
     */
    fun add(element: T) = also { add(keyExtractor(element)) }

    /**
     * Add an optional element to this tag.
     *
     * @param[element] the element to add
     * @return this builder
     */
    fun addOptional(element: T) = also { addOptional(keyExtractor(element)) }
}
