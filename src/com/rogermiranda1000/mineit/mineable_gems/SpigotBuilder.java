package com.rogermiranda1000.mineit.mineable_gems;

import com.rogermiranda1000.helper.RogerPlugin;
import com.rogermiranda1000.versioncontroller.VersionController;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;

import javax.annotation.Nullable;
import java.io.*;
import java.net.URL;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SpigotBuilder {
    private static final String DOWNLOAD_URL = "https://hub.spigotmc.org/jenkins/job/BuildTools/lastSuccessfulBuild/artifact/target/BuildTools.jar";

    public static void build(final String version, final File out, final Runnable onBuild, final RogerPlugin plugin) {
        final File tmpDir = new File(UUID.randomUUID().toString());
        tmpDir.mkdir();

        Bukkit.getScheduler().runTaskAsynchronously(plugin, ()-> {
            try {
                SpigotBuilder.download(DOWNLOAD_URL, new File(tmpDir.getPath() + File.separatorChar + "BuildTools.jar"));

                Process p = Runtime.getRuntime().exec("java -jar BuildTools.jar --compile craftbukkit --rev " + VersionController.version.toString(), null, tmpDir); // compile in tmpDir

                BukkitTask t = Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> plugin.getLogger().info("Compiling... please wait."));

                BufferedReader stdError = new BufferedReader(new InputStreamReader(p.getErrorStream()));
                final List<String> errors = stdError.lines().collect(Collectors.toList());
                t.cancel();

                if (!new File(tmpDir.getPath() + File.separatorChar + "spigot-" + version + ".jar").renameTo(out)) {
                    // rename failed, maybe it failed the compilation?
                    SpigotBuilder.deleteDir(tmpDir);
                    if (errors.size() > 0) throw new BuildToolsFailedException(errors);
                }
                //SpigotBuilder.deleteDir(tmpDir);

                onBuild.run();
            } catch (BuildToolsFailedException ex) {
                plugin.printConsoleErrorMessage("Detected error while building spigot");
                ex.printStackTrace();
                plugin.printConsoleWarningMessage("Contact with the plugin author or place spigot 1.16.5 into plugins/MineIt-MineableGems (with the name 'spigot.jar')");
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    /**
     * @return JAVA8 for <1.17, JAVA16 for 1.17, JAVA17 for >1.17
     */
    private static int javaVersion(String spigotVersion) {
        if (spigotVersion.startsWith("1.17")) return 16;
        if (Integer.parseInt(spigotVersion.split("\\.")[1]) < 17) return 8;
        return 17;
    }

    private static void deleteDir(File file) {
        File[] contents = file.listFiles();
        if (contents != null) {
            for (File f : contents) {
                deleteDir(f);
            }
        }
        file.delete();
    }

    /**
     * @author https://www.baeldung.com/java-download-file
     */
    private static void download(String url, File out) throws IOException {
        try (BufferedInputStream in = new BufferedInputStream(new URL(url).openStream());
             FileOutputStream fileOutputStream = new FileOutputStream(out)) {
            byte []dataBuffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = in.read(dataBuffer, 0, 1024)) != -1) {
                fileOutputStream.write(dataBuffer, 0, bytesRead);
            }
        }
    }
}
