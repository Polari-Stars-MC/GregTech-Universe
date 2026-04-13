plugins {
    `java-library`
    `maven-publish`
    id("net.neoforged.moddev") version("2.0.141")
    idea
    base
}

tasks.named<Wrapper>("wrapper").configure {
    distributionType = Wrapper.DistributionType.BIN
}

val parchmentMinecraftVersion: String by rootProject
val parchmentMappingsVersion: String by rootProject

val minecraftVersion: String by rootProject
val minecraftVersionRange: String by rootProject
val neoVersion: String by rootProject
val loaderVersionRange: String by rootProject


val modLicense: String by rootProject
val modVersion: String by rootProject
val modGroupId: String by rootProject

version = modVersion
group = modGroupId

allprojects {
    if (project == rootProject) {
        return@allprojects
    }
    repositories {
        maven { url = uri("https://repo.repsy.io/mvn/toma/public/") }
        maven { url = uri("https://maven.createmod.net:6085/") }
        maven { url = uri("https://mvn.devos.one/snapshots") }
        maven { url = uri("https://maven.blamejared.com/") }
        maven { url = uri("https://maven.shedaniel.me/") }
        maven { url = uri("https://modmaven.dev") }
        maven { url = uri("https://api.modrinth.com/maven") }
        maven { url = uri("https://maven.ftb.dev/releases") }
        maven { url = uri("https://maven.squiddev.cc") }
        maven { url = uri("https://maven.theillusivec4.top/") }
        maven { url = uri("https://www.cursemaven.com") }
        maven { url = uri("https://maven.gtceu.com") }
        maven { url = uri("https://maven.firstdark.dev/snapshots") }
        maven { url = uri("https://jitpack.io") }
        maven { url = uri("https://maven.terraformersmc.com/releases") }

        maven { url = uri("https://nexus.resourcefulbees.com/repository/maven-public/") }
        maven { url = uri("https://maven.tamaized.com/releases") }
        maven { url = uri("https://dl.cloudsmith.io/public/geckolib3/geckolib/maven/") }
        maven { url = uri("https://maven.wispforest.io/releases") }

//        maven {
//            url = uri("https://maven.minecraftforge.net/")
//
//        }
    }

    dependencies {

    }
}

allprojects {
    if (project == rootProject) {
        return@allprojects
    }
    val modId: String by project
    val modName: String by project

    apply(plugin = "net.neoforged.moddev")
    apply(plugin = "maven-publish")
    apply(plugin = "java-library")
    apply(plugin = "idea")
    apply(plugin = "base")

    base {
        archivesName = modId
    }

    java.toolchain.languageVersion = JavaLanguageVersion.of(21)

    neoForge {
        version = neoVersion
        parchment {
            mappingsVersion = parchmentMappingsVersion
            minecraftVersion.set(parchmentMinecraftVersion)
        }
        accessTransformers.from(rootProject.file("src/res/${project.name}/META-INF/accesstransformer.cfg"))
        runs {
            register("client") {
                client()
                gameDirectory = layout.buildDirectory.file("runs/client").get().asFile
                jvmArgument("-Dmixin.debug.export=true")
                systemProperty("neoforge.enabledGameTestNamespaces", modId)
            }

            register("server") {
                server()
                programArgument("--nogui")
                jvmArgument("-Dmixin.debug.export=true")
                gameDirectory = layout.buildDirectory.file("runs/server").get().asFile
                systemProperty("neoforge.enabledGameTestNamespaces", modId)
            }

            register("gameTestServer") {
                type = "gameTestServer"
                gameDirectory = layout.buildDirectory.file("runs/gts").get().asFile
                systemProperty("neoforge.enabledGameTestNamespaces", modId)
            }

            register("data") {
                data()
                gameDirectory = layout.buildDirectory.file("runs/client").get().asFile
                programArguments.addAll(listOf(
                    "--mod", modId,
                    "--all",
                    "--output", layout.buildDirectory.file("src/generated/").get().asFile.absolutePath,
                    "--existing", rootProject.file("src/res/${project.name}").absolutePath,
                ))
            }

            configureEach {
                systemProperty("forge.logging.markers", "REGISTRIES")
                logLevel = org.slf4j.event.Level.DEBUG
            }
        }

        mods {
            register(modId) {
                sourceSet(sourceSets.main.get())
            }
        }
    }

    val localRuntime by configurations.registering
    configurations.runtimeClasspath.get().extendsFrom(localRuntime.get())

    val commonMixinConfig = rootProject.file("src/res/${project.name}/${modId}.mixins.json")
    val clientMixinConfig = rootProject.file("src/res/${project.name}/${modId}.client.mixins.json")
    val mixinBlocks = buildString {
        if (commonMixinConfig.exists()) {
            appendLine("# The [[mixins]] block allows you to declare your mixin config to FML so that it gets loaded.")
            appendLine("[[mixins]]")
            appendLine("config=\"${modId}.mixins.json\"")
        }
        if (clientMixinConfig.exists()) {
            if (isNotEmpty()) {
                appendLine()
            }
            appendLine("# Client-only mixins")
            appendLine("[[mixins]]")
            appendLine("config=\"${modId}.client.mixins.json\"")
        }
        if (isNotEmpty()) {
            appendLine()
        }
    }

    var generateModMetadata = tasks.register<ProcessResources>("generateModMetadata") {
        var replaceProperties = mapOf(
            "minecraft_version" to minecraftVersion,
            "minecraft_version_range" to minecraftVersionRange,
            "neo_version" to neoVersion,
            "loader_version_range" to loaderVersionRange,
            "mod_id" to modId,
            "mod_name" to modName,
            "mod_version" to modVersion,
            "mod_license" to modLicense,
            "mixin_blocks" to mixinBlocks,
        )
        inputs.properties(replaceProperties)
        expand(replaceProperties)
        from(rootProject.file("src/main/templates"))
        into(layout.buildDirectory.dir("generated/sources/modMetadata").get().asFile.absolutePath)
    }

    sourceSets {
        main {
            java {
                srcDir(rootProject.file("src/main/${project.name}"))
            }
            resources {
                srcDir(layout.buildDirectory.file("src/generated/").get().asFile)
                srcDir(rootProject.file("src/res/${project.name}"))
                srcDir(generateModMetadata.get())
                exclude("**/*.bbmodel")
                exclude("src/generated/**/.cache")
            }
        }
    }


    neoForge.ideSyncTask(generateModMetadata.get())

    tasks.withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
    }

    idea {
        module {
            isDownloadJavadoc = true
            isDownloadSources = true
        }
    }

}

//publishing {
//    publications {
//        register('mavenJava', MavenPublication) {
//            from components.java
//        }
//    }
//    repositories {
//        maven {
//            url "file://${project.projectDir}/repo"
//        }
//    }
//}

