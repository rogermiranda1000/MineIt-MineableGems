package com.rogermiranda1000.mineit.mineable_gems;

import com.rogermiranda1000.helper.CustomCommand;
import com.rogermiranda1000.helper.RogerPlugin;
import com.rogermiranda1000.helper.reflection.OnServerEvent;
import com.rogermiranda1000.helper.reflection.SpigotEventOverrider;
import com.rogermiranda1000.mineit.Mine;
import com.rogermiranda1000.mineit.MineItApi;
import com.rogermiranda1000.mineit.mineable_gems.events.BreakEventListener;
import com.rogermiranda1000.mineit.mineable_gems.recompiler.*;
import com.rogermiranda1000.mineit.mineable_gems.recompiler.Error;
import com.rogermiranda1000.mineit.mineable_gems.recompiler.replacers.CodeReplacer;
import com.rogermiranda1000.mineit.mineable_gems.recompiler.replacers.DependenciesImporter;
import com.rogermiranda1000.mineit.mineable_gems.recompiler.replacers.RegexCodeReplacer;
import com.rogermiranda1000.versioncontroller.Version;
import com.rogermiranda1000.versioncontroller.VersionController;
import me.Mohamad82.MineableGems.Core.DropReader;
import me.Mohamad82.MineableGems.Events.BreakEvent;
import me.Mohamad82.MineableGems.Events.BreakEvent_Legacy;
import me.Mohamad82.MineableGems.Main;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;

import javax.annotation.Nullable;
import java.io.*;
import java.net.URISyntaxException;
import java.util.List;
import java.util.function.Function;
import java.util.regex.Matcher;

public class MinableGems extends RogerPlugin {
    public MinableGems() {
        super(new CustomCommand[]{});
    }

