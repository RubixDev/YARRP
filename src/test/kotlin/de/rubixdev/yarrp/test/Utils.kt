package de.rubixdev.yarrp.test

import net.minecraft.resources.ResourceLocation

//#if MC >= 12101
val String.id: ResourceLocation get() = ResourceLocation.fromNamespaceAndPath("yarrp", this)
//#else
//$$ val String.id get() = ResourceLocation("yarrp", this)
//#endif
