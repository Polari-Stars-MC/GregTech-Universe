import org.gradle.nativeplatform.platform.internal.DefaultNativePlatform

dependencies {
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
    implementation("io.github.electrostat-lab:snaploader:1.1.1-stable")
    jarJar("io.github.electrostat-lab:snaploader:1.1.1-stable")
    additionalRuntimeClasspath("io.github.electrostat-lab:snaploader:1.1.1-stable")
    additionalRuntimeClasspath("com.github.stephengold:Libbulletjme-Windows64:23.0.0")

    compileOnly("team-twilight:twilightforest:4.8.3348:universal")
//    localRuntime("team-twilight:twilightforest:4.8.3348:universal")

    compileOnly("curse.maven:aether-255308:7043502")
//    localRuntime("curse.maven:aether-255308:7043502")
//
//    localRuntime("io.wispforest:accessories-neoforge:1.1.0-beta.53+1.21.1") {
//        exclude(group = "org.sinytra.forgified-fabric-api", module = "fabric-api-base")
//    }
    compileOnly("curse.maven:eternal-starlight-1080592:7788339")
//    localRuntime("curse.maven:eternal-starlight-1080592:7788339")
    compileOnly("org.appliedenergistics:appliedenergistics2:19.2.17")
    localRuntime("org.appliedenergistics:appliedenergistics2:19.2.17")


}