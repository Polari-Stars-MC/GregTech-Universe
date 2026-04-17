
import org.gradle.nativeplatform.platform.internal.DefaultNativePlatform

dependencies {
    implementation("com.github.stephengold:sport:0.9.9") {
        exclude("org.lwjgl")
        exclude("org.joml")
    }

    jarJar("com.github.stephengold:sport:0.9.9")


    implementation("io.github.electrostat-lab:snaploader:1.1.1-stable")
    jarJar("io.github.electrostat-lab:snaploader:1.1.1-stable")

    implementation("com.github.stephengold:Libbulletjme-Windows64:23.0.0")
    jarJar("com.github.stephengold:Libbulletjme-Windows64:23.0.0:SpDebug")
    jarJar("com.github.stephengold:Libbulletjme-Linux64:23.0.0:SpDebug")
    jarJar("com.github.stephengold:Libbulletjme-MacOSX_ARM64:23.0.0:SpDebug")

    var system = DefaultNativePlatform.getCurrentOperatingSystem()
    if (system.isWindows()) {
        additionalRuntimeClasspath("com.github.stephengold:Libbulletjme-Windows64:23.0.0:SpDebug")

    } else if (system.isLinux()) {
        additionalRuntimeClasspath("com.github.stephengold:Libbulletjme-Linux64:23.0.0:SpDebug")
    } else if(system.isMacOsX()) {
        additionalRuntimeClasspath("com.github.stephengold:Libbulletjme-MacOSX_ARM64:23.0.0:SpDebug")
    }
    additionalRuntimeClasspath("io.github.electrostat-lab:snaploader:1.1.1-stable")
    additionalRuntimeClasspath("com.github.stephengold:Libbulletjme-Windows64:23.0.0")

}