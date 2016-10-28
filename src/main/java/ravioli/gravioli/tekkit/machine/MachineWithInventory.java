package ravioli.gravioli.tekkit.machine;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import ravioli.gravioli.tekkit.machine.utilities.Persistent;

public abstract class MachineWithInventory extends MachineBase {
    @Persistent
    private Inventory inventory;

    public MachineWithInventory(String name, int size) {
        this.inventory = Bukkit.createInventory(null, size, name);
    }

    @Override
    public ArrayList<ItemStack> getDrops() {
        ArrayList<ItemStack> drops = new ArrayList();
        ArrayList<ItemStack> inventoryDrops = new ArrayList(Arrays.asList(this.inventory.getContents()));
        inventoryDrops.removeAll(Arrays.asList("", null));
        drops.addAll(inventoryDrops);
        return drops;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            ItemStack item = event.getItem();
            Block block = event.getClickedBlock();
            if (event.getClickedBlock() != null && event.getClickedBlock().getLocation().equals(this.getLocation()) && (item == null || item != null && !item.getType().isBlock() && item.getType() != Material.REDSTONE || !event.getPlayer().isSneaking())) {
                event.setCancelled(true);
                event.getPlayer().openInventory(this.inventory);
            }
        }
    }


    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if ((event.getInventory().equals(this.inventory)) && (event.getSlot() == event.getRawSlot())) {
            this.saveAsync();
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getInventory().equals(this.inventory)) {
            event.setCancelled(true);
        }
    }

    public Inventory getInventory() {
        return this.inventory;
    }

    public abstract HashMap<Integer,ItemStack> addItem(ItemStack item, BlockFace input);
}