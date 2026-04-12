
import org.gradle.nativeplatform.platform.internal.DefaultNativePlatform

dependencies {
    implementation(project(":physics"))
    additionalRuntimeClasspath("io.github.electrostat-lab:snaploader:1.1.1-stable")
    additionalRuntimeClasspath("com.github.stephengold:Libbulletjme-Windows64:23.0.0")
    var system = DefaultNativePlatform.getCurrentOperatingSystem()
    if (system.isWindows()) {
        additionalRuntimeClasspath("com.github.stephengold:Libbulletjme-Windows64:23.0.0:SpDebug")

    } else if (system.isLinux()) {
        additionalRuntimeClasspath("com.github.stephengold:Libbulletjme-Linux64:23.0.0:SpDebug")
    } else if(system.isMacOsX()) {

        additionalRuntimeClasspath("com.github.stephengold:Libbulletjme-MacOSX_ARM64:23.0.0:SpDebug")
    }
}