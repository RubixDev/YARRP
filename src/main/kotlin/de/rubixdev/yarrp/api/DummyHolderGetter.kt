package de.rubixdev.yarrp.api

import java.util.Optional
import net.minecraft.core.Holder
import net.minecraft.core.HolderGetter
import net.minecraft.core.HolderSet
import net.minecraft.resources.ResourceKey
import net.minecraft.tags.TagKey

/**
 * A dummy implementation of [HolderGetter] to help with [RuntimeResourcePack] creation.
 *
 * Since Minecraft 1.21.3, the various recipe builders all expect some [HolderGetter] in
 * order to look up tags in the registry. For the purposes of creating a [RuntimeResourcePack],
 * we don't actually need a registry though; simply storing the tag is sufficient. This
 * implementation allows us to do that.
 */
class DummyHolderGetter<T> : HolderGetter<T> {
    override fun get(resourceKey: ResourceKey<T>): Optional<Holder.Reference<T>> =
        throw RuntimeException("called get with ResourceKey on DummyHolderGetter")

    override fun get(tagKey: TagKey<T>): Optional<HolderSet.Named<T>> = Optional.of(DummyHolderSet(tagKey))
}
