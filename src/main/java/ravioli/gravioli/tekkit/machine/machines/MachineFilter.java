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
import ravioli.gravioli.tekkit.machine.transport.MovingItem;
import ravioli.gravioli.tekkit.machine.transport.PipeReceiver;
import ravioli.gravioli.tekkit.manager.MachineManager;
import ravioli.gravioli.tekkit.util.InventoryUtils;

public class MachineFilter extends MachineWithInventory implements PipeReceiver {
    public MachineFilter() {
        super("Filter Items", 9);
    }

    @Override
    public boolean canReceiveItem(MovingItem item, BlockFace input) {
        return this.acceptableInput(input) && this.canTransport(item.getItemStack());
    }

    @Override
    public void addItem(MovingItem item, BlockFace input) {
        if (this.canTransport(item.getItemStack())) {
            this.routeItem(input.getOppositeFace(), item.getItemStack());
        }
    }

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
        org.bukkit.material.Dispenser dispenser = (org.bukkit.material.Dispenser) this.getBlock().getState().getData();

        Block input = this.getBlock().getRelative(dispenser.getFacing().getOppositeFace());
        Block output = this.getBlock().getRelative(dispenser.getFacing());

        MachineBase machine = MachineManager.getMachineByLocation(input.getLocation());

        Inventory outputInventory = null;
        if (output.getState() instanceof InventoryHolder) {
            InventoryHolder inventoryHolder = (InventoryHolder) output.getState();
            outputInventory = inventoryHolder.getInventory();

            MachineBase outputMachine = MachineManager.getMachineByLocation(output.getLocation());
            if (outputMachine != null && outputMachine instanceof MachineWithInventory && outputMachine.acceptableInput(dispenser.getFacing())) {
                outputInventory = ((MachineWithInventory) outputMachine).getInventory();
            }
        }

        Inventory inputInventory = null;
        if (machine != null) {
            if (machine.acceptableOutput(dispenser.getFacing())) {
                if (machine instanceof MachineWithInventory) {
                    MachineWithInventory inventoryMachine = (MachineWithInventory) machine;
                    inputInventory = inventoryMachine.getInventory();
                }
            }
        } else {
            if (input.getState() instanceof InventoryHolder) {
                InventoryHolder inventoryHolder = (InventoryHolder) input.getState();
                inputInventory = inventoryHolder.getInventory();
            }
        }

        if (inputInventory != null) {
            if (!InventoryUtils.isInventoryEmpty(inputInventory)) {
                ItemStack itemStack = null;
                for (ItemStack item : inputInventory.getContents()) {
                    if (item != null) {
                        if (this.canTransport(item)) {
                            if (outputInventory != null && !InventoryUtils.canFitIntoInventory(outputInventory, item)) {
                                continue;
                            }
                            itemStack = item;
                            break;
                        }
                    }
                }
                if (itemStack != null) {
                    this.routeItem(dispenser.getFacing(), itemStack);
                    inputInventory.removeItem(itemStack);
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
    public void onEnable() {
        if (this.getBlock().getType() != Material.DISPENSER) {
            this.destroy(false);
            return;
        }

        org.bukkit.material.Dispenser dispenser = (org.bukkit.material.Dispenser) this.getBlock().getState().getData();
        this.acceptableInputs[dispenser.getFacing().getOppositeFace().ordinal()] = true;
        this.acceptableOutputs[dispenser.getFacing().ordinal()] = true;
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