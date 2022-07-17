package com.rogermiranda1000.mineit.mineable_gems;

import com.rogermiranda1000.mineit.Mine;
import com.rogermiranda1000.mineit.blocks.Mines;
import com.sun.istack.internal.NotNull;
import me.Mohamad82.MineableGems.Core.CustomDrop;
import org.bukkit.inventory.ItemStack;

public class CustomMineDrop extends CustomDrop {
    @NotNull private final Mine mine;

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
    }

    @NotNull
    public Mine getMine() {
        return this.mine;
    }
}