package ravioli.gravioli.tekkit.machines.transport.utils;

import org.bukkit.block.*;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import ravioli.gravioli.tekkit.utils.InventoryUtils;

public class TransportUtils {
    public static boolean canInventoryBlockReceive(InventoryHolder block, ItemStack item, BlockFace input) {
        if (block instanceof Hopper || block instanceof BrewingStand || block instanceof Beacon) {
            return false;
        }
        if (block instanceof Furnace) {
            Furnace furnace = (Furnace) block;
            if (input == BlockFace.UP) {
                ItemStack smelting = furnace.getInventory().getSmelting();
                if (smelting == null) {
                    return true;
                }
                if (smelting.isSimilar(item)) {
                    return smelting.getAmount() + item.getAmount() <= item.getMaxStackSize();
                }
            } else if (input == BlockFace.DOWN) {
                ItemStack fuel = furnace.getInventory().getFuel();
                if (fuel == null) {
                    return true;
                }
                if (fuel.isSimilar(item)) {
                    return fuel.getAmount() + item.getAmount() <= item.getMaxStackSize();
                }
            }
        } else {
            return InventoryUtils.canFitIntoInventory(block.getInventory(), item);
        }

        return false;
    }
}
