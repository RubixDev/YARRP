package de.rubixdev.yarrp.api

import com.mojang.datafixers.util.Either
import java.util.Optional
import java.util.stream.Stream
import net.minecraft.core.Holder
import net.minecraft.core.HolderOwner
import net.minecraft.core.HolderSet
import net.minecraft.tags.TagKey
import net.minecraft.util.RandomSource

/**
 * A dummy implementation of [HolderSet] to help with [RuntimeResourcePack] creation.
 *
 * In some instances, Minecraft's resource builders expect a [HolderSet] for specifying
 * a collection of e.g. items. When specifying this collection simply by a [TagKey], these
 * must usually be obtained from a [net.minecraft.core.Registry]. But when creating a
 * [RuntimeResourcePack], the only information we need is the actual [TagKey] itself,
 * which will be serialized to a string beforehand anyway. This class is used for exactly
 * that. It allows us to create a [HolderSet] from a [TagKey] without a [net.minecraft.core.Registry],
 * and it always serializes to just the tag.
 */
class DummyHolderSet<T>(val tag: TagKey<T>) : HolderSet.Named<T>(null, tag) {
    override fun stream(): Stream<Holder<T>> = Stream.empty()
    override fun size(): Int = 0
    override fun unwrap(): Either<TagKey<T>, List<Holder<T>>> = Either.left(tag)
    override fun getRandomElement(random: RandomSource): Optional<Holder<T>> = Optional.empty()
    override fun get(index: Int): Holder<T>? = null
    override fun contains(entry: Holder<T>): Boolean = false
    override fun canSerializeIn(owner: HolderOwner<T>): Boolean = true
    override fun unwrapKey(): Optional<TagKey<T>> = Optional.of(tag)
    override fun iterator(): MutableIterator<Holder<T>> = mutableListOf<Holder<T>>().iterator()
}
