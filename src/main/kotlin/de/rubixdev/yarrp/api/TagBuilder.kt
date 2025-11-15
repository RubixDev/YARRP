package de.rubixdev.yarrp.api

import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import net.minecraft.tags.TagEntry
import net.minecraft.tags.TagFile
import net.minecraft.tags.TagKey

class TagBuilder<T> {
    private val builder = net.minecraft.tags.TagBuilder()
    private var replace = false

    fun setReplace(replace: Boolean) = also { this.replace = replace }

    fun add(entry: TagEntry) = also { builder.add(entry) }
    fun add(id: ResourceLocation) = also { builder.addElement(id) }
    fun add(key: ResourceKey<T>) = also { builder.addElement(key.location()) }

    fun addOptional(id: ResourceLocation) = also { builder.addOptionalElement(id) }
    fun addOptional(key: ResourceKey<T>) = also { builder.addOptionalElement(key.location()) }

    fun addTag(id: ResourceLocation) = also { builder.addTag(id) }
    fun addTag(key: TagKey<T>) = also { builder.addTag(key.location) }

    fun addOptionalTag(id: ResourceLocation) = also { builder.addOptionalTag(id) }
    fun addOptionalTag(key: TagKey<T>) = also { builder.addOptionalTag(key.location) }

    fun build() = TagFile(builder.build(), replace)
}
