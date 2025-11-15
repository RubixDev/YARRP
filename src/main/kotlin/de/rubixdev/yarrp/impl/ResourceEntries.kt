package de.rubixdev.yarrp.impl

import java.io.InputStream
import net.minecraft.server.packs.resources.IoSupplier

internal typealias Resource = IoSupplier<InputStream>
internal typealias DirectoryMap = MutableMap<String, PackEntry>
internal data class ResourceEntry(val resource: Resource) : PackEntry()
internal data class DirectoryEntry(val nested: DirectoryMap = mutableMapOf()) : PackEntry()

internal sealed class PackEntry {
    fun asResource(): Resource? = (this as? ResourceEntry)?.resource
    fun asDirectory(): DirectoryMap? = (this as? DirectoryEntry)?.nested

    fun find(path: List<String>): PackEntry? = path.fold(this as PackEntry?) { entry, segment ->
        entry?.asDirectory()?.get(segment)
    }

    fun findAllResources(
        path: List<String> = listOf(),
        consumer: (path: List<String>, resource: Resource) -> Unit,
    ): Unit = asDirectory()?.entries?.forEach { (name, entry) ->
        when (entry) {
            is ResourceEntry -> consumer(path + name, entry.resource)
            is DirectoryEntry -> entry.findAllResources(path + name, consumer)
        }
    } ?: Unit
}
