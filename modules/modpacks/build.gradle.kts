import org.gradle.nativeplatform.platform.internal.DefaultNativePlatform

dependencies {

    // 优化类
    localRuntime("maven.modrinth:acceleratedrendering:1.0.8-1.21.1-alpha")
    localRuntime("maven.modrinth:accelerated-recoiling:v21.1.11-alpha-hotfix")

    implementation("com.simibubi.create:create-1.21.1:6.0.10-272:slim") {
        exclude("info.journeymap")
        exclude("mezz.jei")
    }
    implementation("net.createmod.ponder:ponder-neoforge:1.0.82+mc1.21.1")
    implementation("com.gregtechceu.gtceu:gtceu-1.21.1:7.0.2") {
        exclude("dev.toma.configuration")
    }
//    api("dev.toma.configuration:configuration-1.21.1:3.1.0-neoforge")
    compileOnly("mezz.jei:jei-1.21.1-neoforge:19.27.0.340")
    localRuntime("mezz.jei:jei-1.21.1-neoforge:19.27.0.340")
    implementation(project(":core"))
    localRuntime("dev.ftb.mods:ftb-quests-neoforge:2101.1.24")
    localRuntime("curse.maven:commandstructures-565119:6428806")

    implementation("curse.maven:lithostitched-936015:7832789")
    localRuntime("curse.maven:tectonic-686836:7903156")
    localRuntime("curse.maven:terralith-513688:6090387")
    // implementation("curse.maven:rhenium-1497566:7827201")

//    implementation(project(":space"))
//    implementation(project(":physics"))

//    val system = DefaultNativePlatform.getCurrentOperatingSystem()
//    if (system.isWindows()) {
//        additionalRuntimeClasspath("com.github.stephengold:Libbulletjme-Windows64:23.0.0")
//    } else if (system.isLinux()) {
//        additionalRuntimeClasspath("com.github.stephengold:Libbulletjme-Linux64:23.0.0")
//    } else if (system.isMacOsX()) {
//        additionalRuntimeClasspath("com.github.stephengold:Libbulletjme-MacOSX_ARM64:23.0.0")
//    }

//    implementation("curse.maven:simply-improved-terrain-472872:6112229")

    //    implementation("com.github.glitchfiend:TerraBlender-neoforge:1.21.1-4.1.0.5") //移除tb我们有更好的
}
