import kotlin.reflect.KProperty
import kotlin.reflect.jvm.jvmErasure
import org.gradle.jvm.tasks.Jar

plugins {
    id("maven-publish")
    id("dev.architectury.loom")
    kotlin("jvm")
    id("com.replaymod.preprocess")
    id("me.fallenbreath.yamlang")
    id("org.jetbrains.dokka")
}

val loaderName = if (project.name.endsWith("-common")) "common" else loom.platform.get().name.lowercase()
assert(loaderName in listOf("common", "fabric", "neoforge"))
assert(project.name.endsWith("-$loaderName"))
enum class Loader {
    COMMON,
    FABRIC,
    NEOFORGE,
    ;

    val isCommon get() = this == COMMON
    val isFabric get() = this == FABRIC
    val isNeoForge get() = this == NEOFORGE
}
val loader = when (loaderName) {
    "common" -> Loader.COMMON
    "fabric" -> Loader.FABRIC
    "neoforge" -> Loader.NEOFORGE
    else -> throw AssertionError("invalid loader '$loaderName'")
}

fun Boolean.toInt() = if (this) 1 else 0

val mcVersion: Int by project.extra

preprocess {
    vars.put("MC", mcVersion)
    vars.put("FABRIC", loader.isFabric.toInt())
    vars.put("NEOFORGE", loader.isNeoForge.toInt())
}

@Suppress("PropertyName")
class Props {
    // automatically convert to other types from the string properties
    private inner class Prop {
        @Suppress("UNCHECKED_CAST")
        operator fun <T> getValue(thisRef: Any?, property: KProperty<*>): T = when (property.returnType.jvmErasure) {
            Boolean::class -> project.extra[property.name].toString().toBoolean()
            Int::class -> project.extra[property.name].toString().toInt()
            List::class -> project.extra[property.name].toString().split(',')
            else -> project.extra[property.name]
        } as T
    }
    private val prop = Prop()

    //// Global Properties ////
    val fabric_loader_version: String by prop

    val mod_id: String by prop
    val mod_name: String by prop
    val mod_authors: List<String> by prop
    val mod_version: String by prop
    val mod_description: String by prop
    val maven_group: String by prop
    val archives_base_name: String by prop
    val license: String by prop
    val homepage_url: String by prop
    val sources_url: String by prop
    val issues_url: String by prop

    val fabric_kotlin_version: String by prop
    val neoforge_kotlin_version: String by prop
    val conditional_mixin_version: String by prop

    //// Version Specific Properties ////
    val minecraft_version: String by prop
    val parchment_version: String by prop

    val minecraft_version_range_fabric: String by prop
    val minecraft_version_range_neoforge: String by prop
    val neoforge_version: String by prop
    val neoforge_version_range: String by prop

    val early_loading_screen_version: String by prop
}
val props: Props = Props()

loom {
    runConfigs.all {
        // to make sure it generates all "Minecraft Client (:subproject_name)" applications
        isIdeConfigGenerated = !loader.isCommon
        runDir = "../../run-$loaderName"
        vmArg("-Dmixin.debug.export=true")
    }

    rootDir.resolve("src/main/resources/${props.mod_id}.accesswidener").let {
        if (it.exists()) {
            accessWidenerPath = it
        }
    }

    log4jConfigs.from(rootDir.resolve("log4j-dev.xml"))
}

repositories {
    when (loader) {
        Loader.COMMON -> {}

        Loader.FABRIC -> {}

        Loader.NEOFORGE -> {
            // NeoForge
            maven("https://maven.neoforged.net/releases")
            // Kotlin for Forge
            maven("https://thedarkcolour.github.io/KotlinForForge/")
        }
    }

    // Mixin
    maven("https://repo.spongepowered.org/maven/")
    // Parchment Mappings
    maven("https://maven.parchmentmc.org")
    // Conditional Mixin
    maven("https://jitpack.io")
    maven("https://maven.fallenbreath.me/releases")
    // Other mods from Modrinth
    maven("https://api.modrinth.com/maven")
}

