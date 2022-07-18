package com.rogermiranda1000.mineit.mineable_gems.events;

import com.rogermiranda1000.mineit.Mine;
import com.rogermiranda1000.mineit.MineItApi;
import com.rogermiranda1000.mineit.mineable_gems.CustomMineDrop;
import com.sun.istack.internal.NotNull;
import me.Mohamad82.MineableGems.Core.CustomDrop;
import me.Mohamad82.MineableGems.Main;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class BreakEventListener implements Listener {
    private final MineItApi mineItApiObject;
    private final Main mineableGemsObject;
    private final BreakEvent mineableGemsBreakEvent;

    public BreakEventListener(BreakEvent mineableGemsBreakEvent, @NotNull MineItApi mineItApiObject, @NotNull Main mineableGemsObject) {
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
        List<CustomDrop> dropCandidates;
        synchronized (BreakEventListener.class) {
            dropCandidates = this.mineableGemsObject.gems.get(mined);
        }
        if (dropCandidates == null) return; // any block of that type
        Stream<CustomDrop> insideMineIter = dropCandidates.stream().filter((d) -> d instanceof CustomMineDrop)
                        .filter((md) -> mine != null && mine.equals(((CustomMineDrop) md).getMine())),
                noMineIter = dropCandidates.stream().filter((d) -> !(d instanceof CustomMineDrop));

        // drops to list (https://stackoverflow.com/a/22755993/9178470)
        final List<CustomDrop> drops = new ArrayList<>();
        insideMineIter.sequential().collect(Collectors.toCollection(() -> drops));
        noMineIter.sequential().collect(Collectors.toCollection(() -> drops));

        synchronized (BreakEventListener.class) {
            this.mineableGemsObject.gems.put(mined, drops); // override the drops

            this.mineableGemsBreakEvent.onBlockBreak(e);

            this.mineableGemsObject.gems.put(mined, dropCandidates); // restore original drops
        }
    }
}
