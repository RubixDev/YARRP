package de.rubixdev.yarrp.api

import com.google.gson.Gson
import com.mojang.serialization.Codec
import com.mojang.serialization.JsonOps
import de.rubixdev.yarrp.LOGGER
import de.rubixdev.yarrp.impl.DirectoryEntry
import de.rubixdev.yarrp.impl.PackEntry
import de.rubixdev.yarrp.impl.Resource
import de.rubixdev.yarrp.impl.ResourceEntry
import java.io.InputStream
import java.util.Optional
import net.minecraft.SharedConstants
import net.minecraft.advancements.Advancement
import net.minecraft.advancements.AdvancementHolder
import net.minecraft.advancements.AdvancementRequirements
import net.minecraft.advancements.AdvancementRewards
import net.minecraft.advancements.critereon.RecipeUnlockedTrigger
import net.minecraft.core.Registry
import net.minecraft.data.recipes.RecipeBuilder
import net.minecraft.data.recipes.RecipeOutput
import net.minecraft.data.recipes.SmithingTransformRecipeBuilder
import net.minecraft.data.recipes.SmithingTrimRecipeBuilder
import net.minecraft.data.recipes.SpecialRecipeBuilder
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.packs.FeatureFlagsMetadataSection
import net.minecraft.server.packs.OverlayMetadataSection
import net.minecraft.server.packs.PackLocationInfo
import net.minecraft.server.packs.PackResources
import net.minecraft.server.packs.PackType
import net.minecraft.server.packs.metadata.MetadataSectionSerializer
import net.minecraft.server.packs.metadata.pack.PackMetadataSection
import net.minecraft.server.packs.repository.KnownPack
import net.minecraft.server.packs.repository.PackSource
import net.minecraft.server.packs.resources.IoSupplier
import net.minecraft.server.packs.resources.ResourceFilterSection
import net.minecraft.tags.TagEntry
import net.minecraft.tags.TagFile
import net.minecraft.tags.TagKey
import net.minecraft.world.entity.EntityType
import net.minecraft.world.item.Item
import net.minecraft.world.item.crafting.Recipe
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.material.Fluid

//#if NEOFORGE
//$$ import net.neoforged.neoforge.common.conditions.ICondition
//#endif

//#if MC >= 12101
import net.minecraft.world.item.enchantment.Enchantment
import net.minecraft.core.registries.Registries
//#else
//$$ import net.minecraft.tags.TagManager
//#endif

/**
 * A resource pack that is generated at runtime.
 *
 * @param[info] basic information about this resource pack. May be created using [createInfo]
 * @param[metadata] the main metadata for this resource pack. May be created using [createMetadata]
 * @param[features] optional metadata for required feature flags
 * @param[filter] optional metadata for filters
 * @param[overlays] optional metadata for pack overlays
 * @param[extraFiles] additional raw files to include in this resource pack
 */
