package com.rogermiranda1000.mineit;

import com.sun.istack.internal.NotNull;
import me.Mohamad82.MineableGems.Core.CustomDrop;
import org.bukkit.inventory.ItemStack;

public class CustomMineDrop extends CustomDrop {
    @NotNull private final Mine mine;

    public CustomMineDrop(@NotNull Mine mine, ItemStack drop) {
        super(drop);
        this.mine = mine;
    }

    @NotNull
    public Mine getMine() {
        return this.mine;
    }
}
