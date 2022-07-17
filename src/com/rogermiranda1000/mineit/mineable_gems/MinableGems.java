package com.rogermiranda1000.mineit.mineable_gems;

import com.rogermiranda1000.mineit.ListenerNotFoundException;
import com.rogermiranda1000.mineit.MineIt;
import com.rogermiranda1000.mineit.MineItApi;
import com.rogermiranda1000.mineit.mineable_gems.events.BreakEventListener;
import com.rogermiranda1000.mineit.mineable_gems.recompiler.*;
import com.rogermiranda1000.mineit.mineable_gems.recompiler.Error;
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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.regex.Matcher;

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
        String jarPath = "plugins/MineableGems-1.11.3.jar"; // TODO get name
        String className = DropReader.class.getName();
        String[] compileClasspaths = new String[]{"spigot-1.8.jar", "plugins/MineableGems-1.11.3.jar"};
        try {
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
                                //System.out.println(source.substring(index, index+100));

                                Matcher m = e.getPattern().matcher(code.substring(index));
                                if (!m.find()) throw new CompileException("Can't find error position.", e);
                                int insertFixIndex = code.substring(0, index + m.end()).lastIndexOf('(') + 1;
                                code = code.substring(0, insertFixIndex) + "(List<String>)" + code.substring(insertFixIndex);
                            }
                            return code;
                        }
                    ).recompile(jarPath, className, compileClasspaths, JavaRecompiler.JAVA_8);

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