    public void printConsoleErrorMessage(String msg) {
        Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "[" + this.getName() + "] " + msg);
    }

    public void printConsoleWarningMessage(String msg) {
        Bukkit.getConsoleSender().sendMessage(ChatColor.YELLOW + "[" + this.getName() + "] " + msg);
    }

    @Override
    public String getPluginID() { return "103464"; }

    @Nullable
    private static String getJarPath(Class<?> c) {
        try {
            return c.getProtectionDomain()
                    .getCodeSource()
                    .getLocation()
                    .toURI()
                    .getPath();
        } catch (URISyntaxException e) {
            return null;
        }
    }

    /**
     * @post is is closed
     */
    private static void cloneFile(InputStream is, File out) throws IOException {
        OutputStream os = new FileOutputStream(out);

        byte[] buffer = new byte[8 * 1024];
        int bytesRead;
        while ((bytesRead = is.read(buffer)) != -1) {
            os.write(buffer, 0, bytesRead);
        }
        is.close();
        os.close();
    }

    @SuppressWarnings("ConstantConditions") // ignore NPE
    @Override
    public void onEnable() {
        super.onEnable();

        /* Recompile MineableGems */
        final String className = DropReader.class.getName();
        final String originalJarPath = MinableGems.getJarPath(DropReader.class),
                    thisJarPath = MinableGems.getJarPath(this.getClass()), // this plugin & spigot 1.16
                    mineItJarPath = MinableGems.getJarPath(Mine.class);

        Function<File,Boolean> compile = (spigot)-> {
            try {
                String[] compileClasspaths = new String[]{(spigot != null) ? spigot.getPath() : VersionController.runningJarPath, originalJarPath, thisJarPath, mineItJarPath};

                getLogger().info("Recompiling " + className + "...");
                new JavaRecompiler(new JDCodeDecompiler(),
                        new CodeReplacer[]{
                                new DependenciesImporter(CustomMineDrop.class),
                                new DependenciesImporter(List.class), // for some reason, in >1.13 it doesn't exist
                                new RegexCodeReplacer(
                                        "public\\s+CustomDrop\\s+readCustomDrop\\s*\\(ConfigurationSection ([^,)]+)[^)]*\\)\\s*\\{[\\s\\S]*=\\s*(?=new CustomDrop\\((.+)\\);)",
                                        (groups) -> groups[0] + ".contains(\"Mine\") ? new CustomMineDrop(" + groups[0] + ".getString(\"Mine\"), " + groups[1] + ") : ", // inserted between '=' and 'new CustomDrop'
                                        true, false
                                )/*,
                                new RegexCodeReplacer("(?=plugin\\.gems\\.put\\(([^,]+),([^)]+))",
                                        (groups) -> "if (plugin.gems.get(" + groups[0] + ") != null) " + groups[1] + ".addAll(DropReader.plugin.gems.get(" + groups[0] + "));",
                                        true, true)*/
                        },
                        new RuntimeCompiler(),
                        (code, errors) -> {
                            for (Error e : errors) {
                                //this.getLogger().info("Fixing line " + e.getLine() + "(" + e.getError() + ")...");
                                if (!e.getError().startsWith("incompatible types: ") || !e.getError().endsWith(" cannot be converted to List<String>"))
                                    continue; // we don't know how to solve it

                                int index = MinableGems.ordinalIndexOf(code, "\n", e.getLine() - 1);
                                if (index == -1) throw new CompileException("Line " + e.getLine() + " not found.", e);

                                Matcher m = e.getPattern().matcher(code.substring(index));
                                if (!m.find()) throw new CompileException("Can't find error position.", e);
                                int insertFixIndex = code.substring(0, index + m.end()).lastIndexOf('(') + 1;
                                code = code.substring(0, insertFixIndex) + "(List<String>)" + code.substring(insertFixIndex);
                            }
                            return code;
                        }, true)
                        .recompile(originalJarPath, className, compileClasspaths, RuntimeCompiler.JAVA_8);

                getLogger().info(className + " compiled, the server must be restarted.");
                Bukkit.getScheduler().scheduleSyncDelayedTask(this, () -> getServer().reload(), 1L); // reload once all is loaded
                return false; // don't launch the listener overrider; we need to wait for the reload
            } catch (AlreadyRecompiledException ex) {
                getLogger().info(className + " already recompiled.");
                return true;
            } catch (Exception ex) {
                this.printConsoleErrorMessage("Error while recompiling MineableGems");
                ex.printStackTrace();
                return false; // error
            }
        };

        File tmpSpigot = null;
        if (VersionController.version.compareTo(Version.MC_1_17) > 0) {
            // it won't work on spigot 1.18 and 1.19
            File pluginFolder = this.getDataFolder();
            if (!pluginFolder.exists()) pluginFolder.mkdir();
            tmpSpigot = new File(pluginFolder.getPath() + File.separatorChar + "spigot.jar");
            if (!tmpSpigot.exists() && !JavaRecompiler.alreadyCompiled(originalJarPath, className)) {
                this.getLogger().info("Server >1.17; generating required classes...");
                this.printConsoleWarningMessage("This will take some time. MineableGems won't work until the next reload.");
                this.printConsoleWarningMessage("The server will AUTO RELOAD after the compilation ends.");

                final File spigot = tmpSpigot;
                SpigotBuilder.build("1.17.1", tmpSpigot, ()->compile.apply(spigot), this);
                return; // the runnable will do the work
            }
        }

        if (!compile.apply(tmpSpigot)) return;

        // load after MineableGems
        Bukkit.getScheduler().scheduleSyncDelayedTask(this, ()->{
            PluginManager pm = getServer().getPluginManager();
            Plugin mineableGems = pm.getPlugin("MineableGems");
            Main mineableGemsObject = Main.getInstance();
            MineItApi mineItObject = MineItApi.getInstance();

            // override BlockBreakEvent
            final OnServerEvent<BlockBreakEvent> onBlockBreak = (VersionController.version.compareTo(Version.MC_1_13) < 0) ?
                            SpigotEventOverrider.overrideListener(mineableGems, BreakEvent_Legacy.class, BlockBreakEvent.class) :
                            SpigotEventOverrider.overrideListener(mineableGems, BreakEvent.class, BlockBreakEvent.class);
            pm.registerEvents(new BreakEventListener(e -> onBlockBreak.onEvent(e), mineItObject, mineableGemsObject), this);
        }, 1L);
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
}
