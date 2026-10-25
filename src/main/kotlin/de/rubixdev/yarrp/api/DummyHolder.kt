package de.rubixdev.yarrp.api

import com.mojang.datafixers.util.Either
import java.util.Optional
import java.util.function.Predicate
import java.util.stream.Stream
import kotlin.jvm.optionals.getOrNull
import net.minecraft.core.Holder
import net.minecraft.core.HolderOwner
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import net.minecraft.tags.TagKey

/**
 * A dummy implementation of [Holder] to help with [RuntimeResourcePack] creation.
 *
 * Since Minecraft 1.21.5, the armor trim smithing recipe builder takes an additional argument
 * for the actual pattern to apply, but in the form of a [Holder], which is only really handed
 * out by registries. We cannot really access the vanilla registries during RRP serialization
 * though, but we also don't actually need to, since only the [ResourceKey] is actually
 * required for serialization. This type allows us to do precisely that, acting as a [Holder]
 * without a value behind the key.
 */
class DummyHolder<T>(private val key: ResourceKey<T>) : Holder<T> {
    override fun value(): T? = null
    override fun isBound(): Boolean = true
    override fun `is`(location: ResourceLocation): Boolean = false
    override fun `is`(resourceKey: ResourceKey<T>): Boolean = key == resourceKey
    override fun `is`(predicate: Predicate<ResourceKey<T>>): Boolean = predicate.test(key)
    override fun `is`(tagKey: TagKey<T>): Boolean = false

    @Deprecated("Deprecated in Java")
    override fun `is`(holder: Holder<T>): Boolean = holder.unwrapKey().getOrNull()?.let { it == key } ?: false

    override fun tags(): Stream<TagKey<T>> = Stream.of()
    override fun unwrap(): Either<ResourceKey<T>, T> = Either.left(key)
    override fun unwrapKey(): Optional<ResourceKey<T>> = Optional.of(key)
    override fun kind(): Holder.Kind = Holder.Kind.DIRECT
    override fun canSerializeIn(owner: HolderOwner<T>): Boolean = true
}
