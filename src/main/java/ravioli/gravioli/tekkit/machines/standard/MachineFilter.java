package ravioli.gravioli.tekkit.machines.standard;

import org.bukkit.ChatColor;
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
import ravioli.gravioli.tekkit.machines.Machine;
import ravioli.gravioli.tekkit.machines.MachineWithInventory;
import ravioli.gravioli.tekkit.machines.MachinesManager;
import ravioli.gravioli.tekkit.machines.transport.MovingItem;
import ravioli.gravioli.tekkit.machines.transport.TransportReceiver;
import ravioli.gravioli.tekkit.utils.InventoryUtils;

import java.util.ArrayList;
import java.util.List;

public class MachineFilter extends MachineWithInventory implements TransportReceiver {
    public MachineFilter(Tekkit plugin) {
        super(plugin, "Filter Items", 9);
    }

    @Override
    public boolean canReceiveItem(MovingItem item, BlockFace input) {
        return acceptableInput(input) && canTransport(item.getItemStack());
    }

    @Override
    public void addMovingItem(MovingItem item, BlockFace input) {
        addItem(item.getItemStack(), input);
    }

    public void addItem(ItemStack item, BlockFace input) {
        if (canTransport(item)) {
            routeItem(input.getOppositeFace(), item);
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

        getInventory().setItem(8, check);
    }

    @Override
    public List<ItemStack> getDrops() {
        List<ItemStack> drops = new ArrayList();
        for (int i = 0; i < getInventory().getSize(); i++) {
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
    public void onEnable() {
        org.bukkit.material.Dispenser dispenser = (org.bukkit.material.Dispenser) getBlock().getState().getData();
        acceptableInputs[dispenser.getFacing().getOppositeFace().ordinal()] = true;
        acceptableOutputs[dispenser.getFacing().ordinal()] = true;
    }

    @Override
    public void runMachine() {
        org.bukkit.material.Dispenser dispenser = (org.bukkit.material.Dispenser) getBlock().getState().getData();

        Block input = getBlock().getRelative(dispenser.getFacing().getOppositeFace());
        Block output = getBlock().getRelative(dispenser.getFacing());

        Machine machine = MachinesManager.getMachineByLocation(input.getLocation());

        Inventory outputInventory = null;
        if (output.getState() instanceof InventoryHolder) {
            InventoryHolder inventoryHolder = (InventoryHolder) output.getState();
            outputInventory = inventoryHolder.getInventory();

            Machine outputMachine = MachinesManager.getMachineByLocation(output.getLocation());
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
                        if (canTransport(item)) {
                            if (outputInventory != null && !InventoryUtils.canFitIntoInventory(outputInventory, item)) {
                                continue;
                            }
                            itemStack = item;
                            break;
                        }
                    }
                }
                if (itemStack != null) {
                    routeItem(dispenser.getFacing(), itemStack);
                    inputInventory.removeItem(itemStack);
                }
            }
        }
    }

    public boolean canTransport(ItemStack item) {
        ItemStack check = getInventory().getItem(8);
        if (check.getDurability() == 0) {
            return isWhitelisted(item);
        }
        return !isWhitelisted(item);
    }

    public boolean isWhitelisted(ItemStack item) {
        for (int i = 0; i < getInventory().getSize(); i++) {
            if (i != 8) {
                ItemStack itemStack = getInventory().getItem(i);
                if (itemStack != null && item.isSimilar(itemStack)) {
                    return true;
                }
            }
        }
        return false;
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
    public String getTableName() {
        return "Filter";
    }

    @Override
    public String getName() {
        return "filter";
    }

    @EventHandler
    public void onDispenserActivate(BlockDispenseEvent event) {
        Block block = event.getBlock();
        if (block.getLocation().equals(getLocation())) {
            event.setCancelled(true);
            run();
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getInventory().equals(getInventory()) && event.getSlot() == event.getRawSlot() && event.getSlot() == 8) {
            ItemStack check = getInventory().getItem(8);
            if (check.getDurability() == 0) {
                check.setDurability((short) 15);
            } else {
                check.setDurability((short) 0);
            }
            event.setCancelled(true);
        }
    }
}
