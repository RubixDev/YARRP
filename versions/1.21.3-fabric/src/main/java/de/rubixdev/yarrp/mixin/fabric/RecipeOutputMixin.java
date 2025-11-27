package de.rubixdev.yarrp.mixin.fabric;

import net.minecraft.data.recipes.RecipeOutput;
import org.spongepowered.asm.mixin.Mixin;

//#if MC == 12101 || MC >= 12105
//$$ import me.fallenbreath.conditionalmixin.api.annotation.Condition;
//$$ import me.fallenbreath.conditionalmixin.api.annotation.Restriction;
//$$ import net.fabricmc.fabric.api.datagen.v1.recipe.FabricRecipeExporter;
//#endif

// see https://github.com/FabricMC/fabric/issues/5007 for context
@Mixin(RecipeOutput.class)
//#if MC == 12101 || MC >= 12105
//$$ @Restriction(require = @Condition(value = "fabric-api", versionPredicates = { ">=0.116.4 <0.118.0", ">=0.127.0" }))
//$$ public interface RecipeOutputMixin extends FabricRecipeExporter {}
//#else
public interface RecipeOutputMixin {}
//#endif
