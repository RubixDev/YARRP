# YARRP - Yet Another Runtime Resource Pack

[![GitHub Downloads](https://img.shields.io/github/downloads/RubixDev/YARRP/total?style=for-the-badge&logo=github&label=GitHub%20Downloads&color=%23753fc7)](https://github.com/RubixDev/YARRP/releases)
[![Modrinth Downloads](https://img.shields.io/modrinth/dt/Z2RtCqwR?style=for-the-badge&logo=modrinth&label=Modrinth%20Downloads&color=%2300af5c)](https://modrinth.com/mod/yarrp)
[![CurseForge Downloads](https://img.shields.io/curseforge/dt/1385339?style=for-the-badge&logo=curseforge&label=CurseForge%20Downloads&color=%23f16436)](https://www.curseforge.com/minecraft/mc-mods/yarrp)

<img alt="Icon" src="src/main/resources/assets/yarrp/icon.png" width="128" />

Has it ever bothered you that so much configuration of blocks, items, recipes,
and now even enchantments is defined in untyped JSON files? Well then you should
probably just use
[data generation](https://docs.fabricmc.net/develop/data-generation/setup) which
is much more battle tested and even used by Mojang for the Vanilla data. _But_,
if you _also_ want to be able to adjust the data dynamically or want some things
to be configurable, then this might just be the thing for you. YARRP is inspired
by [ARRP](https://modrinth.com/mod/arrp) and
[BRRP](https://modrinth.com/mod/brrp) but written from the ground up with an
easy to use Kotlin API. It allows you to create resource packs (i.e. both data
packs and asset packs) at runtime, so you don't need to have JSON files ready
for all possible options.

## Usage

To add YARRP as a dependency, add the following to your `build.gradle.kts`:

```kotlin
repositories {
    // you can use either one
    maven("https://jitpack.io")
    maven("https://api.modrinth.com/maven")
}

dependencies {
    // <loader> is one of `fabric` or `neoforge`
    modImplementation("com.github.RubixDev.YARRP:yarrp-mc<minecraft version>-<loader>:<yarrp version>") // when using jitpack
    modImplementation("maven.modrinth:yarrp:<yarrp version>+<minecraft version>-<loader>") // when using modrinth maven
}
```

Don't forget to also list YARRP as a dependency in your `fabric.mod.json` and `neoforge.mods.toml` files.

You can then use YARRP by creating a pack and adding it to one of the register callbacks. Here's an example:

```kotlin
object MyModResources {
    // call this in your mod initializer
    fun register() {
        YarrpCallbacks.register(PackPosition.AFTER_VANILLA) { add(PACK) }
    }

    @JvmField
    val PACK = RuntimeResourcePack(
        RuntimeResourcePack.createInfo(
            Identifier.of("modid", "my_runtime_pack"),
            Text.of("Pack Title"),
            "pack version", // should probably be set to the version of your mod
        ),
        RuntimeResourcePack.createMetadata(Text.of("pack description")),
    )

    val MY_ENCHANTMENT: RegistryKey<Enchantment> = PACK.addEnchantment(
        Identifier.of("modid", "my_enchantment"),
        Enchantment.definition(
            // this dummy type can be used to create a RegistryEntryList from a given TagKey
            // but should only be used for adding resources to a runtime pack
            DummyHolderSet(ItemTags.LEG_ARMOR_ENCHANTABLE),
            5,
            3,
            Enchantment.leveledCost(5, 8),
            Enchantment.leveledCost(55, 8),
            2,
            AttributeModifierSlot.LEGS,
        ),
    )

    init {
        if (MyModSettings.myEnchantmentIsTreasure) {
            PACK.addTag(EnchantmentTags.TREASURE) { add(DEEP_POCKETS) }
        } else {
            PACK.addTag(EnchantmentTags.NON_TREASURE) { add(DEEP_POCKETS) }
        }

        if (MyModSettings.enableRecipe) {
            PACK.addRecipeAndAdvancement(
                Identifier.of("modid", "my_recipe"),
                ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, Items.DIAMOND, 64)
                    .criterion("tick", TickCriterion.Conditions.createTick())
                    .pattern("//")
                    .pattern("//")
                    .input('/', Items.STICK),
            )
        }
    }
}
```
