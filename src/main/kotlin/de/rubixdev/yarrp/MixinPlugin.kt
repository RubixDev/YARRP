package de.rubixdev.yarrp

import me.fallenbreath.conditionalmixin.api.mixin.RestrictiveMixinConfigPlugin

class MixinPlugin : RestrictiveMixinConfigPlugin() {
    override fun getRefMapperConfig(): String? = null
    override fun acceptTargets(myTargets: Set<String?>?, otherTargets: Set<String?>?) {}
    override fun getMixins(): List<String>? = null
}
