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
import java.util.StringJoiner;
import java.util.stream.Collector;

@Mixin(MultiPackResourceManager.class)
public abstract class MultiPackResourceManagerMixin implements CloseableResourceManager {
    @ModifyVariable(method = "<init>", at = @At("HEAD"), argsOnly = true)
    private static List<PackResources> yarrp$injectPacks(List<PackResources> packs, PackType type) {
        var copy = new ArrayList<>(packs);
        var reversedView = Lists.reverse(copy);

        ModConstants.LOGGER.debug("Registering BEFORE_VANILLA packs with type {}", type);
        YarrpCallbacks.run(PackPosition.BEFORE_VANILLA, type, pack -> {
            reversedView.add(pack);
            return Unit.INSTANCE;
        });

        ModConstants.LOGGER.debug("Registering AFTER_VANILLA packs with type {}", type);
        YarrpCallbacks.run(PackPosition.AFTER_VANILLA, type, pack -> {
            copy.add(pack);
            return Unit.INSTANCE;
        });

        ModConstants.LOGGER.debug(
            "Full list of packs is now:{}",
            copy.stream()
                .map(PackResources::packId)
                .collect(
                    Collector.of(
                        () -> new StringJoiner("\n- ", "\n- ", "").setEmptyValue(""),
                        StringJoiner::add,
                        StringJoiner::merge,
                        StringJoiner::toString
                    )
                )
        );

        return copy;
    }
}
