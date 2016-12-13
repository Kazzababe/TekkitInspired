package ravioli.gravioli.tekkit.machines;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import ravioli.gravioli.tekkit.Tekkit;
import ravioli.gravioli.tekkit.database.utils.DatabaseObject;
import ravioli.gravioli.tekkit.utils.InventoryUtils;

import java.util.List;

public abstract class MachineWithInventory extends Machine {
    @DatabaseObject
    private Inventory inventory;

    public MachineWithInventory(Tekkit plugin, String name, int size) {
        super(plugin);
        inventory = Bukkit.createInventory(null, size, name);
    }

    /**
     * Gets a list of all non null items in the machines inventory
     *
     * @return the machines drops
     */
    @Override
    public List<ItemStack> getDrops() {
        return InventoryUtils.getNonNullInventoryContents(inventory);
    }

    /**
     * Gets the machines inventory
     *
     * @return the machines inventory
     */
    public Inventory getInventory() {
        return inventory;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            ItemStack item = event.getItem();
            Block block = event.getClickedBlock();
            if (event.getClickedBlock() != null && event.getClickedBlock().getLocation().equals(this.getLocation()) && (item == null || item != null && !item.getType().isBlock() && item.getType() != Material.REDSTONE || !event.getPlayer().isSneaking())) {
                event.setCancelled(true);
                event.getPlayer().openInventory(inventory);
            }
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getInventory().equals(inventory)) {
            event.setCancelled(true);
        }
    }
}
