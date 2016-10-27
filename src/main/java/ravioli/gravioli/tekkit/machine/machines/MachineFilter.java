package ravioli.gravioli.tekkit.machine.machines;

import java.util.ArrayList;
import java.util.HashMap;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Dispenser;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import ravioli.gravioli.tekkit.Tekkit;
import ravioli.gravioli.tekkit.machine.MachineBase;
import ravioli.gravioli.tekkit.machine.MachineWithInventory;
import ravioli.gravioli.tekkit.util.InventoryUtils;

public class MachineFilter extends MachineWithInventory {
    public MachineFilter() {
        super("Filter Items", 9);
    }

    @Override
    public HashMap<Integer, ItemStack> addItem(ItemStack item, BlockFace input) {
        HashMap<Integer, ItemStack> leftover = new HashMap<Integer, ItemStack>();
        if (this.canTransport(item)) {
            this.routeItem(input.getOppositeFace(), item);
        } else {
            leftover.put(0, item);
        }
        return leftover;
    }

    @Override
    public void run() {
        org.bukkit.material.Dispenser block = (org.bukkit.material.Dispenser) this.getBlock().getState().getData();
        Block input = this.getBlock().getRelative(block.getFacing().getOppositeFace());

        if (input.getState() instanceof InventoryHolder) {
            MachineBase machineCheck = Tekkit.getMachineManager().getMachineByLocation(input.getLocation());
            if (machineCheck == null) {
                InventoryHolder inventoryHolder = (InventoryHolder) input.getState();
                Inventory inventory = inventoryHolder.getInventory();
                if (!InventoryUtils.isInventoryEmpty(inventory)) {
                    ItemStack item = null;
                    for (ItemStack itemStack: inventory.getContents()) {
                        if (itemStack != null) {
                            if (this.canTransport(itemStack)) {
                                item = itemStack;
                                break;
                            }
                        }
                    }
                    if (item != null) {
                        this.routeItem(block.getFacing(), item);
                        inventory.removeItem(item);
                    }
                }
            }
        }
    }

    @Override
    public void onCreate() {
        Dispenser block = (Dispenser) getLocation().getBlock().getState();
        block.getInventory().setItem(4, new ItemStack(Material.PAPER));

        ItemStack check = new ItemStack(Material.STAINED_GLASS_PANE, 1);
        ItemMeta itemMeta = check.getItemMeta();
        itemMeta.setDisplayName(ChatColor.AQUA + "Toggle Filtration Type");
        check.setItemMeta(itemMeta);

        this.getInventory().setItem(8, check);
    }

    @Override
    public void onDestroy() {}

    @Override
    public void onEnable() {
        org.bukkit.material.Dispenser dispenser = (org.bukkit.material.Dispenser) this.getBlock().getState().getData();
        this.acceptableInputs[dispenser.getFacing().getOppositeFace().ordinal()] = true;
        this.acceptableOutputs[dispenser.getFacing().ordinal()] = true;

        if (this.getBlock().getType() != Material.DISPENSER) {
            this.destroy(false);
        }
    }

    @Override
    public ArrayList<ItemStack> getDrops() {
        ArrayList<ItemStack> drops = new ArrayList();
        for (int i = 0; i < this.getInventory().getSize(); i++) {
            if (i != 8) {
                ItemStack item = getInventory().getItem(i);
                if (item != null) {
                    drops.add(item);
                }
            }
        }
        return drops;
    }

    @Override
    public Recipe getRecipe() {
        ItemStack item = new ItemStack(Material.DISPENSER);
        ItemMeta itemMeta = item.getItemMeta();
        itemMeta.setDisplayName(ChatColor.YELLOW + "Filter");
        item.setItemMeta(itemMeta);

        ShapedRecipe recipe = new ShapedRecipe(item);
        recipe.shape("CCC", "GPG", "CTC");
        recipe.setIngredient('C', Material.COBBLESTONE);
        recipe.setIngredient('G', Material.GOLD_INGOT);
        recipe.setIngredient('P', Material.PISTON_BASE);
        recipe.setIngredient('T', Material.ENDER_PEARL);

        return recipe;
    }

    @Override
    public String getName() {
        return "Filter";
    }

    @Override
    public boolean doDrop() {
        return true;
    }

    public boolean canTransport(ItemStack item) {
        ItemStack check = this.getInventory().getItem(8);
        if (check.getDurability() == 0) {
            return this.isWhitelisted(item);
        }
        return !this.isWhitelisted(item);
    }

    public boolean isWhitelisted(ItemStack item) {
        for (int i = 0; i < this.getInventory().getSize(); i++) {
            if (i != 8) {
                ItemStack itemStack = this.getInventory().getItem(i);
                if (itemStack != null && item.isSimilar(itemStack)) {
                    return true;
                }
            }
        }
        return false;
    }

    @EventHandler
    public void onDispenserActivate(BlockDispenseEvent event) {
        Block block = event.getBlock();
        if (block.getLocation().equals(this.getLocation())) {
            event.setCancelled(true);
            this.run();
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getInventory().equals(this.getInventory()) && event.getSlot() == event.getRawSlot() && event.getSlot() == 8) {
            ItemStack check = this.getInventory().getItem(8);
            if (check.getDurability() == 0) {
                check.setDurability((short) 15);
            } else {
                check.setDurability((short) 0);
            }
            event.setCancelled(true);
        }
    }
}