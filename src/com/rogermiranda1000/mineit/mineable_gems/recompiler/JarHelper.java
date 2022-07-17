package com.rogermiranda1000.mineit.mineable_gems.recompiler;

import javax.annotation.Nullable;
import java.io.*;
import java.nio.file.Files;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

/**
 * Util functions to work with .jar
 */
public class JarHelper {
    private static final byte[] BUFFER = new byte[4096 * 1024];

    public static void copy(InputStream input, OutputStream output) throws IOException {
        int bytesRead;
        while ((bytesRead = input.read(JarHelper.BUFFER))!= -1) {
            output.write(JarHelper.BUFFER, 0, bytesRead);
        }
    }

    /**
     * Searches for a class inside a .jar
     *
     * @author https://stackoverflow.com/a/2265206/9178470
     * @author Roger Miranda
     *
     * @param jarPath   .jar path
     * @param classPath Class#getName()
     * @param javaFile  File
     * @return If the class classPath was found
     */
    public static boolean addClassToJar(String jarPath, String classPath, File javaFile) throws IOException {
        classPath = classPath.replace('.', '/')  + ".class";

        FileInputStream copy = new FileInputStream(javaFile);
        File newJarPath = new File(jarPath + ".tmp");
        File jarFile = new File(jarPath);
        ZipFile original = new ZipFile(jarFile);
        ZipOutputStream append = new ZipOutputStream(new FileOutputStream(newJarPath));

        boolean found = false;
        Enumeration<? extends ZipEntry> entries = original.entries();
        while (entries.hasMoreElements()) {
            ZipEntry e = entries.nextElement();
            if (!e.getName().equals(classPath)) {
                append.putNextEntry(e);
                if (!e.isDirectory()) JarHelper.copy(original.getInputStream(e), append);
                append.closeEntry();
            }
            else {
                // the one we're overriding
                e = new ZipEntry(classPath);
                append.putNextEntry(e);
                JarHelper.copy(copy, append);
                append.closeEntry();
                found = true;
            }
        }

        original.close();
        append.close();

        jarFile.delete();
        Files.move(newJarPath.toPath(), jarFile.toPath());

        return found;
    }

    public static boolean addClassToJar(String jarPath, String classPath, String javaFile) throws IOException {
        return JarHelper.addClassToJar(jarPath, classPath, new File(javaFile));
    }

    private static final HashMap<InputStream, ZipFile> openStreams = new HashMap<>(); // TODO open the same zip multiple times

    /**
     * Searches for a class inside a .jar
     * @param jarPath   .jar path
     * @param classPath Class#getName()
     * @return InputStream of the class; null if not found
     */
    @Nullable
    public static InputStream getClassFromFile(String jarPath, String classPath) {
        try {
            ZipFile zipFile = new ZipFile(jarPath);
            Enumeration<? extends ZipEntry> entries = zipFile.entries();

            classPath = classPath.replace('.', '/')  + ".class";

            while(entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (!entry.isDirectory() && entry.getName().equals(classPath)) {
                    InputStream is = zipFile.getInputStream(entry);
                    JarHelper.openStreams.put(is, zipFile);
                    return is;
                }
            }
        } catch (IOException ex) {
            //ex.printStackTrace();
        }
        return null;
    }

    /**
     * Close a InputStream returned by getClassFromFile
     */
    public static void closeInputStream(InputStream is) {
        if (is == null) return;

        ZipFile zip = JarHelper.openStreams.get(is);
        if (zip != null) {
            try {
                zip.close();
            } catch (IOException ignore) { }
        }
    }
}
