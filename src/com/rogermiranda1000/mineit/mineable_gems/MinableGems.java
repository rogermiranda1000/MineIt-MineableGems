package com.rogermiranda1000.mineit.mineable_gems;

import com.rogermiranda1000.mineit.ListenerNotFoundException;
import com.rogermiranda1000.mineit.MineIt;
import com.rogermiranda1000.mineit.MineItApi;
import com.rogermiranda1000.mineit.mineable_gems.events.BreakEventListener;
import com.rogermiranda1000.mineit.mineable_gems.recompiler.CompileException;
import com.rogermiranda1000.mineit.mineable_gems.recompiler.Error;
import com.rogermiranda1000.mineit.mineable_gems.recompiler.JarHelper;
import com.rogermiranda1000.mineit.mineable_gems.recompiler.MatchNotFoundException;
import me.Mohamad82.MineableGems.Core.DropReader;
import me.Mohamad82.MineableGems.Events.BreakEvent;
import me.Mohamad82.MineableGems.Main;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredListener;
import org.bukkit.plugin.java.JavaPlugin;
import org.jd.core.v1.ClassFileToJavaSourceDecompiler;
import org.jd.core.v1.api.loader.Loader;
import org.jd.core.v1.api.loader.LoaderException;
import org.jd.core.v1.api.printer.Printer;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.Callable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MinableGems extends JavaPlugin {
    public void printConsoleErrorMessage(String msg) {
        Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "[" + this.getName() + "] " + msg);
    }

    public void printConsoleWarningMessage(String msg) {
        Bukkit.getConsoleSender().sendMessage(ChatColor.YELLOW + "[" + this.getName() + "] " + msg);
    }

    @Override
    public void onEnable() {
        /* Recompile MineableGems */
        try {
            String jarPath = "plugins/MineableGems-1.11.3.jar"; // TODO get name
            String className = DropReader.class.getName();
            String classPath = jarPath + "/" + className;

            ClassFileToJavaSourceDecompiler decompiler = new ClassFileToJavaSourceDecompiler();
            Printer printer = MinableGems.getPrinter();
            Loader loader = MinableGems.getLoader();

            decompiler.decompile(loader, printer, classPath); // TODO invalid package

            String source = printer.toString();
            // wrong package
            source = "package " + className.substring(0, className.lastIndexOf('.')) + source.substring(source.indexOf(';'));
            Pattern functionPattern = Pattern.compile("public\\s+CustomDrop\\s+readCustomDrop\\s*\\(ConfigurationSection ([^,)]+)[^)]*\\)\\s*\\{\\s*CustomDrop customDrop");
            Matcher matcher = functionPattern.matcher(source);
            if (!matcher.find()) throw new MatchNotFoundException();
            source = source.substring(0,matcher.end()) + " = null; plugin.getLogger().info(\"HEYYY!!!\")" + source.substring(matcher.end()); // append at the middle

            //System.out.println(matcher.group(1)); // you can send "variables" using the RegEx
            //System.out.println(source);

            final File out = new File(className.substring(className.lastIndexOf('.')+1) + ".java");
            FileWriter writer = new FileWriter(out);
            writer.write(source);
            writer.close();
            File compiled = new File(className.substring(className.lastIndexOf('.')+1) + ".class"); // the output file have the same name, but .class

            try {
                System.out.println("Recompiling " + className + "...");
                Callable<Error[]> compile = () -> {
                    try {
                        return MinableGems.compile(out.getName(), new String[]{"spigot-1.8.jar", "plugins/MineableGems-1.11.3.jar"}, "1.8"); // TODO classpath
                    } catch (IOException e) {
                        e.printStackTrace();
                        return new Error[]{};
                    }
                };
                Error[] errors = compile.call();
                if (errors.length > 0) {
                    // try to solve the cast errors
                    for (Error e : errors) {
                        int index = MinableGems.ordinalIndexOf(source, "\n", e.getLine() - 1);
                        if (index == -1) throw new CompileException("Line " + e.getLine() + " not found.", e);
                        //System.out.println(source.substring(index, index+100));

                        Matcher m = e.getPattern().matcher(source.substring(index));
                        if (!m.find()) throw new CompileException("Can't find error position.", e);
                        int insertFixIndex = source.substring(0, index + m.end()).lastIndexOf('(') + 1;
                        source = source.substring(0, insertFixIndex) + "(List<String>)" + source.substring(insertFixIndex);
                    }

                    // change the file with errors
                    writer = new FileWriter(out);
                    writer.write(source);
                    writer.close();

                    errors = compile.call();
                    if (errors.length > 0) throw new CompileException("Unable to solve the errors.", errors);
                }

                if (!JarHelper.addClassToJar(jarPath, className, compiled.getName())) throw new CompileException("Class " + className + " not found inside " + jarPath);
            } catch (CompileException | IOException ex) {
                out.delete();
                if (compiled.exists()) compiled.delete();
                throw ex;
            } finally {
                out.delete();
                if (compiled.exists()) compiled.delete();
            }
        } catch (Exception ex) {
            this.printConsoleErrorMessage("Error while recompiling MineableGems");
            ex.printStackTrace();
        }

        // load after MineableGems
        Bukkit.getScheduler().scheduleSyncDelayedTask(this, ()-> {
            PluginManager pm = getServer().getPluginManager();
            Plugin mineableGems = pm.getPlugin("MineableGems");
            Main mineableGemsObject = Main.getInstance();
            MineItApi mineItObject = MineItApi.getInstance();

            // override BlockBreakEvent
            try {
                final Method mineableGemsOnBreakMethod = MinableGems.overrideListener(mineableGems, BreakEvent.class, "onBlockBreak");
                final Listener mineableGemsBreakListener = MinableGems.getListener(mineableGems, BreakEvent.class);
                this.getServer().getPluginManager().registerEvents(
                        new BreakEventListener((e) -> {
                            try {
                                mineableGemsOnBreakMethod.invoke(mineableGemsBreakListener, e);
                            } catch (IllegalAccessException | InvocationTargetException ex) {
                                ex.printStackTrace();
                            }
                        }, mineItObject, mineableGemsObject), this);
            } catch (ListenerNotFoundException ex) {
                ex.printStackTrace();
            }
        }, 1);
    }

    /**
     * @author https://stackoverflow.com/a/3976656/9178470
     * @return Index of the nth occurance of substr (or -1 if not found)
     */
    private static int ordinalIndexOf(String str, String substr, int n) {
        int pos = str.indexOf(substr);
        while (--n > 0 && pos != -1) pos = str.indexOf(substr, pos + 1);
        return pos;
    }

    /**
     * Compile a .java and get the errors
     * @param javaFilePath .java path
     * @param classpaths    Dependencies
     * @param version       Java compile version (1.8 for Java 8)
     * @return Errors; the compiled class is in ./<javaFilePath name>.class
     */
    private static Error []compile(String javaFilePath, String []classpaths, String version) throws IOException {
        Process p = Runtime.getRuntime().exec("javac -source " + version + " -target " + version + " -classpath " + ((classpaths.length == 0) ? "." : (".:" + String.join(":", classpaths))) + " " + javaFilePath); // compile
        BufferedReader stdError = new BufferedReader(new InputStreamReader(p.getErrorStream()));
        return Error.getErrors(stdError);
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

    private static Method overrideListener(final Plugin plugin, Class<?> match, String name) throws ListenerNotFoundException {
        final Listener lis = MinableGems.getListener(plugin, match);
        if (lis == null) throw new ListenerNotFoundException("Unable to override " + plugin.getName() + " event priority: Listener not found");

        HandlerList.unregisterAll(lis); // all the RegisteredListener on reload are the same Listener

        Method r = null;
        for (final Method m: match.getDeclaredMethods()) {
            // is it an event?
            if (m.getParameterCount() != 1) continue;
            if (!Event.class.isAssignableFrom(m.getParameterTypes()[0])) continue;
            EventHandler eventHandler = m.getAnnotation(EventHandler.class);
            if (eventHandler == null) continue;

            // register again the event, but with the desired priority
            if (m.getName().equals(name)) r = m;
            else {
                final Class<? extends Event> type = m.getParameterTypes()[0].asSubclass(Event.class);
                Bukkit.getPluginManager().registerEvent(type, lis, eventHandler.priority(), (l, e) -> {
                    try {
                        try {
                            m.invoke(l, type.cast(e));
                        } catch (ClassCastException ignore) {}
                    } catch (SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
                        MineIt.instance.printConsoleErrorMessage("Error while overriding " + plugin + " event (" + lis.getClass().getName() + "#" + m.getName() + ")");
                        ex.printStackTrace();
                        MineIt.instance.printConsoleErrorMessage("Protection override failure. Notice this may involve players being able to remove protected regions, so report this error immediately and use an older version of MineIt.");
                    }
                }, plugin, eventHandler.ignoreCancelled());
            }
        }

        if (r == null) throw new ListenerNotFoundException();
        return r;
    }

    private static Listener getListener(Plugin plugin, Class<?> match) {
        Listener lis = null;
        for (RegisteredListener l : HandlerList.getRegisteredListeners(plugin)) {
            if (l.getListener().getClass().equals(match)) {
                lis = l.getListener();
                break;
            }
        }
        return lis;
    }
}
