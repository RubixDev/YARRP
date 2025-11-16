package de.rubixdev.yarrp.mixin;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import de.rubixdev.yarrp.api.DummyHolderSet;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.resources.HolderSetCodec;
import net.minecraft.tags.TagKey;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(HolderSetCodec.class)
public abstract class HolderSetCodecMixin<E> implements Codec<HolderSet<E>> {
    @Shadow
    @Final
    private Codec<Either<TagKey<E>, List<Holder<E>>>> registryAwareCodec;

    @Inject(
        method = "encode(Lnet/minecraft/core/HolderSet;Lcom/mojang/serialization/DynamicOps;Ljava/lang/Object;)Lcom/mojang/serialization/DataResult;",
        at = @At("HEAD"),
        cancellable = true
    )
    public <T> void yarrp$encodeDummySet(
        HolderSet<E> input,
        DynamicOps<T> ops,
        T prefix,
        CallbackInfoReturnable<DataResult<T>> cir
    ) {
        if (input instanceof DummyHolderSet<E>) {
            cir.setReturnValue(registryAwareCodec.encode(input.unwrap().mapRight(List::copyOf), ops, prefix));
        }
    }
}
