package de.rubixdev.yarrp.impl

import com.mojang.serialization.DynamicOps
import de.rubixdev.yarrp.api.DummyHolder
import java.util.Optional
import net.minecraft.core.HolderOwner
import net.minecraft.core.Registry
import net.minecraft.resources.RegistryOps
import net.minecraft.resources.ResourceKey

/**
 * Wrap [DynamicOps] as [RegistryOps] without a registry.
 *
 * For [DummyHolder] to work, the encoding must run with [RegistryOps], but for [DummyHolder]
 * we also don't need an actual registry.
 */
internal class DummyRegistryOps<T>(delegate: DynamicOps<T>) : RegistryOps<T>(delegate, null) {
    override fun <E> owner(registryKey: ResourceKey<out Registry<out E>>): Optional<HolderOwner<E>> =
        Optional.of(object : HolderOwner<E> {})
}
