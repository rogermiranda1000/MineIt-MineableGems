package com.rogermiranda1000.mineit.mineable_gems.events;

import com.rogermiranda1000.mineit.Mine;
import com.rogermiranda1000.mineit.MineItApi;
import com.rogermiranda1000.mineit.mineable_gems.CustomMineDrop;
import me.Mohamad82.MineableGems.Core.CustomDrop;
import me.Mohamad82.MineableGems.Main;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

import java.util.List;
import java.util.stream.Stream;

public class BreakEventListener implements Listener {
    private final MineItApi mineItApiObject;
    private final Main mineableGemsObject;
    private final BreakEvent mineableGemsBreakEvent;

    public BreakEventListener(BreakEvent mineableGemsBreakEvent, MineItApi mineItApiObject, Main mineableGemsObject) {
        this.mineableGemsBreakEvent = mineableGemsBreakEvent;
        this.mineItApiObject = mineItApiObject;
        this.mineableGemsObject = mineableGemsObject;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent e) {
        // code extracted from me.Mohamad82.MineableGems.Events.BreakEvent
        Block block = e.getBlock();
        final Mine mine = this.mineItApiObject.getMine(block);
        String mined = block.getType().toString();
        List<CustomDrop> dropCandidates = this.mineableGemsObject.gems.get(mined);
        Stream<CustomDrop> insideMineIter = dropCandidates.stream().filter((d) -> d instanceof CustomMineDrop).filter((md) -> ((CustomMineDrop)md).getMine().getName().equals(mine == null ? "" : mine.getName())),
                noMineIter = dropCandidates.stream().filter((d) -> !(d instanceof CustomMineDrop));

        synchronized (BreakEventListener.class) {
            // TODO change Main.getInstance().gems
            insideMineIter.forEach((d) -> System.out.println(d.getDrop().getType().name()));
            noMineIter.forEach((d) -> System.out.println(d.getDrop().getType().name()));

            this.mineableGemsBreakEvent.onBlockBreak(e);
        }
    }
}
