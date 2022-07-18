package com.rogermiranda1000.mineit.mineable_gems;

import com.rogermiranda1000.mineit.Mine;
import com.rogermiranda1000.mineit.blocks.Mines;
import me.Mohamad82.MineableGems.Core.CustomDrop;
import org.bukkit.craftbukkit.libs.jline.internal.Nullable;
import org.bukkit.inventory.ItemStack;

public class CustomMineDrop extends CustomDrop {
    private final Mine mine;

    /**
     * @pre Load MineIt before MineableGems
     *      (MineIt-MineableGems loads after MineIt, and MineableGems after MineIt-MineableGems; it should be OK)
     */
    public CustomMineDrop(String mine, ItemStack drop) {
        this(Mines.getInstance().getMine(mine), drop);
    }

    public CustomMineDrop(Mine mine, ItemStack drop) {
        super(drop);
        this.mine = mine;
    }

    @Nullable
    public Mine getMine() {
        if (this.mine == null) return null;
        return Mines.getInstance().getMine(this.mine.getName()); // maybe the mine was deleted
    }
}