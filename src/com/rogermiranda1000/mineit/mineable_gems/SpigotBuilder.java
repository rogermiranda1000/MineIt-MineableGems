package com.rogermiranda1000.mineit.mineable_gems;

import java.io.*;
import java.net.URL;
import java.util.UUID;
import java.util.stream.Stream;

public class SpigotBuilder {
    private static final String DOWNLOAD_URL = "https://hub.spigotmc.org/jenkins/job/BuildTools/lastSuccessfulBuild/artifact/target/BuildTools.jar";

    public static void build(String version, File out) throws IOException {
        File tmpDir = new File(UUID.randomUUID().toString());
        tmpDir.mkdir();

        SpigotBuilder.download(DOWNLOAD_URL, new File(tmpDir.getPath() + File.separatorChar + "BuildTools.jar"));

        Process p = Runtime.getRuntime().exec("java -jar BuildTools.jar --rev 1.16.5", null, tmpDir); // compile in tmpDir

        BufferedReader stdError = new BufferedReader(new InputStreamReader(p.getErrorStream()));
        Stream<String> errors = stdError.lines();
        System.err.println("Detected error while building spigot");
        errors.forEach(l -> System.err.println(l));

        new File(tmpDir.getPath() + File.separatorChar + "spigot-" + version + ".jar").renameTo(out);
        //SpigotBuilder.deleteDir(tmpDir);
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
