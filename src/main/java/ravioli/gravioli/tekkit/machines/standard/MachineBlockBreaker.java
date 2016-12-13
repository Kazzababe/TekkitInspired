package ravioli.gravioli.tekkit.machines.standard;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.material.Dispenser;
import ravioli.gravioli.tekkit.Tekkit;
import ravioli.gravioli.tekkit.machines.Machine;
import ravioli.gravioli.tekkit.machines.MachinesManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

public class MachineBlockBreaker extends Machine {
    public MachineBlockBreaker(Tekkit plugin) {
        super(plugin);
    }

    @Override
    public void onCreate() {
        org.bukkit.block.Dispenser block = (org.bukkit.block.Dispenser) this.getLocation().getBlock().getState();
        block.getInventory().setItem(4, new ItemStack(Material.PAPER));
    }

    @Override
    public void runMachine() {
        Dispenser block = (Dispenser) getBlock().getState().getData();
        Block facing = getBlock().getRelative(block.getFacing());

        if (facing.getType() != Material.AIR && facing.getType() != Material.BEDROCK && facing.getType() != Material.OBSIDIAN) {
            HashSet<ItemStack> drops = new HashSet();
            Machine machineCheck = MachinesManager.getMachineByLocation(facing.getLocation());
            if (machineCheck != null) {
                drops.addAll(machineCheck.getDrops());
                drops.add(machineCheck.getRecipe().getResult());
                machineCheck.destroy(false);
            } else if (!facing.hasMetadata("machine")) {
                drops.addAll(facing.getDrops(new ItemStack(Material.IRON_PICKAXE)));
                if (facing.getState() instanceof InventoryHolder) {
                    InventoryHolder holder = (InventoryHolder) facing.getState();
                    ArrayList<ItemStack> items = new ArrayList<ItemStack>(Arrays.asList(holder.getInventory().getContents()));
                    items.removeAll(Arrays.asList("", null));
                    drops.addAll(items);
                }
                facing.setTypeIdAndData(0, (byte) 0, true);
            } else {
                Machine check = checkBlockMachinePiece(facing);
                if (check != null) {
                    check.onMachinePieceBreak(facing);
                }
                facing.setTypeIdAndData(0, (byte) 0, true);
            }
            drops.forEach(drop -> routeItem(block.getFacing().getOppositeFace(), drop));
        }
    }

    @Override
    public List<ItemStack> getDrops() {
        return new ArrayList();
    }

    @Override
    public Recipe getRecipe() {
        ItemStack item = new ItemStack(Material.DISPENSER);
        ItemMeta itemMeta = item.getItemMeta();
        itemMeta.setDisplayName(ChatColor.YELLOW + "Block Breaker");
        item.setItemMeta(itemMeta);

        ShapedRecipe recipe = new ShapedRecipe(item);
        recipe.shape("CIC", "CPC", "CRC");
        recipe.setIngredient('C', Material.COBBLESTONE);
        recipe.setIngredient('I', Material.IRON_PICKAXE);
        recipe.setIngredient('P', Material.PISTON_BASE);
        recipe.setIngredient('R', Material.REDSTONE);

        return recipe;
    }

    @Override
    public String getTableName() {
        return "BlockBreaker";
    }

    @Override
    public String getName() {
        return "blockbreaker";
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
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            ItemStack item = event.getItem();
            if (event.getClickedBlock() != null && event.getClickedBlock().getLocation().equals(getLocation()) && (item == null || item != null && !item.getType().isBlock() && item.getType() != Material.REDSTONE || !event.getPlayer().isSneaking())) {
                event.setCancelled(true);
            }
        }
    }
}
