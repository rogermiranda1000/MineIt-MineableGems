package com.rogermiranda1000.mineit.mineable_gems;

import com.rogermiranda1000.mineit.ListenerNotFoundException;
import com.rogermiranda1000.mineit.MineIt;
import me.Mohamad82.MineableGems.Core.DropReader;
import me.Mohamad82.MineableGems.Main;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredListener;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class MinableGems extends JavaPlugin {
    @Override
    public void onEnable() {
        // TODO load after
        /*PluginManager pm = getServer().getPluginManager();
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
        }*/
    }

    private void reloadMGConfig() {
        // code from Commands > onCommand > reload
        Main.getInstance().configuration.reloadConfig();
        new DropReader().reload();
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
