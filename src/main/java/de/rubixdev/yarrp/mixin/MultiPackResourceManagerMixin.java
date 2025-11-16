package de.rubixdev.yarrp.mixin;

import com.google.common.collect.Lists;
import de.rubixdev.yarrp.ModConstants;
import de.rubixdev.yarrp.api.YarrpCallbacks;
import de.rubixdev.yarrp.api.PackPosition;
import kotlin.Unit;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.CloseableResourceManager;
import net.minecraft.server.packs.resources.MultiPackResourceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.ArrayList;
import java.util.List;

@Mixin(MultiPackResourceManager.class)
public abstract class MultiPackResourceManagerMixin implements CloseableResourceManager {
    @ModifyVariable(method = "<init>", at = @At("HEAD"), argsOnly = true)
    private static List<PackResources> yarrp$injectPacks(List<PackResources> packs, PackType type) {
        var copy = new ArrayList<>(packs);

        ModConstants.LOGGER.debug("Registering BEFORE_ALL packs with type {}", type);
        var newPacks = new ArrayList<PackResources>();
        YarrpCallbacks.run(PackPosition.BEFORE_ALL, type, pack -> {
            newPacks.add(pack);
            return Unit.INSTANCE;
        });
        Lists.reverse(copy).addAll(Lists.reverse(newPacks));

        YarrpCallbacks.addAfterVanilla(copy, type);

        ModConstants.LOGGER.debug("Registering AFTER_ALL packs with type {}", type);
        YarrpCallbacks.run(PackPosition.AFTER_ALL, type, pack -> {
            copy.add(pack);
            return Unit.INSTANCE;
        });

        YarrpCallbacks.logPackList(copy);
        return copy;
    }
}
