package com.rogermiranda1000.mineit.mineable_gems;

import com.rogermiranda1000.helper.CustomCommand;
import com.rogermiranda1000.helper.RogerPlugin;
import com.rogermiranda1000.helper.reflection.OnServerEvent;
import com.rogermiranda1000.helper.reflection.SpigotEventOverrider;
import com.rogermiranda1000.mineit.MineItApi;
import com.rogermiranda1000.mineit.mineable_gems.events.BreakEventListener;
import com.rogermiranda1000.mineit.mineable_gems.recompiler.*;
import com.rogermiranda1000.mineit.mineable_gems.recompiler.Error;
import com.rogermiranda1000.versioncontroller.VersionController;
import me.Mohamad82.MineableGems.Core.DropReader;
import me.Mohamad82.MineableGems.Events.BreakEvent;
import me.Mohamad82.MineableGems.Main;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
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
    public String getPluginID() { return null; }

    @SuppressWarnings("ConstantConditions") // ignore NPE
    @Override
    public void onEnable() {
        super.onEnable();

        /* Recompile MineableGems */
        String className = DropReader.class.getName();
        try {
            String jarPath = DropReader.class.
                    getProtectionDomain()
                    .getCodeSource()
                    .getLocation()
                    .toURI()
                    .getPath();

            String[] compileClasspaths = new String[]{VersionController.runningJarPath, jarPath};

            getLogger().info("Recompiling " + className + "...");
            new JavaRecompiler(new JDCodeDecompiler(),
                        new CodeReplacer[]{
                                new RegexCodeReplacer(
                                        "public\\s+CustomDrop\\s+readCustomDrop\\s*\\(ConfigurationSection ([^,)]+)[^)]*\\)\\s*\\{\\s*CustomDrop customDrop",
                                        (groups) -> " = null; plugin.getLogger().info(\"HEYYY!!!\")",
                                        true,
                                        true
                                )
                        },
                        new RuntimeCompiler(),
                        (code, errors) -> {
                            // we're assuming it's cast error, and needs to be casted into List<String>
                            for (Error e : errors) {
                                int index = MinableGems.ordinalIndexOf(code, "\n", e.getLine() - 1);
                                if (index == -1) throw new CompileException("Line " + e.getLine() + " not found.", e);
                                //this.getLogger().info("Fixing line " + e.getLine() + "...");

                                Matcher m = e.getPattern().matcher(code.substring(index));
                                if (!m.find()) throw new CompileException("Can't find error position.", e);
                                int insertFixIndex = code.substring(0, index + m.end()).lastIndexOf('(') + 1;
                                code = code.substring(0, insertFixIndex) + "(List<String>)" + code.substring(insertFixIndex);
                            }
                            return code;
                        }, true)
                    .recompile(jarPath, className, compileClasspaths, JavaRecompiler.JAVA_8);

            getLogger().info(className + " compiled, the server must be restarted.");
            getServer().reload();
        } catch (AlreadyRecompiledException ex) {
            getLogger().info(className + " already recompiled.");
        } catch (Exception ex) {
            this.printConsoleErrorMessage("Error while recompiling MineableGems");
            ex.printStackTrace();
        }

        // load after MineableGems
        Bukkit.getScheduler().scheduleSyncDelayedTask(this, ()->{
            PluginManager pm = getServer().getPluginManager();
            Plugin mineableGems = pm.getPlugin("MineableGems");
            Main mineableGemsObject = Main.getInstance();
            MineItApi mineItObject = MineItApi.getInstance();

            // override BlockBreakEvent
            final OnServerEvent<BlockBreakEvent> onBlockBreak = SpigotEventOverrider.overrideListener(mineableGems, BreakEvent.class, BlockBreakEvent.class);
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
