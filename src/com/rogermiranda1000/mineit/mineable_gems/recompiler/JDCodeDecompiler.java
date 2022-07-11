package com.rogermiranda1000.mineit.mineable_gems.recompiler;

import org.jd.core.v1.ClassFileToJavaSourceDecompiler;
import org.jd.core.v1.api.loader.Loader;
import org.jd.core.v1.api.loader.LoaderException;
import org.jd.core.v1.api.printer.Printer;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public class JDCodeDecompiler implements JavaDecompiler {
    public JDCodeDecompiler() {}

    @Override
    public String decompileClass(String jarPath, String className) throws Exception {
        String classPath = jarPath + "/" + className;
        ClassFileToJavaSourceDecompiler decompiler = new ClassFileToJavaSourceDecompiler();
        Printer printer = JDCodeDecompiler.getPrinter();
        Loader loader = JDCodeDecompiler.getLoader();

        decompiler.decompile(loader, printer, classPath);

        String source = printer.toString();
        source = "package " + className.substring(0, className.lastIndexOf('.')) + source.substring(source.indexOf(';')); // fix package

        return source;
    }

    private static Loader getLoader() {
        return new Loader() {
            @Override
            public byte[] load(String internalName) throws LoaderException {
                String jarPath = internalName.substring(0, internalName.lastIndexOf('/')),
                        classPath = internalName.substring(jarPath.length() + 1);
                InputStream is = JarHelper.getClassFromFile(jarPath,  classPath);

                if (is == null) {
                    return null;
                } else {
                    try (InputStream in = is; ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                        byte[] buffer = new byte[1024];
                        int read = in.read(buffer);

                        while (read > 0) {
                            out.write(buffer, 0, read);
                            read = in.read(buffer);
                        }

                        JarHelper.closeInputStream(is);
                        return out.toByteArray();
                    } catch (IOException e) {
                        JarHelper.closeInputStream(is);
                        throw new LoaderException(e);
                    }
                }
            }

            @Override
            public boolean canLoad(String internalName) {
                String jarPath = internalName.substring(0, internalName.lastIndexOf('/')),
                        classPath = internalName.substring(jarPath.length() + 1);
                InputStream is;
                if (!new File(jarPath).exists()) {
                    //System.err.println("The file " + jarPath + " doesn't exists!");
                    return false;
                }
                else if ((is = JarHelper.getClassFromFile(jarPath, classPath)) == null) {
                    //System.err.println("The class " + classPath + " couldn't be found inside " + jarPath);
                    return false;
                }
                JarHelper.closeInputStream(is);
                return true;
            }
        };
    }

    private static Printer getPrinter() {
        return new Printer() {
            protected static final String TAB = "  ";
            protected static final String NEWLINE = "\n";

            protected int indentationCount = 0;
            protected StringBuilder sb = new StringBuilder();

            @Override public String toString() { return sb.toString(); }

            @Override public void start(int maxLineNumber, int majorVersion, int minorVersion) {}
            @Override public void end() {}

            @Override public void printText(String text) { sb.append(text); }
            @Override public void printNumericConstant(String constant) { sb.append(constant); }
            @Override public void printStringConstant(String constant, String ownerInternalName) { sb.append(constant); }
            @Override public void printKeyword(String keyword) { sb.append(keyword); }
            @Override public void printDeclaration(int type, String internalTypeName, String name, String descriptor) { sb.append(name); }
            @Override public void printReference(int type, String internalTypeName, String name, String descriptor, String ownerInternalName) { sb.append(name); }

            @Override public void indent() { this.indentationCount++; }
            @Override public void unindent() { this.indentationCount--; }

            @Override public void startLine(int lineNumber) { for (int i=0; i<indentationCount; i++) sb.append(TAB); }
            @Override public void endLine() { sb.append(NEWLINE); }
            @Override public void extraLine(int count) { while (count-- > 0) sb.append(NEWLINE); }

            @Override public void startMarker(int type) {}
            @Override public void endMarker(int type) {}
        };
    }
}
