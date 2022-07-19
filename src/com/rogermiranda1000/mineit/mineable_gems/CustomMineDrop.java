package com.rogermiranda1000.mineit.mineable_gems;

import com.rogermiranda1000.mineit.Mine;
import com.rogermiranda1000.mineit.MineChangedEvent;
import com.rogermiranda1000.mineit.blocks.Mines;
import com.sun.istack.internal.NotNull;
import me.Mohamad82.MineableGems.Core.CustomDrop;
import org.bukkit.craftbukkit.libs.jline.internal.Nullable;
import org.bukkit.inventory.ItemStack;

public class CustomMineDrop extends CustomDrop implements MineChangedEvent {
    @Nullable
    private Mine mine;

    /**
     * @pre Load MineIt before MineableGems
     *      (MineIt-MineableGems loads after MineIt, and MineableGems after MineIt-MineableGems; it should be OK)
     */
    public CustomMineDrop(String mine, ItemStack drop) {
        this(Mines.getInstance().getMine(mine), drop);
    }

    public CustomMineDrop(@NotNull Mine mine, ItemStack drop) {
        super(drop);
        this.mine = mine;
        this.mine.addMineListener(this);
    }

    @Nullable
    public Mine getMine() {
        return this.mine;
    }

    @Override
    public void onMineChanged() { }

    @Override
    public void onMineRemoved() {
        this.mine = null;
    }
}