dependencies {
    minecraft("com.mojang:minecraft:${props.minecraft_version}")
    mappings(
        loom.layered {
            officialMojangMappings()
            parchment("org.parchmentmc.data:parchment-${props.minecraft_version}:${props.parchment_version}@zip")
        },
    )

    // outside the fabric specific projects this should only be used for the @Environment annotation
    modCompileOnly("net.fabricmc:fabric-loader:${props.fabric_loader_version}")

    when (loader) {
        Loader.COMMON -> {
            modCompileOnly("me.fallenbreath:conditional-mixin-common:${props.conditional_mixin_version}")
        }

        Loader.FABRIC -> {
            modLocalRuntime("maven.modrinth:early-loading-screen:${props.early_loading_screen_version}")

            modImplementation("net.fabricmc:fabric-loader:${props.fabric_loader_version}")

            include(modImplementation("me.fallenbreath:conditional-mixin-fabric:${props.conditional_mixin_version}")!!)

            modImplementation("net.fabricmc:fabric-language-kotlin:${props.fabric_kotlin_version}")
        }

        Loader.NEOFORGE -> {
            "neoForge"("net.neoforged:neoforge:${props.neoforge_version}")

            include(
                modImplementation("me.fallenbreath:conditional-mixin-neoforge:${props.conditional_mixin_version}")!!,
            )

            implementation("thedarkcolour:kotlinforforge-neoforge:${props.neoforge_kotlin_version}")
        }
    }
}

var versionSuffix = ""
// detect github action environment variables
// https://docs.github.com/en/actions/learn-github-actions/environment-variables#default-environment-variables
if (System.getenv("BUILD_RELEASE") != "true") {
    val buildNumber = System.getenv("BUILD_ID")
    versionSuffix += buildNumber?.let { "+build.$it" } ?: "-SNAPSHOT"
}
val fullModVersion = props.mod_version + versionSuffix

tasks.named<ProcessResources>("processResources") {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    val authors = props.mod_authors.joinToString(if (loader.isFabric) "\",\"" else ", ")

    // See https://minecraft.wiki/w/Pack_format#List_of_resource_pack_formats
    val resourcePackVersions = mapOf(
        12006 to "32",
        12101 to "34",
        12103 to "42",
        12104 to "46",
        12105 to "55",
        12106 to "63",
        12108 to "64",
        12110 to "69.0",
    )

    val replaceProperties = mapOf(
        "minecraft_version_range_fabric" to props.minecraft_version_range_fabric,
        "minecraft_version_range_neoforge" to props.minecraft_version_range_neoforge,
        "fabric_loader_version" to props.fabric_loader_version,
        "neoforge_version_range" to props.neoforge_version_range,
        "fabric_kotlin_version" to props.fabric_kotlin_version,
        "neoforge_kotlin_version" to props.neoforge_kotlin_version,
        "description" to props.mod_description,
        "homepage_url" to props.homepage_url,
        "sources_url" to props.sources_url,
        "issues_url" to props.issues_url,
        "mod_id" to props.mod_id,
        "mod_name" to props.mod_name,
        "version" to fullModVersion,
        "license" to props.license,
        "authors" to authors,
        "pack_format_number" to resourcePackVersions[mcVersion],
    )
    inputs.properties(replaceProperties)

    filesMatching(listOf("fabric.mod.json", "META-INF/neoforge.mods.toml", "pack.mcmeta")) {
        expand(replaceProperties + mapOf("project" to project))
    }

    if (!loader.isFabric) {
        exclude {
            it.file.name == "fabric.mod.json"
        }
    }
}

// https://github.com/Fallen-Breath/yamlang
yamlang {
    targetSourceSets = listOf(sourceSets.main.get())
    inputDir = "assets/${props.mod_id}/lang"
}

base {
    archivesName = "${props.archives_base_name}-mc${props.minecraft_version}-$loaderName"
}

version = "v$fullModVersion"
group = props.maven_group

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

kotlin {
    jvmToolchain(21)
}

java {
    withSourcesJar()
}

tasks.named<Jar>("jar") {
    from(rootProject.file("LICENSE")) {
        rename { "${it}_${props.archives_base_name}" }
    }
}

// configure the maven publication
publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            artifactId = base.archivesName.get()
            from(components["java"])
        }
    }

    // select the repositories you want to publish to
    repositories {
        // uncomment to publish to the local maven
        // mavenLocal()
    }
}
