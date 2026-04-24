package org.polaris2023.gtu.space.init;

import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.objects.PhysicsRigidBody;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.logging.Level;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public final class SpaceBootstrap {
    private static final String LIB_VERSION = "23.0.0";
    private static final String BULLET_INIT_PROPERTY = "gtu.libbulletjme.initialized";
    private static boolean initialized;

    private SpaceBootstrap() {
    }

    public static synchronized void init() {
        if (initialized) {
            return;
        }
        if (Boolean.getBoolean(BULLET_INIT_PROPERTY)) {
            PhysicsRigidBody.logger2.setLevel(Level.OFF);
            PhysicsSpace.logger.setLevel(Level.OFF);
            initialized = true;
            return;
        }

        try {
            NativeBundle bundle = NativeBundle.detect();
            Path libraryPath = extractNativeLibrary(bundle);
            loadWithLibbulletjmeClassLoader(bundle, libraryPath);
            PhysicsRigidBody.logger2.setLevel(Level.OFF);
            PhysicsSpace.logger.setLevel(Level.OFF);
            System.setProperty(BULLET_INIT_PROPERTY, Boolean.TRUE.toString());
            initialized = true;
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to initialize GTU Space Bullet runtime", exception);
        }
    }

    private static Path extractNativeLibrary(NativeBundle bundle) throws IOException {
        Path cacheDir = Path.of(System.getProperty("java.io.tmpdir"), "gtu_space", "libbulletjme", LIB_VERSION);
        Files.createDirectories(cacheDir);

        Path extractedLibrary = cacheDir.resolve(bundle.loaderFileName());
        if (Files.exists(extractedLibrary)) {
            return extractedLibrary;
        }

        try (InputStream nestedJarStream = SpaceBootstrap.class.getClassLoader().getResourceAsStream(bundle.jarResource())) {
            if (nestedJarStream != null) {
                try (ZipInputStream zipInputStream = new ZipInputStream(nestedJarStream)) {
                    ZipEntry entry;
                    while ((entry = zipInputStream.getNextEntry()) != null) {
                        if (!bundle.libraryEntry().equals(entry.getName())) {
                            continue;
                        }
                        Files.copy(zipInputStream, extractedLibrary, StandardCopyOption.REPLACE_EXISTING);
                        return extractedLibrary;
                    }
                }
            }
        }

        try (InputStream directStream = SpaceBootstrap.class.getClassLoader().getResourceAsStream(bundle.libraryEntry())) {
            if (directStream != null) {
                Files.copy(directStream, extractedLibrary, StandardCopyOption.REPLACE_EXISTING);
                return extractedLibrary;
            }
        }

        throw new IOException("Missing native library " + bundle.libraryEntry());
    }

    private static void loadWithLibbulletjmeClassLoader(NativeBundle bundle, Path libraryPath) throws Exception {
        Class<?> loaderClass = Class.forName("com.jme3.system.NativeLibraryLoader");
        Method loadMethod = loaderClass.getMethod(
                "loadLibbulletjme", boolean.class, File.class, String.class, String.class
        );

        boolean loaded = (boolean) loadMethod.invoke(
                null,
                true,
                libraryPath.getParent().toFile(),
                bundle.buildType(),
                bundle.flavor()
        );

        if (!loaded) {
            throw new IOException("Libbulletjme native loader returned false for " + libraryPath);
        }
    }

    private record NativeBundle(
            String jarResource,
            String libraryEntry,
            String loaderFileName,
            String buildType,
            String flavor
    ) {
        private static NativeBundle detect() throws IOException {
            String osName = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
            String arch = System.getProperty("os.arch", "").toLowerCase(Locale.ROOT);

            if (osName.contains("win")) {
                return new NativeBundle(
                        "META-INF/jarjar/Libbulletjme-Windows64-23.0.0-SpDebug.jar",
                        "native/windows/x86_64/bulletjme.dll",
                        "Windows64DebugSp_bulletjme.dll",
                        "Debug",
                        "Sp"
                );
            }
            if (osName.contains("linux") && (arch.contains("amd64") || arch.contains("x86_64"))) {
                return new NativeBundle(
                        "META-INF/jarjar/Libbulletjme-Linux64-23.0.0-SpDebug.jar",
                        "native/linux/x86_64/libbulletjme.so",
                        "Linux64DebugSp_libbulletjme.so",
                        "Debug",
                        "Sp"
                );
            }
            if (osName.contains("mac") && (arch.contains("aarch64") || arch.contains("arm64"))) {
                return new NativeBundle(
                        "META-INF/jarjar/Libbulletjme-MacOSX_ARM64-23.0.0-SpDebug.jar",
                        "native/osx/arm64/libbulletjme.dylib",
                        "MacOSX_ARM64DebugSp_libbulletjme.dylib",
                        "Debug",
                        "Sp"
                );
            }

            throw new IOException("Unsupported platform for bulletjme: os=" + osName + ", arch=" + arch);
        }
    }
}
