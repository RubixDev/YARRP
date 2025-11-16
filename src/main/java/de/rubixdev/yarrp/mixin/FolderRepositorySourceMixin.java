package de.rubixdev.yarrp.mixin;

import de.rubixdev.yarrp.ModConstants;
import de.rubixdev.yarrp.api.PackPosition;
import de.rubixdev.yarrp.api.YarrpCallbacks;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.FolderRepositorySource;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.RepositorySource;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Consumer;

@Mixin(FolderRepositorySource.class)
public abstract class FolderRepositorySourceMixin implements RepositorySource {
    @Shadow
    @Final
    private PackType packType;

    @Inject(method = "loadPacks", at = @At("HEAD"))
    public void yarrp$injectPacks(Consumer<Pack> consumer, CallbackInfo ci) {
        ModConstants.LOGGER.debug("Registering BEFORE_USER packs with type {}", packType);
        YarrpCallbacks.run(
            PackPosition.BEFORE_USER,
            packType,
            YarrpCallbacks.wrapPackConsumer(consumer, packType, Pack.Position.BOTTOM)
        );
        YarrpCallbacks.run(
            PackPosition.AFTER_USER,
            packType,
            YarrpCallbacks.wrapPackConsumer(consumer, packType, Pack.Position.TOP)
        );
    }
}
