package de.rubixdev.yarrp.api

import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import net.minecraft.tags.TagEntry
import net.minecraft.tags.TagFile
import net.minecraft.tags.TagKey

/**
 * A type-safe tag builder for creating a [TagFile].
 *
 * Unlike the vanilla [net.minecraft.tags.TagBuilder], this one also has a [setReplace]
 * method to set the value of the `replace` field in the resulting [TagFile].
 * It also has additional type-safe methods which enforce the correct type of entries to
 * be added.
 */
open class TagBuilder<T> {
    private val builder = net.minecraft.tags.TagBuilder()

    /**
     * The current value of the `replace` field.
     */
    var replace = false

    /**
     * Set the value of the `replace` field.
     *
     * This controls, whether the contents of this tag should fully replace tag contents
     * from lower priority data packs. The default value is `false`.
     *
     * @param[replace] the new value
     * @return this builder
     */
    fun setReplace(replace: Boolean) = also { this.replace = replace }

    /**
     * Add a [TagEntry] to this tag.
     *
     * @param[entry] the tag entry
     * @return this builder
     */
    fun add(entry: TagEntry) = also { builder.add(entry) }

    /**
     * Add an element to this tag.
     *
     * @param[id] the resource location to add
     * @return this builder
     */
    fun add(id: ResourceLocation) = also { builder.addElement(id) }

    /**
     * Add an element to this tag.
     *
     * @param[key] the resource key to add
     * @return this builder
     */
    fun add(key: ResourceKey<T>) = also { add(key.location()) }

    /**
     * Add an optional element to this tag.
     *
     * @param[id] the resource location to add
     * @return this builder
     */
    fun addOptional(id: ResourceLocation) = also { builder.addOptionalElement(id) }

    /**
     * Add an optional element to this tag.
     *
     * @param[key] the resource key to add
     * @return this builder
     */
    fun addOptional(key: ResourceKey<T>) = also { addOptional(key.location()) }

    /**
     * Add another tag to this tag.
     *
     * @param[id] the resource location of the other tag
     * @return this builder
     */
    fun addTag(id: ResourceLocation) = also { builder.addTag(id) }

    /**
     * Add another tag to this tag.
     *
     * @param[key] the tag to add
     * @return this builder
     */
    fun addTag(key: TagKey<T>) = also { addTag(key.location) }

    /**
     * Add another optional tag to this tag.
     *
     * @param[id] the resource location of the other tag
     * @return this builder
     */
    fun addOptionalTag(id: ResourceLocation) = also { builder.addOptionalTag(id) }

    /**
     * Add another optional tag to this tag.
     *
     * @param[key] the tag to add
     * @return this builder
     */
    fun addOptionalTag(key: TagKey<T>) = also { addOptionalTag(key.location) }

    /**
     * Build the final [TagFile]
     *
     * @return the resulting [TagFile]
     */
    fun build() = TagFile(builder.build(), replace)
}
