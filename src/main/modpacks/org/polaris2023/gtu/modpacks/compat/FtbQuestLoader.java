package org.polaris2023.gtu.modpacks.compat;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.loading.FMLConfig;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import org.polaris2023.gtu.modpacks.GregtechUniverseModPacks;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Stream;

import static org.polaris2023.gtu.modpacks.GregtechUniverseModPacks.MOD_ID;

@EventBusSubscriber(modid = MOD_ID)
public class FtbQuestLoader {


    private static final Logger LOGGER = LoggerFactory.getLogger("GTU Quests");
    private static final String QUEST_RESOURCE_PATH = "data/" + MOD_ID + "/ftbquests/quests";
    private static final Path CONFIG_PATH = FMLPaths.CONFIGDIR.get().resolve("ftbquests/quests");

    @SubscribeEvent
    public static void configLoader(ServerStartingEvent event) {
        try {
            Set<String> files = findAllResourceFiles();
            if (files.isEmpty()) {
                LOGGER.warn("Don't find FTB Quest data config.");
                return;
            }
            int copied = 0;
            for (String file : files) {
                if (copyResource(file)) {
                    copied++;
                }
            }
            LOGGER.info("FTB Quest config Loaded, Copied {} / {} files", copied, files.size());
        } catch (IOException e) {
            LOGGER.error("Fail Load FTB Quest config.", e);
        }
    }

    @SubscribeEvent
    public static void saveToDevelopment(ServerStoppingEvent event) {
        if (!FMLEnvironment.production && MOD_ID.equals(System.getProperty("neoforge.enabledGameTestNamespaces"))) {
            Path ftbEnvPath = Paths.get(System.getProperty("user.dir"))
                    .getParent()
                    .getParent()
                    .getParent()
                    .getParent()
                    .getParent()
                    .resolve("src/res/modpacks/" + QUEST_RESOURCE_PATH);
            try(Stream<Path> walk = Files.walk(CONFIG_PATH)) {

                walk
                        .filter(Files::isRegularFile)
                        .filter(p -> p.toString().endsWith(".snbt"))
                        .forEach(p -> {
                            Path relativize = CONFIG_PATH.relativize(p);
                            Path resolve = ftbEnvPath.resolve(relativize);
                            try {
                                Files.createDirectories(resolve.getParent());
                                Files.copy(p,resolve,StandardCopyOption.REPLACE_EXISTING);
                            } catch (IOException ignored) {}

                        });
            } catch (IOException ignored) {
            }

            LOGGER.info(ftbEnvPath.toFile().getAbsolutePath());

        }
    }


    private static boolean copyResource(String resourcePath) {
        String relativePath = resourcePath.substring(QUEST_RESOURCE_PATH.length() + 1);
        Path target = CONFIG_PATH.resolve(relativePath);
        try(InputStream is = FtbQuestLoader.class.getClassLoader()
                .getResourceAsStream(resourcePath)) {
            if (is == null) {
                LOGGER.warn("{} Resources is null", resourcePath);
                return false;
            }
            Files.createDirectories(target.getParent());
            if (!Files.exists(target)) {
                Files.copy(is, target, StandardCopyOption.REPLACE_EXISTING);
                LOGGER.debug("Copied: {}", relativePath);
                return true;
            }
            return false;
        } catch (IOException e) {
            LOGGER.error("Fail copied {}", resourcePath, e);
            return false;
        }

    }

    private static Set<String> findAllResourceFiles() throws IOException {
        Set<String> files = new HashSet<>();
        Enumeration<URL> resources = FtbQuestLoader.class.getClassLoader()
                .getResources(QUEST_RESOURCE_PATH);
        while (resources.hasMoreElements()) {
            URL url = resources.nextElement();
            String protocol = url.getProtocol();
            LOGGER.info("Scanning protocol: {}", protocol);

            if (protocol.equals("jar")) {
                scanJarFiles(url, files);
            } else if (protocol.equals("union")) {
                scanUnionFiles(url, files);
            } else if (protocol.equals("file")) {
                scanDirectoryFiles(Paths.get(url.getPath()), files);
            } else {
                LOGGER.warn("Unknown protocol: {}", protocol);
            }
        }
        return files;
    }

    private static void scanDirectoryFiles(Path directory, Set<String> files) throws IOException {
        if (!Files.exists(directory)) {
            return;
        }

        try(Stream<Path> walk = Files.walk(directory)) {
            walk
                    .filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".snbt"))
                    .forEach(p -> {
                        String relativePath = directory.relativize(p).toString().replace(File.pathSeparator, "/");
                        LOGGER.info("ScanDirFiles {}", relativePath);
                        files.add(QUEST_RESOURCE_PATH + "/" + relativePath);
                    });
        }
    }

    private static void scanJarFiles(URL url, Set<String> files) throws IOException {
        JarURLConnection jarConn = (JarURLConnection) url.openConnection();
        try(JarFile jf = jarConn.getJarFile()) {
            Enumeration<JarEntry> entries = jf.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String name = entry.getName();
                LOGGER.info("scanJarFile {}", name);
                if (name.startsWith(QUEST_RESOURCE_PATH) && name.endsWith(".snbt") && !entry.isDirectory()) {
                    files.add(name);
                }
            }
        }
    }

    private static void scanUnionFiles(URL url, Set<String> files) throws IOException {
        // union 协议是 NeoForge 开发环境使用的联合文件系统
        // 尝试将其转换为 URI 并作为目录扫描
        try {
            URI uri = url.toURI();
            Path path = Paths.get(uri);
            if (Files.isDirectory(path)) {
                scanDirectoryFiles(path, files);
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to scan union URL: {}", url, e);
        }
    }


}
