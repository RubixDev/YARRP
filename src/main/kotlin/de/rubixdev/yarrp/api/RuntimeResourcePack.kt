package de.rubixdev.yarrp.api

import com.google.gson.Gson
import com.mojang.serialization.Codec
import com.mojang.serialization.JsonOps
import de.rubixdev.yarrp.ModConstants.LOGGER
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
import net.minecraft.world.item.crafting.Recipe

//#if NEOFORGE
//$$ import net.neoforged.neoforge.common.conditions.ICondition
//#endif

//#if MC >= 12101
import net.minecraft.world.item.enchantment.Enchantment
import net.minecraft.core.registries.Registries
//#else
//$$ import net.minecraft.tags.TagManager
//#endif

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

        @JvmField
        val SOURCE = object : PackSource {
            override fun decorate(name: Component): Component = Component.translatable("pack.nameAndSource", name, Component.literal("Runtime generated"))
            override fun shouldAddAutomatically(): Boolean = true
        }

        @JvmStatic
        fun createInfo(id: ResourceLocation, title: Component, version: String) = PackLocationInfo(
            id.path,
            title,
            SOURCE,
            Optional.of(KnownPack(id.namespace, id.path, version)),
        )

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

    override fun getNamespaces(type: PackType): Set<String> = root.nested[type.directory]?.asDirectory()?.keys ?: setOf()

    override fun <T : Any> getMetadataSection(metaReader: MetadataSectionSerializer<T>): T? =
        @Suppress("UNCHECKED_CAST")
        when (metaReader.metadataSectionName) {
            PackMetadataSection.TYPE.metadataSectionName -> metadata as T
            FeatureFlagsMetadataSection.TYPE.metadataSectionName -> features as T?
            ResourceFilterSection.TYPE.metadataSectionName -> filter as T?
            OverlayMetadataSection.TYPE.metadataSectionName -> overlays as T?
            else -> null
        }

    override fun location(): PackLocationInfo = info

    override fun close() {}

    fun <T> addResource(
        type: PackType,
        registry: ResourceKey<out Registry<T>>,
        codec: Codec<T>,
        id: ResourceLocation,
        value: T,
    ): ResourceKey<T> = ResourceKey.create(registry, id).also {
        addResource(type, registry.path, codec, id, value)
    }

    fun <T> addResource(
        type: PackType,
        path: List<String>,
        codec: Codec<T>,
        id: ResourceLocation,
        value: T,
    ) {
        val path = listOf(id.namespace) + path + id.splitPath.withJsonExt
        val json = GSON.toJson(codec.encodeStart(JsonOps.INSTANCE, value).getOrThrow())
        LOGGER.debug("adding resource:\n{}\n{}", path.joinToString("/"), json)
        addResource(type, path, json)
    }

    fun addResource(type: PackType, path: List<String>, resource: String) = addResource(type, path) { resource.byteInputStream() }

    fun addResource(type: PackType, path: List<String>, resource: Resource) {
        val fullPath = listOf(type.directory) + path

        // ensure directory exists
        val dirPath = fullPath.dropLast(1)
        val dir = dirPath.fold(root as PackEntry) { entry, segment ->
            when (entry) {
                is ResourceEntry -> throw IllegalArgumentException("Part of resource path is already stored as a resource")
                is DirectoryEntry -> entry.nested.getOrPut(segment, ::DirectoryEntry)
            }
        }.asDirectory() ?: throw IllegalArgumentException("Part of resource path is already stored as a resource")

        // insert (or overwrite) resource entry
        dir[path.last()] = ResourceEntry(resource)
    }

    ////// Tags //////

    inline fun <T> addItemsToTag(tagKey: TagKey<T>, tagBuilder: TagBuilder<T>.() -> Unit) = addItemsToTag(tagKey, TagBuilder<T>().apply(tagBuilder))

    fun <T> addItemsToTag(tagKey: TagKey<T>, tagBuilder: TagBuilder<T>) = addItemsToTag(tagKey, tagBuilder.build())

    @JvmOverloads
    fun <T> addItemsToTag(tagKey: TagKey<T>, tagEntries: List<TagEntry>, replace: Boolean = false) = addItemsToTag(tagKey, TagFile(tagEntries, replace))

    fun <T> addItemsToTag(tagKey: TagKey<T>, tagFile: TagFile): TagKey<T> {
        val path = mutableListOf<String>().apply {
            add(tagKey.location.namespace)
            addAll(tagKey.registry.tagPath)
            addAll(tagKey.location.splitPath.withJsonExt)
        }
        val json = GSON.toJson(TagFile.CODEC.encodeStart(JsonOps.INSTANCE, tagFile).getOrThrow())
        LOGGER.debug("adding tag:\n{}\n{}", path.joinToString("/"), json)
        addResource(PackType.SERVER_DATA, path, json)
        return tagKey
    }

    ////// Enchantments //////

    //#if MC >= 12101
    fun addEnchantment(id: ResourceLocation, enchantment: Enchantment.EnchantmentDefinition) =
        addEnchantment(id, Enchantment.enchantment(enchantment))

    fun addEnchantment(id: ResourceLocation, enchantment: Enchantment.Builder) =
        addEnchantment(id, enchantment.build(id))

    fun addEnchantment(id: ResourceLocation, enchantment: Enchantment) =
        addResource(PackType.SERVER_DATA, Registries.ENCHANTMENT, Enchantment.DIRECT_CODEC, id, enchantment)
    //#endif

    ////// Recipes and Advancements //////

    //#if MC >= 12101
    fun addRecipe(id: ResourceLocation, recipe: Recipe<*>) =
        addResource(PackType.SERVER_DATA, Registries.RECIPE, Recipe.CODEC, id, recipe)
    //#else
    //$$ fun addRecipe(id: ResourceLocation, recipe: Recipe<*>) =
    //$$     addResource(PackType.SERVER_DATA, listOf("recipes"), Recipe.CODEC, id, recipe)
    //#endif

    fun addAdvancement(id: ResourceLocation, advancementBuilder: Advancement.Builder) = addAdvancement(advancementBuilder.build(id))

    fun addAdvancement(entry: AdvancementHolder) = addAdvancement(entry.id, entry.value)

    //#if MC >= 12101
    fun addAdvancement(id: ResourceLocation, advancement: Advancement) =
        addResource(PackType.SERVER_DATA, Registries.ADVANCEMENT, Advancement.CODEC, id, advancement)
    //#else
    //$$ fun addAdvancement(id: ResourceLocation, advancement: Advancement) =
    //$$     addResource(PackType.SERVER_DATA, listOf("advancements"), Advancement.CODEC, id, advancement)
    //#endif

    val recipeExporter by lazy {
        object : RecipeOutput {
            override fun accept(recipeId: ResourceLocation, recipe: Recipe<*>, advancement: AdvancementHolder?) {
                addRecipe(recipeId, recipe)
                if (advancement != null) addAdvancement(advancement)
            }

            override fun advancement(): Advancement.Builder = Advancement.Builder.recipeAdvancement().parent(AdvancementHolder(RecipeBuilder.ROOT_RECIPE_ADVANCEMENT, null))

            //#if NEOFORGE
            //$$ override fun accept(recipeId: ResourceLocation, recipe: Recipe<*>, advancement: AdvancementHolder?, vararg conditions: ICondition) =
            //$$     accept(recipeId, recipe, advancement)
            //#endif
        }
    }

    val recipeExporterOnlyRecipe by lazy {
        object : RecipeOutput {
            override fun accept(recipeId: ResourceLocation, recipe: Recipe<*>, advancement: AdvancementHolder?) {
                addRecipe(recipeId, recipe)
            }

            override fun advancement(): Advancement.Builder = Advancement.Builder.recipeAdvancement()

            //#if NEOFORGE
            //$$ override fun accept(recipeId: ResourceLocation, recipe: Recipe<*>, advancement: AdvancementHolder?, vararg conditions: ICondition) =
            //$$     accept(recipeId, recipe, advancement)
            //#endif
        }
    }

    val recipeExporterOnlyAdvancement by lazy {
        object : RecipeOutput {
            override fun accept(recipeId: ResourceLocation, recipe: Recipe<*>, advancement: AdvancementHolder?) {
                if (advancement != null) addAdvancement(advancement)
            }

            override fun advancement(): Advancement.Builder = Advancement.Builder.recipeAdvancement().parent(AdvancementHolder(RecipeBuilder.ROOT_RECIPE_ADVANCEMENT, null))

            //#if NEOFORGE
            //$$ override fun accept(recipeId: ResourceLocation, recipe: Recipe<*>, advancement: AdvancementHolder?, vararg conditions: ICondition) =
            //$$     accept(recipeId, recipe, advancement)
            //#endif
        }
    }

    fun addRecipeAndAdvancement(recipeId: ResourceLocation, builder: RecipeBuilder) = builder.save(recipeExporter, recipeId)

    fun addRecipeAndAdvancement(recipeId: ResourceLocation, builder: SmithingTransformRecipeBuilder) = builder.save(recipeExporter, recipeId)

    fun addRecipeAndAdvancement(recipeId: ResourceLocation, builder: SmithingTrimRecipeBuilder) = builder.save(recipeExporter, recipeId)

    fun addRecipeAndAdvancement(recipeId: ResourceLocation, builder: SpecialRecipeBuilder) = builder.save(recipeExporter, recipeId)

    //#if MC >= 12101
    fun addRecipe(recipeId: ResourceLocation, builder: RecipeBuilder): ResourceKey<Recipe<*>> =
        builder.save(recipeExporterOnlyRecipe, recipeId).let { ResourceKey.create(Registries.RECIPE, recipeId) }

    fun addRecipe(recipeId: ResourceLocation, builder: SmithingTransformRecipeBuilder): ResourceKey<Recipe<*>> =
        builder.save(recipeExporterOnlyRecipe, recipeId).let { ResourceKey.create(Registries.RECIPE, recipeId) }

    fun addRecipe(recipeId: ResourceLocation, builder: SmithingTrimRecipeBuilder): ResourceKey<Recipe<*>> =
        builder.save(recipeExporterOnlyRecipe, recipeId).let { ResourceKey.create(Registries.RECIPE, recipeId) }

    fun addRecipe(recipeId: ResourceLocation, builder: SpecialRecipeBuilder): ResourceKey<Recipe<*>> =
        builder.save(recipeExporterOnlyRecipe, recipeId).let { ResourceKey.create(Registries.RECIPE, recipeId) }
    //#else
    //$$ fun addRecipe(recipeId: ResourceLocation, builder: RecipeBuilder) =
    //$$     builder.save(recipeExporterOnlyRecipe, recipeId).let { recipeId }
    //$$
    //$$ fun addRecipe(recipeId: ResourceLocation, builder: SmithingTransformRecipeBuilder) =
    //$$     builder.save(recipeExporterOnlyRecipe, recipeId).let { recipeId }
    //$$
    //$$ fun addRecipe(recipeId: ResourceLocation, builder: SmithingTrimRecipeBuilder) =
    //$$     builder.save(recipeExporterOnlyRecipe, recipeId).let { recipeId }
    //$$
    //$$ fun addRecipe(recipeId: ResourceLocation, builder: SpecialRecipeBuilder) =
    //$$     builder.save(recipeExporterOnlyRecipe, recipeId).let { recipeId }
    //#endif

    fun addAdvancement(recipeId: ResourceLocation, builder: RecipeBuilder) = builder.save(recipeExporterOnlyAdvancement, recipeId)

    fun addAdvancement(recipeId: ResourceLocation, builder: SmithingTransformRecipeBuilder) = builder.save(recipeExporterOnlyAdvancement, recipeId)

    fun addAdvancement(recipeId: ResourceLocation, builder: SmithingTrimRecipeBuilder) = builder.save(recipeExporterOnlyAdvancement, recipeId)

    fun addAdvancement(recipeId: ResourceLocation, builder: SpecialRecipeBuilder) = builder.save(recipeExporterOnlyAdvancement, recipeId)

    fun advancementBuilderForRecipe(recipe: ResourceKey<out Recipe<*>>): Advancement.Builder = advancementBuilderForRecipe(recipe.location())

    fun advancementBuilderForRecipe(recipeId: ResourceLocation): Advancement.Builder = recipeExporterOnlyAdvancement.advancement()
        .addCriterion("has_the_recipe", RecipeUnlockedTrigger.unlocked(recipeId))
        .rewards(AdvancementRewards.Builder.recipe(recipeId))
        .requirements(AdvancementRequirements.Strategy.OR)
}
