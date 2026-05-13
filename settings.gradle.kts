pluginManagement {
    repositories {
        gradlePluginPortal()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version("1.0.0")
}

val modules = listOf("core", "modpacks", "physics")

for (it in modules) {
    include(it)
    var f = file("modules/$it")
    f.mkdirs()
    project(":$it").projectDir = f
}

// CHK now provides the pack's space gameplay.
includeBuild("../chk黑洞修复")

