import gg.essential.gradle.util.noServerRunConfigs

plugins {
    kotlin("jvm")
    id("gg.essential.multi-version")
    id("gg.essential.defaults")
}

val modGroup: String by project
val modBaseName: String by project
group = modGroup
base.archivesName.set("$modBaseName-${platform.mcVersionStr}")

val accessTransformerName = "patcher1${platform.mcMinor}_at.cfg"

loom {
    noServerRunConfigs()
    forge {
        accessTransformer(rootProject.file("src/main/resources/$accessTransformerName"))
    }
    mixin {
        defaultRefmapName.set("patcher.mixins.refmap.json")
    }
    runConfigs {
        getByName("client") {
            property("fml.coreMods.load", "club.sk1er.patcher.tweaker.PatcherTweaker")
            property("patcher.debugBytecode", "true")
            property("mixin.debug.verbose", "true")
            property("mixin.debug.export", "true")
            property("mixin.dumpTargetOnFailure", "true")
            programArgs("--tweakClass", "org.spongepowered.asm.launch.MixinTweaker")
            programArgs("--mixin", "patcher.mixins.json")
        }
    }
}

repositories {
    maven("https://repo.essential.gg/repository/maven-public/")
    maven("https://repo.spongepowered.org/repository/maven-public/")
}

val embed by configurations.creating
configurations.implementation.get().extendsFrom(embed)

dependencies {
    embed("gg.essential:universalcraft-1.8.9-forge:394")
    embed("gg.essential:elementa:700")
    embed("gg.essential:vigilance:306")
    embed("io.github.llamalad7:mixinextras-common:0.4.1")
    embed("com.github.char:Koffee:88ba1b0") {
        isTransitive = false
    }
    embed("com.github.ben-manes.caffeine:caffeine:2.9.1")
    embed("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
    embed("org.spongepowered:mixin:0.7.11-SNAPSHOT") {
        isTransitive = false
    }
    embed("org.ow2.asm:asm:9.6")
    embed("org.ow2.asm:asm-commons:9.6")
    embed("com.google.guava:guava:33.0.0-jre")
}

tasks.compileKotlin {
    kotlinOptions {
        freeCompilerArgs += listOf("-Xopt-in=kotlin.RequiresOptIn", "-Xno-param-assertions", "-Xjvm-default=all-compatibility")
    }
}

tasks.processResources {
    rename("(.+_at.cfg)", "META-INF/$1")
}

tasks.jar {
    from(embed.files.map { zipTree(it) })
    exclude("META-INF/*.SF")
    exclude("META-INF/*.DSA")
    exclude("META-INF/*.RSA")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    manifest.attributes(mapOf(
        "FMLCorePlugin" to "club.sk1er.patcher.tweaker.PatcherTweaker",
        "ModSide" to "CLIENT",
        "FMLAT" to accessTransformerName,
        "FMLCorePluginContainsFMLMod" to "Yes, yes it does",
        "Main-Class" to "club.sk1er.container.ContainerMessage",
        "TweakClass" to "org.spongepowered.asm.launch.MixinTweaker",
        "TweakOrder" to "0",
        "MixinConfigs" to "patcher.mixins.json",
        "ForceLoadAsMod" to "true"
    ))
}