class RuntimeResourcePack(
    private val info: PackLocationInfo,
    val metadata: PackMetadataSection,
    val features: FeatureFlagsMetadataSection? = null,
    val filter: ResourceFilterSection? = null,
    val overlays: OverlayMetadataSection? = null,
    // TODO: client-only metadata (LanguageResourceMetadata, GuiResourceMetadata, VillagerResourceMetadata, AnimationResourceMetadata, TextureResourceMetadata)
    private val extraFiles: Map<List<String>, IoSupplier<InputStream>> = mapOf(),
) : PackResources {
    companion object {
        private val GSON = Gson()

        //#if MC >= 12101
        private val ResourceKey<out Registry<*>>.path get() = Registries.elementsDirPath(this).split("/")
        private val ResourceKey<out Registry<*>>.tagPath get() = Registries.tagsDirPath(this).split("/")
        //#else
        //$$ private val ResourceKey<out Registry<*>>.path get() = location().splitPath
        //$$ private val ResourceKey<out Registry<*>>.tagPath get() = TagManager.getTagDir(this).split("/")
        //#endif
        private val ResourceLocation.splitPath get() = path.split("/")
        private val List<String>.withJsonExt get() = dropLast(1) + "${last()}.json"

        /**
         * The [PackSource] that should be used by runtime packs.
         */
        @JvmField
        val SOURCE = object : PackSource {
            override fun decorate(name: Component): Component =
                Component.translatable("pack.nameAndSource", name, Component.literal("runtime generated"))
            override fun shouldAddAutomatically(): Boolean = true
        }

        /**
         * A utility function for creating [PackLocationInfo] for runtime packs.
         *
         * Uses [SOURCE] as the [PackSource].
         *
         * @param[id] the pack identifier
         * @param[title] the pack title
         * @param[version] the pack version. You probably want this to be the same as your mod version
         * @return the created [PackLocationInfo]
         */
        @JvmStatic
        fun createInfo(id: ResourceLocation, title: Component, version: String) = PackLocationInfo(
            id.path,
            title,
            SOURCE,
            // without this, Minecraft considers the pack to be experimental
            Optional.of(KnownPack(id.namespace, id.path, version)),
        )

        /**
         * A utility function for creating the [PackMetadataSection] for a runtime pack.
         *
         * As the pack is generated at runtime, its pack version is always set to the current version.
         *
         * @param[description] the pack description
         * @param[type] the pack type. Defaults to server data
         * @return the created [PackMetadataSection]
         */
        @JvmStatic
        @JvmOverloads
        fun createMetadata(description: Component, type: PackType = PackType.SERVER_DATA) = PackMetadataSection(
            description,
            SharedConstants.getCurrentVersion().getPackVersion(type),
            Optional.empty(),
        )
    }

    private val root = DirectoryEntry()

    // TODO: is this needed for anything but a pack.png
    override fun getRootResource(vararg segments: String): IoSupplier<InputStream>? = extraFiles[segments.toList()]

    override fun getResource(type: PackType, id: ResourceLocation): IoSupplier<InputStream>? {
        val path = listOf(type.directory, id.namespace) + id.path.split("/")
        return root.find(path)?.asResource()
    }

    override fun listResources(
        type: PackType,
        namespace: String,
        prefix: String,
        consumer: PackResources.ResourceOutput,
    ) {
        val basePath = listOf(type.directory, namespace) + prefix.split("/")
        root.find(basePath)?.findAllResources { path, resource ->
            val id = ResourceLocation.tryBuild(namespace, (basePath.drop(2) + path).joinToString("/")) ?: run {
                LOGGER.error("Invalid path in pack: {}:{}, ignoring", namespace, path.joinToString("/"))
                return@findAllResources
            }
            consumer.accept(id, resource)
        }
    }

    override fun getNamespaces(type: PackType): Set<String> =
        root.nested[type.directory]?.asDirectory()?.keys ?: setOf()

    override fun <T> getMetadataSection(metaReader: MetadataSectionSerializer<T>): T? =
        @Suppress("UNCHECKED_CAST")
        when (metaReader) {
            PackMetadataSection.TYPE -> metadata as T
            FeatureFlagsMetadataSection.TYPE -> features as T?
            ResourceFilterSection.TYPE -> filter as T?
            OverlayMetadataSection.TYPE -> overlays as T?
            else -> null
        }

    override fun location(): PackLocationInfo = info

    override fun close() {}

    ////////////////////////////////////////////////////

    /**
     * Add a generic serializable resource in a registry.
     *
     * @param[type] the resource type
     * @param[registry] the registry which holds this type of resource
     * @param[codec] a codec to serialize the value
     * @param[id] the resource location within the registry
     * @param[value] the raw resource value
     * @return a [ResourceKey] pointing to the created resource
     */
    fun <T> addResource(
        type: PackType,
        registry: ResourceKey<out Registry<T>>,
        codec: Codec<T>,
        id: ResourceLocation,
        value: T,
    ): ResourceKey<T> = ResourceKey.create(registry, id).also {
        addResource(type, registry.path, codec, id, value)
    }

    /**
     * Add a generic serializable resource at a path.
     *
     * @param[type] the resource type
     * @param[path] the base path of the resource
     * @param[codec] a codec to serialize the value
     * @param[id] the resource location
     * @param[value] the raw resource value
     */
    fun <T> addResource(type: PackType, path: List<String>, codec: Codec<T>, id: ResourceLocation, value: T) {
        val path = listOf(id.namespace) + path + id.splitPath.withJsonExt
        val json = GSON.toJson(codec.encodeStart(JsonOps.INSTANCE, value).getOrThrow())
        LOGGER.debug("adding resource to {}:\n{}\n{}", info.id, path.joinToString("/"), json)
        addResource(type, path, json)
    }

    /**
     * Add a generic resource with the given content.
     *
     * @param[type] the resource type
     * @param[path] the full path of the resource
     * @param[resource] the string content of the resource file
     */
    fun addResource(type: PackType, path: List<String>, resource: String) =
        addResource(type, path) { resource.byteInputStream() }

    /**
     * Add a generic resource with the given content.
     *
     * @param[type] the resource type
     * @param[path] the full path of the resource
     * @param[resource] the [InputStream] supplier of the resource file
     */
    fun addResource(type: PackType, path: List<String>, resource: Resource) {
        val fullPath = listOf(type.directory) + path

        // ensure directory exists
        val dirPath = fullPath.dropLast(1)
        val dir = dirPath.fold(root as PackEntry) { entry, segment ->
            when (entry) {
                is ResourceEntry -> throw IllegalArgumentException(
                    "Part of resource path is already stored as a resource",
                )

                is DirectoryEntry -> entry.nested.getOrPut(segment, ::DirectoryEntry)
            }
        }.asDirectory() ?: throw IllegalArgumentException("Part of resource path is already stored as a resource")

        // insert (or overwrite) resource entry
        dir[path.last()] = ResourceEntry(resource)
    }

    ////// Tags //////

    /**
     * Add an [EntityType] tag definition using an [IntrinsicHolderTagBuilder] callback.
     *
     * @param[tagKey] the tag to create or add to or replace
     * @param[tagBuilder] a callback on [IntrinsicHolderTagBuilder] to create the tag
     * @return the passed [TagKey]
     */
    inline fun addEntityTypeTag(
        tagKey: TagKey<EntityType<*>>,
        tagBuilder: IntrinsicHolderTagBuilder<EntityType<*>>.() -> Unit,
    ) = addTag(tagKey, IntrinsicHolderTagBuilder.entityType(), tagBuilder)

    /**
     * Add a [Fluid] tag definition using an [IntrinsicHolderTagBuilder] callback.
     *
     * @param[tagKey] the tag to create or add to or replace
     * @param[tagBuilder] a callback on [IntrinsicHolderTagBuilder] to create the tag
     * @return the passed [TagKey]
     */
    inline fun addFluidTag(tagKey: TagKey<Fluid>, tagBuilder: IntrinsicHolderTagBuilder<Fluid>.() -> Unit) =
        addTag(tagKey, IntrinsicHolderTagBuilder.fluid(), tagBuilder)

    /**
     * Add a [Item] tag definition using an [IntrinsicHolderTagBuilder] callback.
     *
     * @param[tagKey] the tag to create or add to or replace
     * @param[tagBuilder] a callback on [IntrinsicHolderTagBuilder] to create the tag
     * @return the passed [TagKey]
     */
    inline fun addItemTag(tagKey: TagKey<Item>, tagBuilder: IntrinsicHolderTagBuilder<Item>.() -> Unit) =
        addTag(tagKey, IntrinsicHolderTagBuilder.item(), tagBuilder)

    /**
     * Add a [Block] tag definition using an [IntrinsicHolderTagBuilder] callback.
     *
     * @param[tagKey] the tag to create or add to or replace
     * @param[tagBuilder] a callback on [IntrinsicHolderTagBuilder] to create the tag
     * @return the passed [TagKey]
     */
    inline fun addBlockTag(tagKey: TagKey<Block>, tagBuilder: IntrinsicHolderTagBuilder<Block>.() -> Unit) =
        addTag(tagKey, IntrinsicHolderTagBuilder.block(), tagBuilder)

    /**
     * Add a tag definition using a [TagBuilder] callback.
     *
     * @param[tagKey] the tag to create or add to or replace
     * @param[tagBuilder] a callback on [TagBuilder] to create the tag
     * @return the passed [TagKey]
     */
    inline fun <T> addTag(tagKey: TagKey<T>, tagBuilder: TagBuilder<T>.() -> Unit) =
        addTag(tagKey, TagBuilder(), tagBuilder)

    /**
     * Add a tag definition using a generic [TagBuilder] callback.
     *
     * @param[tagKey] the tag to create or add to or replace
     * @param[builder] the [TagBuilder] instance to use
     * @param[tagBuilder] a callback on the provided type of [TagBuilder] to create the tag
     * @return the passed [TagKey]
     */
    inline fun <T, B : TagBuilder<T>> addTag(tagKey: TagKey<T>, builder: B, tagBuilder: B.() -> Unit) =
        addTag(tagKey, builder.apply(tagBuilder))

    /**
     * Add a tag definition using a [TagBuilder].
     *
     * @param[tagKey] the tag to create or add to or replace
     * @param[tagBuilder] a [TagBuilder] to create the tag
     * @return the passed [TagKey]
     */
    fun <T> addTag(tagKey: TagKey<T>, tagBuilder: TagBuilder<T>) = addTag(tagKey, tagBuilder.build())

    /**
     * Add a tag definition given its entries and `replace` value.
     *
     * @param[tagKey] the tag to create or add to or replace
     * @param[tagEntries] the entry list for the tag
     * @param[replace] whether this tag definition should replace existing tag definitions with
     *                 the same [TagKey] in other data packs. Defaults to `false`
     * @return the passed [TagKey]
     */
    @JvmOverloads
    fun <T> addTag(tagKey: TagKey<T>, tagEntries: List<TagEntry>, replace: Boolean = false) =
        addTag(tagKey, TagFile(tagEntries, replace))

    /**
     * Add a tag definition given its [TagFile] description.
     *
     * @param[tagKey] the tag to create or add to or replace
     * @param[tagFile] the tag definition
     * @return the passed [TagKey]
     */
    fun <T> addTag(tagKey: TagKey<T>, tagFile: TagFile) = tagKey.also {
        val path = mutableListOf<String>().apply {
            add(tagKey.location.namespace)
            addAll(tagKey.registry.tagPath)
            addAll(tagKey.location.splitPath.withJsonExt)
        }
        val json = GSON.toJson(TagFile.CODEC.encodeStart(JsonOps.INSTANCE, tagFile).getOrThrow())
        LOGGER.debug("adding tag:\n{}\n{}", path.joinToString("/"), json)
        addResource(PackType.SERVER_DATA, path, json)
    }

    ////// Enchantments //////

    //#if MC >= 12101
    /**
     * Add an enchantment given its [Enchantment.EnchantmentDefinition].
     *
     * @param[id] the enchantment identifier
     * @param[enchantment] the enchantment definition
     * @return a [ResourceKey] pointing to the created enchantment
     */
    fun addEnchantment(id: ResourceLocation, enchantment: Enchantment.EnchantmentDefinition) =
        addEnchantment(id, Enchantment.enchantment(enchantment))

    /**
     * Add an enchantment given an [Enchantment.Builder].
     *
     * @param[id] the enchantment identifier
     * @param[enchantment] the enchantment builder
     * @return a [ResourceKey] pointing to the created enchantment
     */
    fun addEnchantment(id: ResourceLocation, enchantment: Enchantment.Builder) =
        addEnchantment(id, enchantment.build(id))

    /**
     * Add an enchantment.
     *
     * @param[id] the enchantment identifier
     * @param[enchantment] the enchantment
     * @return a [ResourceKey] pointing to the created enchantment
     */
    fun addEnchantment(id: ResourceLocation, enchantment: Enchantment) =
        addResource(PackType.SERVER_DATA, Registries.ENCHANTMENT, Enchantment.DIRECT_CODEC, id, enchantment)
    //#endif

    ////// Recipes and Advancements //////

    /**
     * Add a recipe.
     *
     * @param[id] the recipe identifier
     * @param[recipe] the recipe
     * @return a [ResourceKey] pointing to the created recipe
     */
    //#if MC >= 12101
    fun addRecipe(id: ResourceLocation, recipe: Recipe<*>) =
        addResource(PackType.SERVER_DATA, Registries.RECIPE, Recipe.CODEC, id, recipe)
    //#else
    //$$ fun addRecipe(id: ResourceLocation, recipe: Recipe<*>) =
    //$$     addResource(PackType.SERVER_DATA, listOf("recipes"), Recipe.CODEC, id, recipe)
    //#endif

    /**
     * Add an advancement from an [Advancement.Builder].
     *
     * @param[id] the advancement identifier
     * @param[advancementBuilder] the advancement builder
     * @return a [ResourceKey] pointing to the created advancement
     */
    fun addAdvancement(id: ResourceLocation, advancementBuilder: Advancement.Builder) =
        addAdvancement(advancementBuilder.build(id))

    /**
     * Add an advancement from an [AdvancementHolder].
     *
     * @param[entry] the full advancement definition
     * @return a [ResourceKey] pointing to the created advancement
     */
    fun addAdvancement(entry: AdvancementHolder) = addAdvancement(entry.id, entry.value)

    /**
     * Add an advancement.
     *
     * @param[id] the advancement identifier
     * @param[advancement] the advancement definition
     * @return a [ResourceKey] pointing to the created advancement
     */
    //#if MC >= 12101
    fun addAdvancement(id: ResourceLocation, advancement: Advancement) =
        addResource(PackType.SERVER_DATA, Registries.ADVANCEMENT, Advancement.CODEC, id, advancement)
    //#else
    //$$ fun addAdvancement(id: ResourceLocation, advancement: Advancement) =
    //$$     addResource(PackType.SERVER_DATA, listOf("advancements"), Advancement.CODEC, id, advancement)
    //#endif

    private inline fun createRecipeExporter(
        crossinline impl: (recipeId: ResourceLocation, recipe: Recipe<*>, advancement: AdvancementHolder?) -> Unit,
    ) = object : RecipeOutput {
        override fun accept(recipeId: ResourceLocation, recipe: Recipe<*>, advancement: AdvancementHolder?) =
            impl(recipeId, recipe, advancement)

        @Suppress("removal", "DEPRECATION") // vanilla does the same
        override fun advancement(): Advancement.Builder =
            Advancement.Builder.recipeAdvancement().parent(RecipeBuilder.ROOT_RECIPE_ADVANCEMENT)

        //#if NEOFORGE
        //$$ override fun accept(recipeId: ResourceLocation, recipe: Recipe<*>, advancement: AdvancementHolder?, vararg conditions: ICondition) =
        //$$     accept(recipeId, recipe, advancement)
        //#endif
    }

    /**
     * A [RecipeOutput] which adds recipes and advancements to this pack.
     */
    val recipeExporter: RecipeOutput by lazy {
        createRecipeExporter { recipeId, recipe, advancement ->
            addRecipe(recipeId, recipe)
            if (advancement != null) addAdvancement(advancement)
        }
    }

    /**
     * A [RecipeOutput] which ignores advancements and only adds recipes to this pack.
     */
    val recipeExporterOnlyRecipe: RecipeOutput by lazy {
        createRecipeExporter { recipeId, recipe, _ ->
            addRecipe(recipeId, recipe)
        }
    }

    /**
     * A [RecipeOutput] which ignores recipes and only adds advancements to this pack.
     */
    val recipeExporterOnlyAdvancement: RecipeOutput by lazy {
        createRecipeExporter { _, _, advancement ->
            if (advancement != null) addAdvancement(advancement)
        }
    }

    /**
     * Add both a recipe and an advancement using the given recipe builder.
     *
     * @param[recipeId] the recipe identifier
     * @param[builder] the recipe builder
     */
    fun addRecipeAndAdvancement(recipeId: ResourceLocation, builder: RecipeBuilder) =
        builder.save(recipeExporter, recipeId)

    /**
     * Add both a recipe and an advancement using the given recipe builder.
     *
     * @param[recipeId] the recipe identifier
     * @param[builder] the recipe builder
     */
    fun addRecipeAndAdvancement(recipeId: ResourceLocation, builder: SmithingTransformRecipeBuilder) =
        builder.save(recipeExporter, recipeId)

    /**
     * Add both a recipe and an advancement using the given recipe builder.
     *
     * @param[recipeId] the recipe identifier
     * @param[builder] the recipe builder
     */
    fun addRecipeAndAdvancement(recipeId: ResourceLocation, builder: SmithingTrimRecipeBuilder) =
        builder.save(recipeExporter, recipeId)

    /**
     * Add both a recipe and an advancement using the given recipe builder.
     *
     * @param[recipeId] the recipe identifier
     * @param[builder] the recipe builder
     */
    fun addRecipeAndAdvancement(recipeId: ResourceLocation, builder: SpecialRecipeBuilder) =
        builder.save(recipeExporter, recipeId)

    //#if MC >= 12101
    /**
     * Add only the recipe from a recipe builder.
     *
     * @param[recipeId] the recipe identifier
     * @param[builder] the recipe builder
     * @return a [ResourceKey] pointing to the created recipe
     */
    fun addRecipe(recipeId: ResourceLocation, builder: RecipeBuilder): ResourceKey<Recipe<*>> =
        builder.save(recipeExporterOnlyRecipe, recipeId).let { ResourceKey.create(Registries.RECIPE, recipeId) }

    /**
     * Add only the recipe from a recipe builder.
     *
     * @param[recipeId] the recipe identifier
     * @param[builder] the recipe builder
     * @return a [ResourceKey] pointing to the created recipe
     */
    fun addRecipe(recipeId: ResourceLocation, builder: SmithingTransformRecipeBuilder): ResourceKey<Recipe<*>> =
        builder.save(recipeExporterOnlyRecipe, recipeId).let { ResourceKey.create(Registries.RECIPE, recipeId) }

    /**
     * Add only the recipe from a recipe builder.
     *
     * @param[recipeId] the recipe identifier
     * @param[builder] the recipe builder
     * @return a [ResourceKey] pointing to the created recipe
     */
    fun addRecipe(recipeId: ResourceLocation, builder: SmithingTrimRecipeBuilder): ResourceKey<Recipe<*>> =
        builder.save(recipeExporterOnlyRecipe, recipeId).let { ResourceKey.create(Registries.RECIPE, recipeId) }

    /**
     * Add only the recipe from a recipe builder.
     *
     * @param[recipeId] the recipe identifier
     * @param[builder] the recipe builder
     * @return a [ResourceKey] pointing to the created recipe
     */
    fun addRecipe(recipeId: ResourceLocation, builder: SpecialRecipeBuilder): ResourceKey<Recipe<*>> =
        builder.save(recipeExporterOnlyRecipe, recipeId).let { ResourceKey.create(Registries.RECIPE, recipeId) }
    //#else
    //$$ /**
    //$$  * Add only the recipe from a recipe builder.
    //$$  *
    //$$  * @param[recipeId] the recipe identifier
    //$$  * @param[builder] the recipe builder
    //$$  * @return the passed recipe identifier
    //$$  */
    //$$ fun addRecipe(recipeId: ResourceLocation, builder: RecipeBuilder) =
    //$$     builder.save(recipeExporterOnlyRecipe, recipeId).let { recipeId }
    //$$
    //$$ /**
    //$$  * Add only the recipe from a recipe builder.
    //$$  *
    //$$  * @param[recipeId] the recipe identifier
    //$$  * @param[builder] the recipe builder
    //$$  * @return the passed recipe identifier
    //$$  */
    //$$ fun addRecipe(recipeId: ResourceLocation, builder: SmithingTransformRecipeBuilder) =
    //$$     builder.save(recipeExporterOnlyRecipe, recipeId).let { recipeId }
    //$$
    //$$ /**
    //$$  * Add only the recipe from a recipe builder.
    //$$  *
    //$$  * @param[recipeId] the recipe identifier
    //$$  * @param[builder] the recipe builder
    //$$  * @return the passed recipe identifier
    //$$  */
    //$$ fun addRecipe(recipeId: ResourceLocation, builder: SmithingTrimRecipeBuilder) =
    //$$     builder.save(recipeExporterOnlyRecipe, recipeId).let { recipeId }
    //$$
    //$$ /**
    //$$  * Add only the recipe from a recipe builder.
    //$$  *
    //$$  * @param[recipeId] the recipe identifier
    //$$  * @param[builder] the recipe builder
    //$$  * @return the passed recipe identifier
    //$$  */
    //$$ fun addRecipe(recipeId: ResourceLocation, builder: SpecialRecipeBuilder) =
    //$$     builder.save(recipeExporterOnlyRecipe, recipeId).let { recipeId }
    //#endif

    /**
     * Add only the advancement from a recipe builder.
     *
     * @param[recipeId] the recipe identifier
     * @param[builder] the recipe builder
     */
    fun addAdvancement(recipeId: ResourceLocation, builder: RecipeBuilder) =
        builder.save(recipeExporterOnlyAdvancement, recipeId)

    /**
     * Add only the advancement from a recipe builder.
     *
     * @param[recipeId] the recipe identifier
     * @param[builder] the recipe builder
     */
    fun addAdvancement(recipeId: ResourceLocation, builder: SmithingTransformRecipeBuilder) =
        builder.save(recipeExporterOnlyAdvancement, recipeId)

    /**
     * Add only the advancement from a recipe builder.
     *
     * @param[recipeId] the recipe identifier
     * @param[builder] the recipe builder
     */
    fun addAdvancement(recipeId: ResourceLocation, builder: SmithingTrimRecipeBuilder) =
        builder.save(recipeExporterOnlyAdvancement, recipeId)

    /**
     * Add only the advancement from a recipe builder.
     *
     * @param[recipeId] the recipe identifier
     * @param[builder] the recipe builder
     */
    fun addAdvancement(recipeId: ResourceLocation, builder: SpecialRecipeBuilder) =
        builder.save(recipeExporterOnlyAdvancement, recipeId)

    /**
     * Create an advancement builder for unlocking the given recipe.
     *
     * @param[recipe] the recipe to unlock
     * @return the created advancement builder
     */
    fun advancementBuilderForRecipe(recipe: ResourceKey<out Recipe<*>>): Advancement.Builder =
        advancementBuilderForRecipe(recipe.location())

    /**
     * Create an advancement builder for unlocking the given recipe.
     *
     * @param[recipeId] the recipe to unlock
     * @return the created advancement builder
     */
    fun advancementBuilderForRecipe(recipeId: ResourceLocation): Advancement.Builder =
        recipeExporterOnlyAdvancement.advancement()
            .addCriterion("has_the_recipe", RecipeUnlockedTrigger.unlocked(recipeId))
            .rewards(AdvancementRewards.Builder.recipe(recipeId))
            .requirements(AdvancementRequirements.Strategy.OR)
}
